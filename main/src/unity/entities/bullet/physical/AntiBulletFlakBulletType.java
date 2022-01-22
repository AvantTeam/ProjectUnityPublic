package unity.entities.bullet.physical;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

/** @author EyeOfDarkness */
public class AntiBulletFlakBulletType extends FlakBulletType{
    public float bulletDamage = 5f;
    public float bulletSlowDownScl = 0.5f;
    public float bulletRadius = 40f;
    public Interp interp = Interp.pow3;

    public AntiBulletFlakBulletType(float speed, float damage){
        super(speed, damage);
        collidesGround = true;
        despawnHit = true;
        shrinkY = 0.2f;
    }

    @Override
    public void hit(Bullet b, float x, float y){
        super.hit(b, x, y);
        Rect r1 = Tmp.r1.setSize(bulletRadius * 2f).setCenter(b.x, b.y);
        Groups.bullet.intersect(r1.x, r1.y, r1.width, r1.height, bl -> {
            if(b.team != bl.team && bl.type.hittable && b.within(bl, bulletRadius)){
                float in = interp.apply(Mathf.clamp((bulletRadius - b.dst(bl)) / bulletRadius));
                bl.vel.scl(Mathf.lerp(1f, bulletSlowDownScl, in));
                bl.damage -= bulletDamage * in;
                if(bl.damage <= 0f) bl.remove();
            }
        });
    }
}
