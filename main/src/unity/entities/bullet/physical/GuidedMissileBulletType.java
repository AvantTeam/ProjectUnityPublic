package unity.entities.bullet.physical;

import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

/** @author EyeOfDarkness */
public class GuidedMissileBulletType extends MissileBulletType{
    public float threshold = 1f;
    public float targetingInaccuracy = 17f;

    public GuidedMissileBulletType(float speed, float damage){
        super(speed, damage);
    }

    @Override
    public void update(Bullet b){
        if(b.data instanceof WeaponMount mount && homingPower > 0){
            if(targetingInaccuracy > 0.001f){
                Tmp.v1.trns(Mathf.randomSeed(b.id, 360f), Mathf.randomSeed(((long)b.id << 2L) + 351L, targetingInaccuracy));
            }else{
                Tmp.v1.setZero();
            }
            float ang = b.angleTo(mount.aimX + Tmp.v1.x, mount.aimY + Tmp.v1.y);
            b.rotation(Angles.moveToward(b.rotation(), ang, homingPower * Time.delta * 50f));
            if(Angles.within(b.rotation(), ang, threshold)){
                b.data = null;
            }
        }
        super.update(b);
    }
}
