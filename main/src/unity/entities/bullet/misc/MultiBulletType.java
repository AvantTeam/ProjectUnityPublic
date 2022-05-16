package unity.entities.bullet.misc;

import arc.audio.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;

/**
 * Experimental multi bullet type.
 * @author EyeOfDarkness
 */
public class MultiBulletType extends BulletType{
    public boolean mirror = true;
    public Sound shootSound = Sounds.none;
    public MultiBulletData[] bullets = {};

    public MultiBulletType(){
        hittable = false;
        absorbable = false;
        despawnEffect = hitEffect = shootEffect = Fx.none;
        collides = collidesTiles = false;
        lifetime = 0f;
        speed = 0f;
        damage = 0f;
    }

    @Override
    public float calculateRange(){
        float max = 0f;
        for(MultiBulletData b : bullets){
            max = Math.max(max, b.type.range);
        }
        return max;
    }

    @Override
    public float estimateDPS(){
        float sum = 0f;
        for(MultiBulletData b : bullets){
            float x = (mirror && (b.x != 0f || b.rotation != 0f)) ? 2f : 1f;
            sum += b.type.estimateDPS() * x;
        }
        return sum;
    }

    @Override
    public void init(Bullet b){
        if(lifetime <= 0f){
            b.remove();
        }else if(b.owner instanceof Unit e){
            b.data = new Vec2(b.x - e.x, b.y - e.y).rotate(-e.rotation);
            for(WeaponMount mount : e.mounts){
                if(mount.weapon.bullet == this){
                    Weapon w = mount.weapon;
                    float mx = e.x + Angles.trnsx(e.rotation - 90, w.x, w.y),
                    my = e.y + Angles.trnsy(e.rotation - 90, w.x, w.y),
                    weaponRotation = e.rotation - 90 + (w.rotate ? mount.rotation : 0),
                    bx = mx + Angles.trnsx(weaponRotation, w.shootX, w.shootY),
                    by = my + Angles.trnsy(weaponRotation, w.shootX, w.shootY);
                    b.fdata = Mathf.dst(bx, by, mount.aimX, mount.aimY);
                    break;
                }
            }
        }
    }

    @Override
    public void update(Bullet b){
        if(b.data instanceof Vec2 v && b.owner instanceof Unit e){
            Tmp.v1.set(v).rotate(e.rotation).add(e);
            b.set(Tmp.v1);
            b.rotation(e.rotation);
        }
    }

    @Override
    public void despawned(Bullet b){
        if(bullets.length > 0 && b.owner instanceof Unit e && e.isAdded()){
            for(MultiBulletData data : bullets){
                float scl = data.type.scaleLife ? Mathf.clamp(b.fdata / data.type.range) : 1f;
                Tmp.v1.trns(b.rotation(), data.x, data.y).add(b);
                data.type.create(b.owner, b.team, Tmp.v1.x, Tmp.v1.y, b.rotation() + data.rotation, 1f, scl);
                data.type.shootEffect.at(Tmp.v1.x, Tmp.v1.y, b.rotation() + data.rotation);
                if(mirror && (data.x != 0f || data.rotation != 0f)){
                    Tmp.v1.trns(b.rotation(), -data.x, data.y).add(b);
                    data.type.create(b.owner, b.team, Tmp.v1.x, Tmp.v1.y, b.rotation() - data.rotation, 1f, scl);
                    data.type.shootEffect.at(Tmp.v1.x, Tmp.v1.y, b.rotation() - data.rotation);
                }
            }
            shootSound.at(b, Mathf.random(0.9f, 1.1f));
        }
    }

    @Override
    public void hit(Bullet b, float x, float y){

    }

    @Override
    public void drawLight(Bullet b){

    }

    @Override
    public Bullet create(Entityc owner, Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl, Object data){
        Bullet b = super.create(owner, team, x, y, angle, damage, velocityScl, lifetimeScl, data);
        b.vel.setZero();
        return b;
    }

    public static class MultiBulletData{
        BulletType type;
        float x, y, rotation;

        public MultiBulletData(BulletType type, float x, float y, float rotation){
            this.type = type;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }
    }
}
