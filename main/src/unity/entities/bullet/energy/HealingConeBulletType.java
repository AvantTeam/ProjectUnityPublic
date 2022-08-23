package unity.entities.bullet.energy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import unity.util.*;

/** @author EyeOfDarkness */
public class HealingConeBulletType extends BulletType{
    public float cone = 45f;
    public float length = 250f;
    public int scanAccuracy = 30;
    public StatusEffect allyStatus = StatusEffects.none;
    public float allyStatusDuration = 0f;
    public Color color = Pal.heal;

    private int idx = 0;

    public HealingConeBulletType(float damage){
        this.damage = damage;
        speed = 0.001f;
        hitEffect = Fx.none;
        despawnEffect = Fx.none;
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
    }

    @Override
    public float calculateRange(){
        return length;
    }

    @Override
    public float continuousDamage(){
        return damage / 30f * 60f;
    }

    @Override
    public float estimateDPS(){
        return damage * 100f / 30f * 3f;
    }

    @Override
    public void init(){
        super.init();
        drawSize = Math.max(drawSize, length * 2f);
    }

    @Override
    public void init(Bullet b){
        super.init(b);
        b.data = new float[scanAccuracy];
    }

    @Override
    public void update(Bullet b){
        if(!(b.data instanceof float[] data)) return;
        idx = 0;
        Utils.shotgunRange(scanAccuracy, cone, b.rotation(), ang -> {
            Tmp.v1.trns(ang, length).add(b);
            Vars.world.raycastEachWorld(b.x, b.y, Tmp.v1.x, Tmp.v1.y, (cx, cy) -> {
                Tile tile = Vars.world.tile(cx, cy);
                boolean bl = tile != null && tile.build != null && tile.team() != b.team && tile.block() != null && tile.block().absorbLasers;
                if(bl){
                    float dst = Math.min(b.dst(cx * Vars.tilesize, cy * Vars.tilesize), length);
                    data[idx] = dst * dst;
                }else{
                    data[idx] = length * length;
                }
                return bl;
            });
            idx++;
        });
        if(b.timer(1, 30f)){
            Tmp.r1.setCentered(b.x, b.y, 1f);
            Utils.shotgunRange(3, cone, b.rotation(), ang -> {
                Tmp.v1.trns(ang, length).add(b);
                Tmp.r1.merge(Tmp.v1);
            });

            Groups.unit.intersect(Tmp.r1.x, Tmp.r1.y, Tmp.r1.width, Tmp.r1.height, unit -> {
                if(b.within(unit, length + (unit.hitSize / 2f)) && Angles.within(b.rotation(), b.angleTo(unit), cone)){
                    int index = Mathf.clamp(Mathf.round(((Utils.angleDistSigned(b.angleTo(unit), b.rotation()) + cone) / (cone * 2f)) * (data.length - 1)), 0, data.length - 1);
                    if((b.dst2(unit) + (unit.hitSize / 2f)) < data[index]){
                        if(unit.team != b.team){
                            unit.damage(b.damage);
                            unit.apply(status, statusDuration);
                        }else{
                            unit.heal((unit.maxHealth / 100f) * healPercent);
                            unit.apply(allyStatus, allyStatusDuration);
                        }
                    }
                }
            });

            Utils.castConeTile(b.x, b.y, length, b.rotation(), cone, (building, tile) -> {
                if(building != null){
                    if(building.team == b.team){
                        if(building.damaged()){
                            building.heal(building.maxHealth / 100f * healPercent);
                            Fx.healBlockFull.at(building.x, building.y, building.block.size, Pal.heal);
                        }
                    }else{
                        building.damage(b.damage * buildingDamageMultiplier);
                    }
                }
            }, null, data);
        }
    }

    @Override
    public void draw(Bullet b){
        if(!(b.data instanceof float[] data)) return;
        float z = Draw.z();
        Draw.z(Layer.buildBeam);
        float fout = Mathf.clamp(b.time > b.lifetime - 16f ? 1f - (b.time - (b.lifetime - 16f)) / 16f : 1f) * Mathf.clamp(b.time / 16f) * length;

        Tmp.v1.trns(b.rotation() - cone, Math.min(Mathf.sqrt(data[0]), fout)).add(b);
        Draw.color(color);
        if(!Vars.renderer.animateShields) Draw.alpha(0.3f);
        for(int i = 1; i < scanAccuracy; i++){
            float ang = Mathf.lerp(-cone, cone, i / (scanAccuracy - 1f)) + b.rotation();
            Tmp.v2.trns(ang, Math.min(Mathf.sqrt(data[i]), fout)).add(b);
            Fill.tri(b.x, b.y, Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
            Tmp.v1.set(Tmp.v2);
        }

        Draw.color();
        Draw.z(z);
    }

    @Override
    public void drawLight(Bullet b){}
}
