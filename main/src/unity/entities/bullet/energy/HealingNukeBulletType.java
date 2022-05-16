package unity.entities.bullet.energy;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import unity.util.*;

/** @author EyeOfDarkness */
public class HealingNukeBulletType extends BulletType{
    public float radius = 650f;
    public int rays = 180;
    public StatusEffect allyStatus = StatusEffects.none;
    public float allyStatusDuration = 2f * 60f;

    public HealingNukeBulletType(){
        super(0f, 20f);
        hitEffect = Fx.none;
        despawnEffect = Fx.none;
        shootEffect = Fx.none;
        smokeEffect = Fx.none;
        buildingDamageMultiplier = 10f;

        collides = collidesTiles = false;
        hittable = absorbable = false;
    }

    @Override
    public float calculateRange(){
        return radius;
    }

    @Override
    public void init(){
        super.init();
        drawSize = radius * 2f;
    }

    @Override
    public void init(Bullet b){
        float[] data = Utils.castCircle(b.x, b.y, radius, rays, bd -> true, building -> {
            if(building.team == b.team){
                Fx.healBlockFull.at(building.x, building.y, building.block.size, Pal.heal);
                building.heal((healPercent / 100f) * building.maxHealth);
            }else{
                building.damage(damage * b.damageMultiplier() * buildingDamageMultiplier);
            }
        }, tile -> tile.block().absorbLasers && tile.team() != b.team);

        Units.nearby(Tmp.r1.setCentered(b.x, b.y, radius * 2f), u -> {
            float ang = b.angleTo(u);
            float dst = u.dst2(b) - ((u.hitSize * u.hitSize) / 2f);
            int idx = Mathf.mod(Mathf.round((ang % 360f) / (360f / data.length)), data.length);
            float d = data[idx];

            if(b.within(u, radius + (u.hitSize / 2f)) && dst <= d * d){
                if(u.team == b.team){
                    u.heal((healPercent / 100f) * u.maxHealth);
                    u.apply(allyStatus, allyStatusDuration);
                }else{
                    u.damage(damage * b.damageMultiplier());
                    u.apply(status, statusDuration);
                }
            }
        });

        b.data = data;
    }

    @Override
    public void draw(Bullet b){
        if(b.data instanceof float[] data){
            Draw.color(Pal.heal);
            Draw.alpha(0.3f * b.fout());
            for(int i = 0; i < data.length; i++){
                float ang1 = i * (360f / data.length),
                ang2 = (i + 1f) * (360f / data.length),
                len1 = data[i], len2 = data[(i + 1) % data.length];
                Vec2 v1 = Tmp.v1.trns(ang1, len1).add(b),
                v2 = Tmp.v2.trns(ang2, len2).add(b);

                Fill.tri(b.x, b.y, v1.x, v1.y, v2.x, v2.y);
            }

            Draw.reset();
        }
    }
}
