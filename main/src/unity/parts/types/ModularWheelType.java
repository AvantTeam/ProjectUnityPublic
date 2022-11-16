package unity.parts.types;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.content.effects.*;
import unity.gen.*;
import unity.graphics.*;
import unity.parts.*;
import unity.parts.stat.*;
import unity.ui.*;
import unity.util.*;

public class ModularWheelType extends ModularPartType{
    public TextureRegion moving;
    public boolean isWheel = false;
    public ModularWheelType(String name){
        super(name);
        open = true;
        drawPriority = 0;
        updates = true;
    }

    @Override
    public void load(){
        super.load();
        moving = getPartSprite("unity-part-"+name+"-moving");
    }
    private float steerStr;
    public void wheel (float speedPerRps, float traction, float nominalWeight, float steerStrength){
        stats.add(new WheelStat(speedPerRps,traction, nominalWeight,steerStrength));
        steerStr = steerStrength;
    }

    private DrawTransform dt = new DrawTransform(new Vec2(0,0),0);

    @Override
    public void update(Entityc ent, ModularPart part){
        if(ent instanceof ModularUnitc unit){
            float vellen = unit.vel().len();
            if(!(unit.vel().len() > 0.01f && unit.elevation()<0.01)){
                dt.setTranslate(unit.x(),unit.y());
                dt.setRotation(unit.rotation());

                //driveDist += vellen;

                float dustvel = 0;
                if(unit.moving()){
                    dustvel = vellen - unit.speed();
                }

                Tmp.v1.set(unit.vel()).scl(dustvel * 40 / vellen);//
                Vec2 nvt = new Vec2();
                boolean b = part.getY() - 1 < 0 || (unit.construct().parts[part.getX()][part.getY() - 1] != null && unit.construct().parts[part.getX()][part.getY() - 1].type instanceof ModularWheelType);
                if((Mathf.random() < 0.1) && b){
                    Tmp.v2.set(part.cx * ModularPartType.partSize, part.ay * ModularPartType.partSize);
                    dt.transform(Tmp.v2);
                    nvt.set(Tmp.v1.x + Mathf.range(3), Tmp.v1.y + Mathf.range(3));
                    Tile t = Vars.world.tileWorld(Tmp.v2.x, Tmp.v2.y);
                    if(t != null){
                        OtherFx.dust.at(Tmp.v2.x, Tmp.v2.y, 0, t.floor().mapColor, nvt);
                    }

                }
            }
            if(part.prop_index>=0){
                float relativeMotion = vellen + part.cx * Mathf.degreesToRadians * unit.rotateVel() * partSize;
                unit.partTransientProp()[part.prop_index] += relativeMotion;
            }

        }
    }


    @Override
    public void draw(DrawTransform transform, ModularPart part, Entityc entityc){
        super.draw(transform, part,entityc);
        float driveDist = 0;
        float rotationOffset = 0;
        if(entityc instanceof ModularUnitc unit){
            driveDist = unit.partTransientProp()!=null?unit.partTransientProp()[part.prop_index]:0;
            if(isWheel && part.cy > 0.5){
                rotationOffset = Mathf.clamp(unit.steerAngle(),-steerStr*20,steerStr*20);
            }
        }
        Vec2 pos = new Vec2(part.cx * partSize, part.cy * partSize);
        transform.transform(pos);


        transform.drawRect(outline[0], part.cx * partSize, part.cy * partSize);
        drawTreads(pos.x, pos.y, partSize, transform.getRotation() - 90 + rotationOffset, driveDist);
    }

    @Override
    public void drawEditor(PartsEditorElement editor, int x, int y, boolean valid){
        var point = editor.gridToUi((x+w*0.5f)*32 ,(y+h*0.5f)*32);
        float rollspd = editor.statmap != null? ((ModularUnitStatMap)editor.statmap).rps :0;
        drawTreads(point.x,point.y,32f * editor.scl, 0 ,Time.time * rollspd / 4f);
    }

    void drawTreads(float x, float y, float scl, float rotation, float rollDist){
        if(h<=w){
            float ang = rollDist*16/h;
            for(int i = 0; i < 4; i++){
                UnityDrawf.drawRotRect(moving, x, y, w * scl, moving.height * scl/16f, h * scl, rotation, ang + i * 90, ang + i * 90 + 90);
            }
        }else{
            float r = 0.5f * w;
            float lscl = (moving.height/partSize) * Draw.scl;
            float treadlength = (h - (r * 2) + r * Mathf.pi * 2) * partSize;
            float offset = (rollDist * Draw.scl) % lscl;
            for(float i = 0; i < treadlength; i += lscl){
                UnityDrawf.drawTread(moving, x, y, w * scl, (h + 0.5f) * scl, scl * 0.5f * w, rotation, (i + offset)*scl , (i + offset + lscl)*scl);
            }
        }
    }
}
