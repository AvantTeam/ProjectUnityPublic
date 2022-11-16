package unity.ui;

import arc.*;
import arc.Graphics.Cursor.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.entities.Effect.*;
import mindustry.input.*;
import mindustry.ui.*;
import unity.parts.*;
import unity.parts.ModularConstructBuilder.*;

import static arc.Core.scene;
import static mindustry.Vars.*;
import static unity.graphics.UnityPal.*;

public class PartsEditorElement extends Element{
    //clipping
    private final Rect scissorBounds = new Rect();
    private final Rect widgetAreaBounds = new Rect();
    //history for undos
    Seq<byte[]> prev = new Seq<>();
    int index = -1;

    public ModularConstructBuilder builder;
    public ModularPartType selected = null;
    public ModularPartStatMap statmap = null;
    public boolean erasing = false;
    public boolean mirror = true;
    //selection boxes?

    float panx=0,pany=0;
    float prevpx,prevpy;

    float mousex,mousey;
    float anchorx,anchory;
    public float scl = 1;
    float targetscl = 1;
    int dragTriggered = 0;

    float minimiseScl = 0.5f;


    public PartsEditorElement(ModularConstructBuilder builder){
        this.builder = builder;

        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                dragTriggered = 0;
                anchorx = x;
                anchory = y;
                prevpx = panx;
                prevpy = pany;
                mousex = x;
                mousey = y;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                if(Mathf.dst(anchorx,anchory,x,y)<5){
                    onClicked(event,x,y,pointer,button);
                }
            }


            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                panx = (x-anchorx)+prevpx;
                pany = (y-anchory)+prevpy;
                dragTriggered++;
                mousex = x;
                mousey = y;
                if(mobile){
                    /// set zoom?
                }
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                scene.setScrollFocus(PartsEditorElement.this); ///AAAAAA
                mousex = x;
                mousey = y;
                var g = uiToGrid(x,y);
                if(!tileValid(g.x,g.y)){
                    Core.graphics.cursor(SystemCursor.hand);
                    return true;
                }
                int b = builder.index(g.x,g.y);
                if((selected!=null && builder.parts[b]!=0) ||
                   (selected==null && builder.parts[b]==0) ){
                    Core.graphics.cursor(SystemCursor.hand);
                }else{
                    Core.graphics.cursor(SystemCursor.arrow);
                }
                return true;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                zoom(amountY * targetscl * -0.1f);
                return true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                super.exit(event, x, y, pointer, toActor);
                Core.graphics.cursor(SystemCursor.arrow);
                scene.setScrollFocus(null);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                scene.setScrollFocus(PartsEditorElement.this);
            }

        });
    }

    public static float maxZoom = 1;
    public static float minZoom = 0.2f;

    @Override
    public void act(float delta){
        super.act(delta);
        //targetscl += Core.input.axis(Binding.zoom) / 10f * targetscl;
        //targetscl = Mathf.clamp(targetscl,minZoom,maxZoom);
    }

    public void zoom(float am){
        targetscl += am;
        targetscl = Mathf.clamp(targetscl,minZoom,maxZoom);
    }

    public void onClicked(InputEvent event, float x, float y, int pointer, KeyCode button){
        if(button==KeyCode.mouseRight){
            selected = null;
            return;
        }
        var g = uiToGrid(x,y);
        if(tileValid(g.x,g.y)){
            if(selected!=null){
                boolean b = builder.placePart(selected, g.x, g.y);
                placeEffectAt(g.x, g.y,selected.w,selected.h, Color.white);
                if(b && mirror && mirrorX(g.x)!=g.x){
                    builder.placePart(selected, mirrorX(g.x,selected.w), g.y);
                    placeEffectAt(mirrorX(g.x,selected.w), g.y,selected.w,selected.h, Color.white);
                }
            }else{
                var type = builder.partTypeAt(g.x, g.y);
                if(type!=null){
                    placeEffectAt(g.x, g.y, type.w, type.h, Color.red);
                    builder.deletePartAt(g.x, g.y);

                    if(mirror && mirrorX(g.x) != g.x){
                        type = builder.partTypeAt(mirrorX(g.x), g.y);
                        if(type!=null){
                            placeEffectAt(mirrorX(g.x), g.y, type.w, type.h, Color.red);
                            builder.deletePartAt(mirrorX(g.x), g.y);
                        }
                    }
                }
            }
            onAction();
        }
    }

    public boolean tileValid(int x,int y){
        return (x>=0 && x<builder.w && y>=0 && y< builder.h);
    }

    public void setBuilder(ModularConstructBuilder builder){
        this.builder = builder;
    }

    public void rebuild(){
        //?
    }
    float gx = 0,gy = 0;
    LongSeq partsToDraw = new LongSeq();
    LongSeq[] bucketSort = new LongSeq[5];
    @Override
    public void draw(){
        float cx =   panx + width*0.0f; // cam center relative to gx
        float cy =   pany + height*0.0f; // cam center relative to gy
        float pscl = scl;
        scl += (targetscl-scl)*0.1;

        float scldiff = scl/pscl;




        float dx = cx*(scldiff-1);
        panx+=dx;
        float dy = cy*(scldiff-1);
        pany+=dy;

        gx = gx();
        gy = gy();
        float midx = x + width * 0.5f;
        float midy = y + height * 0.5f;


        Draw.color(bgCol);
        Fill.rect(midx,midy,width,height);

        widgetAreaBounds.set(x,y,width,height);
        scene.calculateScissors(widgetAreaBounds, scissorBounds);
        if(!ScissorStack.push(scissorBounds)){
            return;
        }
        Draw.color(blueprintCol);
        rectCorner(0,0, builder.w*32, builder.h*32);
        Draw.color(blueprintColAccent);
        //draw border and center lines
        Lines.stroke(5 * scl);
        rectLine(0,0, builder.w*32, builder.h*32);
        int mid = builder.w/2;
        if(builder.w % 2 == 1){
            rectLine(mid*32,0, 32, builder.h*32);
        }else{
            line(mid*32,0, mid * 32, builder.h * 32);
        }
        mid = builder.h/2;
        if(builder.h % 2 == 1){
            rectLine(0,mid*32, builder.w*32,32);
        }else{
            line(0,mid*32,builder.w*32, mid * 32);
        }
        Draw.reset();

        Point2 minPoint = uiToGrid(0,0);
        int minx = Mathf.clamp(minPoint.x,0,builder.w-1);
        int miny = Mathf.clamp(minPoint.y,0,builder.h-1);

        Point2 maxPoint = uiToGrid(width,height);
        int maxx = Mathf.clamp(maxPoint.x,0,builder.w-1);
        int maxy = Mathf.clamp(maxPoint.y,0,builder.h-1);
        //draw grid
        Draw.color(blueprintColAccent);
        for(int i = minx;i<=maxx;i++){
            for(int j = miny; j <= maxy; j++){
                if(i>0 && j>0){
                    Fill.square(gx + i * 32 * scl, gy + j * 32 * scl, 3 * scl, 45);
                }
            }
        }
        //draw highlight cursor
        Point2 cursor = uiToGrid(mousex,mousey);
        if(selected == null){
            rectCorner(cursor.x*32,cursor.y*32, 32, 32);
            if(mirror && mirrorX(cursor.x)!=cursor.x){
                rectCorner(mirrorX(cursor.x)*32,cursor.y*32, 32, 32);
            }
        }

        partsToDraw.clear();
        //draw modules
        for(int j = maxy;j>=miny;j--){
            for(int i = minx;i<=maxx;i++){
                var partdata = builder.parts[builder.index(i,j)];
                if(partdata==0){
                    continue;
                }

                int px = PartData.x(partdata);
                int py = PartData.y(partdata);
                if(!(Mathf.clamp(px,minx,maxx) == i && Mathf.clamp(py,miny,maxy) == j)){
                    continue;
                }
                partsToDraw.add(partdata);
            }
        }
        int px,py;
        long partdata;
        boolean valid;


        if(scl>minimiseScl){
            //sort
            if(bucketSort[0]==null){
                for(int i = 0; i < 5; i++){
                    bucketSort[i] = new LongSeq();
                }
            }
            for(int i = 0; i < 5; i++){
                bucketSort[i].clear();
            }
            for(int i = 0; i < partsToDraw.size; i++){
                partdata = partsToDraw.get(i);
                bucketSort[PartData.Type(partdata).drawPriority].add(partdata);
            }
            int index = 0;
            for(int i = 0; i < 5; i++){
                System.arraycopy(bucketSort[i].items,0,partsToDraw.items,index,bucketSort[i].size);
                index += bucketSort[i].size;
            }

            ///draw outline
            for(int i = 0; i < partsToDraw.size; i++){
                partdata = partsToDraw.get(i);
                var type = PartData.Type(partdata);
                px = PartData.x(partdata);
                py = PartData.y(partdata);
                Draw.color();
                type.drawEditorOutline(this, px, py, builder.valid[builder.index(px, py)]);
            }
        }

        for(int i = 0;i<partsToDraw.size;i++){
            partdata = partsToDraw.get(i);
            px = PartData.x(partdata);
            py = PartData.y(partdata);
            var type = PartData.Type(partdata);
            valid = builder.valid[builder.index(px, py)];
            if(scl<minimiseScl){ // temp
                Draw.color(valid ? Color.white : Color.red);
                type.drawEditorMinimised(this,px,py, valid);
            }else{
                Draw.color(valid ? Color.white : Color.red);
                type.drawEditor(this,px,py, valid);
            }
        }

        //top
        if(scl>minimiseScl){
            for(int i = 0; i < partsToDraw.size; i++){
                partdata = partsToDraw.get(i);
                var type = PartData.Type(partdata);
                if(!type.drawsTop){
                    continue;
                }
                px = PartData.x(partdata);
                py = PartData.y(partdata);
                Draw.color();
                type.drawEditorTop(this, px, py, builder.valid[builder.index(px, py)]);
            }
        }
        //overlay
        for(int i = 0;i<partsToDraw.size;i++){
            partdata = partsToDraw.get(i);
            var type = PartData.Type(partdata);
            if(!type.drawsOverlay){
                continue;
            }
            px = PartData.x(partdata);
            py = PartData.y(partdata);
            type.drawEditorOverlay(this,px,py);
        }


        if(selected!=null){
            Color highlight = Color.white;
            var placeError = builder.canPlaceDebug(selected, cursor.x, cursor.y);
            if(placeError != PlaceProblem.NO_PROBLEM){
                highlight = Color.red;
                var pos = gridToUi((cursor.x+0.5f)*32,(cursor.y+0.5f)*32);
                Draw.color(highlight, 1f);
                text(Fonts.outline, placeError.text, pos.x,pos.y-30);
            }
            Draw.color(bgCol, 0.5f);
            rectCorner(cursor.x*32,cursor.y*32, selected.w*32, selected.h*32);
            selected.drawEditorSelect(this,cursor.x,cursor.y,false);

            if(mirror && mirrorX(cursor.x)!=cursor.x){
                Draw.color(bgCol, 0.5f);
                rectCorner(mirrorX(cursor.x,selected.w)*32,cursor.y*32, selected.w*32, selected.h*32);
                selected.drawEditorSelect(this,mirrorX(cursor.x,selected.w),cursor.y,false);
            }
        }

        for(int i =0;i<fx.size;i++){
            if(fx.get(i).startime + fx.get(i).effect.duration < Time.time){
                fx.remove(i);
                i--;
                continue;
            }
            fx.get(i).draw();
        }

        if(statmap!=null){
            statmap.drawEditor(this);
        }

        ScissorStack.pop();

    }

    public int mirrorX(int x, int w){
        return builder.w-x-w;
    }
    public int mirrorX(int x){
        return builder.w-x-1;
    }
    public void rect(TextureRegion tr,float x,float y,float sclMul){
        if(tr == null){
            Draw.rect(Core.atlas.white(), gx + (x) * scl, gy + (y) * scl);
        }else{
            Draw.rect(tr, gx + (x) * scl, gy + (y) * scl, tr.width * scl * sclMul, tr.height * scl * sclMul);
        }
    }
    public void rect(TextureRegion tr,float x,float y,float w,float h){
        Draw.rect(tr,gx + (x)*scl,gy + (y)*scl,w*scl,h*scl);
    }
    public void rectCorner(float x,float y,float w,float h){
        Fill.rect(gx + (x+w*0.5f)*scl,gy + (y+h*0.5f)*scl,w*scl,h*scl);
    }
    public void rectCorner(TextureRegion tr, float x,float y,float w,float h){
        Draw.rect(tr,gx + (x+w*0.5f)*scl,gy + (y+h*0.5f)*scl,w*scl,h*scl);
    }
    public void rectLine(float x,float y,float w,float h, float pad){
        Lines.rect(gx+x*scl,gy+y*scl, w*scl, h*scl, pad*scl,pad*scl);
    }
    public void rectLine(float x,float y,float w,float h){
        Lines.rect(gx+x*scl,gy+y*scl, w*scl, h*scl);
    }
    public void line(float x,float y,float x2,float y2){
        Lines.line(gx+x*scl,gy+y*scl, gx+x2*scl,gy+y2*scl);
    }

    public float gx(){
        return x + (width - builder.w * 32  * scl) * 0.5f + panx;
    }
    public float gy(){
        return y + (height - builder.h * 32  * scl) * 0.5f + pany;
    }

    public Point2 uiToGrid(float x,float y){
        return new Point2(Mathf.floor((x-gx()+this.x)/(32f*scl)), Mathf.floor((y-gy()+this.y)/(32f*scl)));
    }
    public Vec2 gridToUi(float x,float y){
        return new Vec2(gx+x*scl,gy+y*scl);
        }

    public enum EditorTextAlign{
        LEFT,CENTER,RIGHT
    }
    public void text(Font font, String str,float x,float y){
        text(font,str,x,y,EditorTextAlign.CENTER);
    }

    public void text(Font font, String str,float x,float y, EditorTextAlign align){
        GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        lay.setText(font, str);

        font.setColor(Draw.getColor());
        font.getCache().clear();
        switch(align){
            case LEFT:
                font.getCache().addText(str, x, y + lay.height / 2f + 1); break;
            case CENTER:
                font.getCache().addText(str, x - lay.width / 2f, y + lay.height / 2f + 1); break;
            case RIGHT:
                font.getCache().addText(str, x - lay.width, y + lay.height / 2f + 1); break;
        }
        font.getCache().draw(parentAlpha);

        Pools.free(lay);
    }



    @Override
    public float getPrefWidth(){
        return Core.graphics.getWidth();
    }
    @Override
    public float getPrefHeight(){
        return Core.graphics.getHeight();
    }

    @Override
    public float getMinWidth(){
        return 32;
    }

    @Override
    public float getMinHeight(){
        return 32;
    }

    public void select(ModularPartType type){
        selected = type;
    }

    public void deselect(){
        selected = null;
    }

    public void onAction(){
        prev.add(builder.exportFull());
        if(index!=prev.size-2){
            prev.removeRange(index+2,prev.size-1);
        }
        index++;
    }
    public void redo(){
        index++;
        if(index>=prev.size){
            index = prev.size-1;
            return;
        }
        builder.paste(prev.get(index));
    }
    public void undo(){
        index--;
        if(index<0){
            index = -1;
            return;
        }
        builder.paste(prev.get(index));

    }

    public Seq<EditorEffectWrapper> fx = new Seq<>(false);

    public static class EditorEffect{
        float duration;
        Cons2<EffectContainer,PartsEditorElement> run;

        public EditorEffect(float duration, Cons2<EffectContainer,PartsEditorElement> run){
            this.duration = duration;
            this.run = run;
        }

        public void at(PartsEditorElement e,float x, float y, float rotation, Color c){
            e.effectAt(this,x,y,rotation,c);
        }
    }

    public class EditorEffectWrapper{
        EffectContainer e;
        EditorEffect effect;
        float startime;
        EditorEffectWrapper(EditorEffect eff, float x,float y,float rotation,Color c){
            effect = eff;
            e = new EffectContainer();
            e.x = x;
            e.y =y ;
            e.rotation = rotation;
            e.color = c;
            e.lifetime = eff.duration;
            startime = Time.time;
        }

        public void draw(){
            e.time = Time.time - startime;
            effect.run.get(e,PartsEditorElement.this);
        }
    }

    public void effectAt(EditorEffect e, float x, float y, float rotation, Color c){
        fx.add(new EditorEffectWrapper(e,x,y,rotation,c));
    }

    static EditorEffect placeEffect = new EditorEffect(30f,(e,draw) -> {
        Lines.stroke(e.foutpow()*4*draw.scl,e.color);
        draw.rectLine(e.x,e.y,(e.rotation%256)*32,(Mathf.floor(e.rotation/256))*32, e.finpow()*8);
    });

    void placeEffectAt(int x,int y,int w,int h, Color c){
        placeEffect.at(this,x*32,y*32,w+h*256,c);
    }

}
