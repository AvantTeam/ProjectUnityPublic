package unity.entities.bullet.laser;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.content.*;
import unity.content.effects.*;
import unity.util.*;

/** @author EyeOfDarkness */
public class SagittariusLaserBulletType extends BulletType{
    private static boolean hit;
    private static float hitX, hitY;

    public Color[] colors = {Pal.heal.cpy().a(0.2f), Pal.heal.cpy().a(0.5f), Pal.heal.cpy().mul(1.2f), Color.white};
    public float length = 550f, width = 45f;
    public int lasers = 8;

    public SagittariusLaserBulletType(float damage){
        super(0f, damage);
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;

        hitEffect = HitFx.coloredHitSmall;
        hitColor = Pal.heal;
    }

    @Override
    public float calculateRange(){
        return length;
    }

    @Override
    public void init(){
        super.init();
        despawnHit = false;
        drawSize = length * 2f;
    }

    @Override
    public void init(Bullet b){
        super.init(b);
        b.data = new SagittariusLaserData();
    }

    @Override
    public void update(Bullet b){
        if(b.timer(1, 5f) && b.data instanceof SagittariusLaserData data){

            float fin = b.time < 200 ? b.time / 200f : 1f;
            float sfn = (b.time < 60f ? b.time / 60f : 1f);
            float fn = sfn + (fin / 15f);
            float w = (width * sfn) / 3f;
            if(b.owner instanceof Physicsc owner){
                Tmp.v1.trns(b.rotation() + 180f, 25f * fin);
                owner.impulse(Tmp.v1);
            }

            for(int i = 0; i < lasers; i++){
                float ang = i * 360f / lasers, time = b.time * 2f;
                float sin = Mathf.sinDeg(time + ang), cos = Mathf.cosDeg(time + ang);
                Vec2
                    p = Tmp.v1.trns(b.rotation() + 90f, sin * 9f * fin, cos * 4f * fin).add(b),
                    end = Tmp.v2.trns(b.rotation() + sin * 20f * fin, length).add(p);

                hit = false;
                Utils.collideLineRawEnemy(!collidesTeam ? b.team : null, p.x, p.y, end.x, end.y, w, h -> h != b.owner && !(h instanceof Unit unit && unit.team == b.team), (building, direct) -> {
                    if(direct){
                        if(building.team != b.team){
                            building.damage(damage * fn * buildingDamageMultiplier);
                        }else{
                            building.heal(building.maxHealth * (healPercent / 100f));
                        }

                        hit = building.block.absorbLasers;
                        hitX = building.x;
                        hitY = building.y;
                    }

                    return building.block.absorbLasers;
                }, unit -> {
                    if(unit.team != b.team){
                        unit.damage(damage * fn);
                        Tmp.v3.set(unit).sub(b).nor().scl(knockback * 80f * fn);
                        unit.impulse(Tmp.v3);
                        unit.apply(status, statusDuration);
                    }

                    return false;
                }, (ex, ey) -> hit(b, ex, ey), true);

                data.length[i] = length;
                if(hit){
                    data.length[i] = b.dst(hitX, hitY);
                }
            }
        }
    }

    @Override
    public void despawned(Bullet b){
        super.despawned(b);
        if(b.owner instanceof Unit){
            ((Unit)b.owner).apply(UnityStatusEffects.speedFatigue, 6f * 60f);
        }
    }

    @Override
    public void draw(Bullet b){
        if(b.data instanceof SagittariusLaserData data){
            float cw = width + Mathf.absin(0.8f, 1.5f);
            float fout = Mathf.clamp((b.lifetime - b.time) / 16f);
            float fin = b.time < 200 ? b.time / 200f : 1f;
            float sfn = (b.time < 60f ? b.time / 60f : 1f);

            for(Color color : colors){
                float w = cw * (sfn + (fin / 15f)) * fout;
                for(int i = 0; i < lasers; i++){
                    Draw.color(color);

                    float length = data.length[i];
                    float ang = i * 360f / lasers, time = b.time * 2f;
                    float sin = Mathf.sinDeg(time + ang), cos = Mathf.cosDeg(time + ang);
                    float rot = b.rotation() + sin * 20f * fin;

                    Vec2 p = Tmp.v1.trns(b.rotation() + 90f, sin * 9f * fin, cos * 4f * fin).add(b),
                        end = Tmp.v2.trns(rot, length).add(p);

                    Lines.stroke(w);
                    Lines.line(p.x, p.y, end.x, end.y, false);
                    Drawf.tri(end.x, end.y, Lines.getStroke() * 1.22f, cw * 3 + width / 1.5f, rot);
                    Draw.color(Tmp.c1.set(color).a(Mathf.pow(color.a, lasers / 3f)));
                    Fill.circle(p.x, p.y, w);
                    if(color == colors[0]) Drawf.light(p.x, p.y, end.x, end.y, w * 1.7f * b.fout(), colors[0], 0.6f);
                }

                cw *= 0.5f;
            }
        }
    }

    @Override
    public void drawLight(Bullet b){

    }

    class SagittariusLaserData{
        float[] length = new float[lasers];
    }
}
