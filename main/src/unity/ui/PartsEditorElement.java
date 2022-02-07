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
import arc.util.*;
import unity.parts.*;

import static arc.Core.scene;
import static unity.graphics.UnityPal.*;

public class PartsEditorElement extends Element{
    //clipping
    private final Rect scissorBounds = new Rect();
    private final Rect widgetAreaBounds = new Rect();
    //

    public ModularConstructBuilder builder;
    public ModularPartType selected = null;
    public boolean erasing = false;
    public boolean mirror = true;
    //selection boxes?
    //copy paste??????

    float panx=0,pany=0;
    float prevpx,prevpy;

    float mousex,mousey;
    float anchorx,anchory;
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
            }
        });
    }

    public void onClicked(InputEvent event, float x, float y, int pointer, KeyCode button){
        if(button==KeyCode.mouseRight){
            selected = null;
            return;
        }
        var g = uiToGrid(x,y);
        Log.info(g);
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

    @Override
    public void draw(){
        float midx = x + width * 0.5f;
        float midy = y + height * 0.5f;
        float gx = gx(), gy = gy();

        Draw.color(bgCol);
        Fill.rect(midx,midy,width,height);

        widgetAreaBounds.set(x,y,width,height);
        scene.calculateScissors(widgetAreaBounds, scissorBounds);
        if(!ScissorStack.push(scissorBounds)){
            return;
        }
        Draw.color(blueprintCol);
        rectCorner(gx,gy, builder.w*32, builder.h*32);
        Draw.color(blueprintColAccent);
        //draw border and center lines
        Lines.stroke(5);
        Lines.rect(gx,gy, builder.w*32, builder.h*32);
        int mid = builder.w/2;
        if(builder.w % 2 == 1){
            Lines.rect(gx+mid*32,gy, 32, builder.h*32);
        }else{
            Lines.line(gx+mid*32,gy, gx+mid*32,gy+builder.h*32);
        }
        mid = builder.h/2;
        if(builder.h % 2 == 1){
            Lines.rect(gx,gy+mid*32, builder.w*32,32);
        }else{
            Lines.line(gx,gy+mid*32,gx+builder.w*32, gx+mid*32);
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
                    Fill.square(gx + i * 32, gy + j * 32, 3, 45);
                }
            }
        }
        //draw highlight cursor
        Point2 cursor = uiToGrid(mousex,mousey);
        if(selected == null){
            rectCorner(gx+cursor.x*32,gy+cursor.y*32, 32, 32);
            if(mirror && mirrorX(cursor.x)!=cursor.x){
                rectCorner(gx+mirrorX(cursor.x)*32,gy+cursor.y*32, 32, 32);
            }
        }


        //draw modules
        for(int i = minx;i<=maxx;i++){
            for(int j = miny;j<=maxy;j++){
                var part = builder.parts[i][j];
                if(part==null){
                    continue;
                }
                if(!part.isHere(i,j)){
                    continue;
                }
                Draw.color(bgCol);
                rectCorner(gx+i*32,gy+j*32, part.type.w*32, part.type.h*32);
                Draw.color(builder.valid[i][j]?Color.white:Color.red);
                rectCorner(part.type.icon,gx+i*32,gy+j*32, part.type.w*32, part.type.h*32);
            }
        }

        if(selected!=null){
            Color highlight = Color.white;
            if(!builder.canPlace(selected, cursor.x, cursor.y)){
                highlight = Color.red;
            }
            Draw.color(bgCol, 0.5f);
            rectCorner(gx+cursor.x*32,gy+cursor.y*32, selected.w*32, selected.h*32);
            Draw.color(highlight, 0.5f);
            rectCorner(selected.icon,gx+cursor.x*32,gy+cursor.y*32, 32*selected.w, 32*selected.h);

            if(mirror && mirrorX(cursor.x)!=cursor.x){
                Draw.color(bgCol, 0.5f);
                rectCorner(gx+mirrorX(cursor.x,selected.w)*32,gy+cursor.y*32, selected.w*32, selected.h*32);
                Draw.color(highlight, 0.5f);
                rectCorner(selected.icon,gx+mirrorX(cursor.x,selected.w)*32,gy+cursor.y*32, 32*selected.w, 32*selected.h);
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
        Fill.rect(x+w*0.5f,y+h*0.5f,w,h);
    }
    public void rectCorner(TextureRegion tr, float x,float y,float w,float h){
        Draw.rect(tr,x+w*0.5f,y+h*0.5f,w,h);
    }


    public float gx(){
        return x + (width - builder.w * 32) * 0.5f + panx;
    }
    public float gy(){
        return y + (height - builder.h * 32) * 0.5f + pany;
    }

    public Point2 uiToGrid(float x,float y){
        return new Point2(Mathf.floor((x-gx()+this.x)/32f), Mathf.floor((y-gy()+this.y)/32f));
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
}
