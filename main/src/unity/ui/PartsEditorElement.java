package unity.ui;

import arc.*;
import arc.Graphics.Cursor.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.struct.*;
import arc.util.*;
import mindustry.input.*;
import unity.parts.*;

import static arc.Core.scene;
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
    public boolean erasing = false;
    public boolean mirror = true;
    //selection boxes?

    float panx=0,pany=0;
    float prevpx,prevpy;

    float mousex,mousey;
    float anchorx,anchory;
    float scl = 1,targetscl = 1;
    int dragTriggered = 0;


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
                if((selected!=null && builder.parts[g.x][g.y]!=null) ||
                   (selected==null && builder.parts[g.x][g.y]==null) ){
                    Core.graphics.cursor(SystemCursor.hand);
                }else{
                    Core.graphics.cursor(SystemCursor.arrow);
                }
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

    @Override
    public void act(float delta){
        super.act(delta);
        targetscl += Core.input.axis(Binding.zoom) / 10f * targetscl;
        targetscl = Mathf.clamp(targetscl,0.2f,5);
    }

    public void zoom(float am){
        targetscl *= am;
        targetscl = Mathf.clamp(targetscl,0.2f,5);
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
                if(b && mirror && mirrorX(g.x)!=g.x){
                    builder.placePart(selected, mirrorX(g.x,selected.w), g.y);
                }
            }else{
                builder.deletePartAt(g.x,g.y);
                if(mirror && mirrorX(g.x)!=g.x){
                    builder.deletePartAt(mirrorX(g.x), g.y);
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
        Lines.stroke(5);
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
                    Fill.square(gx + i * 32 * scl, gy + j * 32 * scl, 3, 45);
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


        //draw modules
        for(int i = minx;i<=maxx;i++){
            for(int j = miny;j<=maxy;j++){
                var part = builder.parts[i][j];
                if(part==null){
                    continue;
                }
                if(!(Math.max(part.getX(),minx)==i && Math.max(part.getY(),miny)==j)){
                    continue;
                }
                Draw.color(bgCol);
                rectCorner(part.getX()*32,part.getY()*32, part.type.w*32, part.type.h*32);
                Draw.color(builder.valid[i][j]?Color.white:Color.red);
                rectCorner(part.type.icon,part.getX()*32,part.getY()*32, part.type.w*32, part.type.h*32);
            }
        }

        if(selected!=null){
            Color highlight = Color.white;
            if(!builder.canPlace(selected, cursor.x, cursor.y)){
                highlight = Color.red;
            }
            Draw.color(bgCol, 0.5f);
            rectCorner(cursor.x*32,cursor.y*32, selected.w*32, selected.h*32);
            Draw.color(highlight, 0.5f);
            rectCorner(selected.icon,cursor.x*32,cursor.y*32, 32*selected.w, 32*selected.h);

            if(mirror && mirrorX(cursor.x)!=cursor.x){
                Draw.color(bgCol, 0.5f);
                rectCorner(mirrorX(cursor.x,selected.w)*32,cursor.y*32, selected.w*32, selected.h*32);
                Draw.color(highlight, 0.5f);
                rectCorner(selected.icon,mirrorX(cursor.x,selected.w)*32,cursor.y*32, 32*selected.w, 32*selected.h);
            }
        }

        ScissorStack.pop();

    }
    public int mirrorX(int x, int w){
        return builder.w-x-w;
    }
    public int mirrorX(int x){
        return builder.w-x-1;
    }

    public void rectCorner(float x,float y,float w,float h){
        Fill.rect(gx + (x+w*0.5f)*scl,gy + (y+h*0.5f)*scl,w*scl,h*scl);
    }
    public void rectCorner(TextureRegion tr, float x,float y,float w,float h){
        Draw.rect(tr,gx + (x+w*0.5f)*scl,gy + (y+h*0.5f)*scl,w*scl,h*scl);
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
        prev.add(builder.export());
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
        builder.set(prev.get(index));
    }
    public void undo(){
        index--;
        if(index<0){
            index = -1;
            return;
        }
        builder.set(prev.get(index));
    }

}
