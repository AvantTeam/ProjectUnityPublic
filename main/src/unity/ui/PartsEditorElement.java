package unity.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import unity.parts.*;

import static arc.Core.scene;
import static unity.graphics.UnityPal.*;

public class PartsEditorElement extends Element{
    //clipping
    private final Rect scissorBounds = new Rect();
    private final Rect widgetAreaBounds = new Rect();
    //

    ModularConstructBuilder builder;
    ModularPartType selected = null;
    boolean erasing = false;
    //selection boxes?
    //copy paste??????

    float panx=0,pany=0;

    float mousex,mousey;

    public PartsEditorElement(ModularConstructBuilder builder){
        this.builder = builder;
        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                onClicked(event,x,y,pointer,button);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                super.touchUp(event, x, y, pointer, button);
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer){
                super.touchDragged(event, x, y, pointer);
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                mousex = x;
                mousey = y;
                return true;
            }
        });
    }

    public void onClicked(InputEvent event, float x, float y, int pointer, KeyCode button){

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
        rectCorner(gx,gy, builder.w*32, builder.w*32);
        Draw.color(blueprintColAccent);
        Lines.stroke(5);
        linerectCorner(gx,gy, builder.w*32, builder.w*32);

        ScissorStack.pop();

    }


    public void rectCorner(float x,float y,float w,float h){
        Fill.rect(x-w*0.5f,y-w*0.5f,w,h);
    }
    public void rectCorner(TextureRegion tr, float x,float y,float w,float h){
        Draw.rect(tr,x-w*0.5f,y-w*0.5f,w,h);
    }
    public void linerectCorner(float x,float y,float w,float h){
        Lines.rect(x-w*0.5f,y-w*0.5f,w,h);
    }


    public float gx(){
        return (x + width - builder.w * 32) * 0.5f + panx;
    }
    public float gy(){
        return (x + width - builder.w * 32) * 0.5f + pany;
    }

    public Point2 uiToGrid(float x,float y){
        return new Point2(Mathf.floor((x-gx())/32f), Mathf.floor((y-gy())/32f));
    }

    @Override
    public float getPrefWidth(){
        return Core.graphics.getWidth();
    }
    @Override
    public float getPrefHeight(){
        return Core.graphics.getWidth();
    }

    @Override
    public float getMinWidth(){
        return builder.w*32;
    }

    @Override
    public float getMinHeight(){
        return builder.h*32;
    }

    public void select(ModularPartType type){
        selected = type;
    }

    public void deselect(){
        selected = null;
    }
}
