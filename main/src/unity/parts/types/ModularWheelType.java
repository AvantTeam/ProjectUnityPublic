package unity.parts.types;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import unity.graphics.*;
import unity.parts.*;
import unity.parts.stat.*;
import unity.util.*;

public class ModularWheelType extends ModularPartType{
    TextureRegion base,moving;
    public ModularWheelType(String name){
        super(name);
        open = true;
    }

    @Override
    public void load(){
        super.load();
        base = getPartSprite("unity-part-"+name+"-base");
        moving = getPartSprite("unity-part-"+name+"-moving");
    }

    public void wheel (float wheelStrength, float nominalWeight, float maxSpeed){
        stats.add(new WheelStat(wheelStrength,nominalWeight,maxSpeed));
    }

    //ugly but uh
    public static float rollDistance = 0;
    @Override
    public void draw(DrawTransform transform, ModularPart part){
        super.draw(transform, part);
        Vec2 pos = new Vec2(part.cx*partSize,part.cy*partSize);
        transform.transform(pos);
        float ang = rollDistance*16;

        transform.drawRect(base,part.cx*partSize,part.cy*partSize);
        if(h==1){
            for(int i = 0; i < 4; i++){
                UnityDrawf.drawRotRect(moving, pos.x, pos.y, w * partSize, moving.height * Draw.scl, moving.height * Draw.scl, transform.getRotation() - 90, ang + i * 90, ang + i * 90 + 90);
            }
        }else{
            float treadlength = h*partSize-partSize + partSize* Mathf.pi;
            float offset = (rollDistance * Draw.scl) % (moving.height * Draw.scl);
            for(float i =0;i < treadlength;i += moving.height* Draw.scl){
                UnityDrawf.drawTread(moving, pos.x, pos.y, w * partSize, (h+0.5f) * partSize, partSize*0.5f, transform.getRotation() - 90, i + offset, i + offset + moving.height* Draw.scl);
            }
        }
        //transform.drawRect(base,part.ax*partSize,part.ay*partSize);
    }
}
