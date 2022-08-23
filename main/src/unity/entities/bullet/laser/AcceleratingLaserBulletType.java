package unity.entities.bullet.laser;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.util.*;

/** @author EyeOfDarkness */
public class AcceleratingLaserBulletType extends BulletType{
    public float maxLength = 1000f;
    public float laserSpeed = 15f;
    public float accel = 25f;
    public float width = 12f, collisionWidth = 8f;
    public float fadeTime = 60f;
    public float fadeInTime = 8f;
    public float oscOffset = 1.4f, oscScl = 1.1f;
    public float pierceAmount = 4f;
    public boolean fastUpdateLength = true;
    public Color[] colors = {Color.valueOf("ec745855"), Color.valueOf("ec7458aa"), Color.valueOf("ff9c5a"), Color.white};
    public Boolf2<Bullet, Building> buildingInsulator = (b, building) -> building.block.absorbLasers || building.health > (damage * buildingDamageMultiplier) / 2f;
    public Boolf2<Bullet, Unit> unitInsulator = (b, unit) -> unit.health > damage / 2f && unit.hitSize > width;

    public AcceleratingLaserBulletType(float damage){
        super(0f, damage);
        despawnEffect = Fx.none;
        collides = false;
        pierce = true;
        impact = true;
        keepVelocity = false;
        hittable = false;
        absorbable = false;
    }

    @Override
    public float estimateDPS(){
        return damage * (lifetime / 2f) / 5f * 3f;
    }

    @Override
    public float continuousDamage(){
        return damage / 5f * 60f;
    }

    @Override
    public float calculateRange(){
        return maxRange > 0 ? maxRange : maxLength / 1.5f;
    }

    @Override
    public void init(){
        super.init();
        drawSize = maxLength * 2f;
        despawnHit = false;
    }

    @Override
    public void draw(Bullet b){
        float fadeIn = fadeInTime <= 0f ? 1f : Mathf.clamp(b.time / fadeInTime);
        float fade = Mathf.clamp(b.time > b.lifetime - fadeTime ? 1f - (b.time - (lifetime - fadeTime)) / fadeTime : 1f) * fadeIn;
        float tipHeight = width / 2f;

        Lines.lineAngle(b.x, b.y, b.rotation(), b.fdata);
        for(int i = 0; i < colors.length; i++){
            float f = ((float)(colors.length - i) / colors.length);
            float w = f * (width + Mathf.absin(Time.time + (i * oscOffset), oscScl, width / 4)) * fade;

            Tmp.v2.trns(b.rotation(), b.fdata - tipHeight).add(b);
            Tmp.v1.trns(b.rotation(), width * 2f).add(Tmp.v2);
            Draw.color(colors[i]);
            Fill.circle(b.x, b.y, w / 2f);
            Lines.stroke(w);
            Lines.line(b.x, b.y, Tmp.v2.x, Tmp.v2.y, false);
            for(int s : Mathf.signs){
                Tmp.v3.trns(b.rotation(), w * -0.7f, w * s);
                Fill.tri(Tmp.v2.x, Tmp.v2.y, Tmp.v1.x, Tmp.v1.y, Tmp.v2.x + Tmp.v3.x, Tmp.v2.y + Tmp.v3.y);
            }
        }
        Tmp.v2.trns(b.rotation(), b.fdata + tipHeight).add(b);
        Drawf.light(b.x, b.y, Tmp.v2.x, Tmp.v2.y, width * 2f, colors[0], 0.5f);
        Draw.reset();
    }

    @Override
    public void drawLight(Bullet b){

    }

    @Override
    public void init(Bullet b){
        super.init(b);
        b.data = new LaserData();
    }

    @Override
    public void update(Bullet b){
        boolean timer = b.timer(0, 5f);

        if(b.data instanceof LaserData){
            LaserData vec = (LaserData)b.data;
            if(vec.restartTime >= 5f){
                if(accel > 0.01f){
                    vec.velocity = Mathf.clamp((vec.velocityTime / accel) + vec.velocity, 0f, laserSpeed);
                    b.fdata = Mathf.clamp(b.fdata + (vec.velocity * Time.delta), 0f, maxLength);
                    vec.velocityTime += Time.delta;
                }else if(timer){
                    b.fdata = maxLength;
                }
            }else{
                vec.restartTime += Time.delta;
                if(fastUpdateLength && vec.target != null){
                    vec.pierceOffsetSmooth = Mathf.lerpDelta(vec.pierceOffsetSmooth, vec.pierceOffset, 0.2f);
                    Tmp.v2.trns(b.rotation(), maxLength * 1.5f).add(b);
                    float dst = Intersector.distanceLinePoint(b.x, b.y, Tmp.v2.x, Tmp.v2.y, vec.target.getX(), vec.target.getY());
                    b.fdata = ((b.dst(vec.target) - vec.targetSize) + dst) + pierceAmount + (vec.pierceOffsetSmooth * vec.targetSize);
                }
            }
        }

        if(timer){
            boolean p = pierceCap > 0;
            Tmp.v1.trns(b.rotation(), b.fdata).add(b);
            if(b.data instanceof LaserData data){
                if(p){
                    data.pierceScore = 0f;
                    data.pierceOffset = 0f;
                }

                Utils.collideLineRawEnemyRatio(b.team, b.x, b.y, Tmp.v1.x, Tmp.v1.y, collisionWidth, (building, ratio, direct) -> {
                    boolean h = buildingInsulator.get(b, building);
                    if(direct){
                        if(h){
                            if(p) data.pierceScore += building.block.size * (building.block.absorbLasers ? 3f : 1f) * ratio;
                            if(!p || data.pierceScore >= pierceCap){
                                Tmp.v2.trns(b.rotation(), maxLength * 1.5f).add(b);
                                float dst = Intersector.distanceLinePoint(b.x, b.y, Tmp.v2.x, Tmp.v2.y, building.x, building.y);
                                data.velocity = 0f;
                                data.restartTime = 0f;
                                data.velocityTime = 0f;
                                data.pierceOffset = 1f - Mathf.clamp(data.pierceScore - pierceCap);
                                if(fastUpdateLength){
                                    if(building != data.target) data.pierceOffsetSmooth = data.pierceOffset;
                                    data.target = building;
                                    data.targetSize = building.block.size * Vars.tilesize / 2f;
                                }
                                b.fdata = ((b.dst(building) - (building.block.size * Vars.tilesize / 2f)) + dst) + pierceAmount + (data.pierceOffsetSmooth * data.targetSize);
                            }
                        }
                        building.damage(damage * buildingDamageMultiplier * ratio);
                    }
                    return !p ? h : data.pierceScore >= pierceCap;
                }, (unit, ratio) -> {
                    boolean h = unitInsulator.get(b, unit);
                    if(h){
                        if(p) data.pierceScore += (((unit.hitSize / Vars.tilesize) / 2f) + (unit.health / 4000f)) * ratio;
                        if(!p || data.pierceScore >= pierceCap){
                            Tmp.v2.trns(b.rotation(), maxLength * 1.5f).add(b);
                            float dst = Intersector.distanceLinePoint(b.x, b.y, Tmp.v2.x, Tmp.v2.y, unit.x, unit.y);
                            data.velocity = 0f;
                            data.restartTime = 0f;
                            data.velocityTime = 0f;
                            data.pierceOffset = 1f - Mathf.clamp(data.pierceScore - pierceCap);
                            if(fastUpdateLength){
                                if(unit != data.target) data.pierceOffsetSmooth = data.pierceOffset;
                                data.target = unit;
                                data.targetSize = unit.hitSize / 2f;
                            }
                            b.fdata = ((b.dst(unit) - (unit.hitSize / 2f)) + dst) + pierceAmount + (data.pierceOffsetSmooth * data.targetSize);
                        }
                    }
                    //hitEntity(b, unit, unit.health);
                    hitEntityAlt(b, unit, b.damage * ratio);
                    return !p ? h : data.pierceScore >= pierceCap;
                }, (ex, ey) -> hit(b, ex, ey));
            }
        }
    }

    void hitEntityAlt(Bullet b, Unit unit, float damage){
        unit.damage(damage);
        Tmp.v3.set(unit).sub(b).nor().scl(knockback * 80f);
        if(impact) Tmp.v3.setAngle(b.rotation() + (knockback < 0 ? 180f : 0f));
        unit.impulse(Tmp.v3);
        unit.apply(status, statusDuration);
    }

    public static class LaserData{
        public float lastLength, lightningTime, velocity, velocityTime, targetSize, pierceOffset, pierceOffsetSmooth, pierceScore, restartTime = 5f;
        public Position target;
    }
}
