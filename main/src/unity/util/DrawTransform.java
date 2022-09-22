package unity.util;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;

public class DrawTransform{
    static Vec2 temp = new Vec2();
    Vec2 n1 = new Vec2(),n2 = new Vec2(),transl = new Vec2();
    float rot=0,scl=1;

    public DrawTransform(Vec2 transl, float rot){
        this.transl.set(transl);
        this.rot = rot;
        recalc();
    }
    public DrawTransform(){
        recalc();
    }

    public void setTranslate(Vec2 transl){
        this.transl.set(transl);
    }
    public void setTranslate(float x,float y){
       this.transl.set(x,y);
    }

    public float getRotation(){
        return rot;
    }

    public void setRotation(float rot){
        this.rot = rot;
        recalc();
    }

    public void setScale(float scl){
        this.scl = scl;
        recalc();
    }

    private void recalc(){
        n1.set(Mathf.cosDeg(90-rot)*scl,-Mathf.sinDeg(90-rot)*scl);
        n2.set(Mathf.sinDeg(90-rot)*scl,Mathf.cosDeg(90-rot)*scl);
    }

    public void transform(Vec2 in){
        in.set(in.x*n1.x + in.y*n2.x , in.x*n1.y + in.y*n2.y).add(transl);
    }

    public void drawRect(TextureRegion t,float x,float y){
        transform(temp.set(x,y));
        Draw.rect(t,temp.x,temp.y,rot-90); //replace with the raw quad drawing.
    }
    public void drawRect(TextureRegion t,float x,float y,float w,float h){
       transform(temp.set(x,y));
       Draw.rect(t,temp.x,temp.y,w*Draw.scl,h*Draw.scl,rot-90);
    }
    public void drawRectScl(TextureRegion t,float x,float y,float w,float h){
       transform(temp.set(x,y));
       Draw.rect(t,temp.x,temp.y,t.width*w*Draw.scl,t.height*h*Draw.scl,rot-90);
    }
    public void drawRect(TextureRegion t,float x,float y,float rotation){
       transform(temp.set(x,y));
       Draw.rect(t,temp.x,temp.y,rot-90 + rotation);
    }

}
