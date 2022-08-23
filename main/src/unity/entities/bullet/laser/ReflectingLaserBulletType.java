package unity.entities.bullet.laser;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.util.*;

public class ReflectingLaserBulletType extends BulletType{
    private static int p = 0;
    private final static Vec2 vec = new Vec2();

    public Color[] colors = {};
    public float length = 500f, reflectLength = 200f;
    public float width = 65f, lengthFalloff = 0.5f;
    public float reflectRange = 80f, reflectLoss = 0.75f;
    public float minimumTargetLength = 70f;
    public int reflections = 5;
    public int reflectLightning = 10;

    public ReflectingLaserBulletType(float damage){
        super(0f, damage);
        lifetime = 16f;
        impact = true;
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
    }

    @Override
    public void init(){
        super.init();
        drawSize = length * 2f;
        despawnHit = false;
    }

    @Override
    public void init(Bullet b){
        super.init(b);

        b.fdata = b.data == null ? length : reflectLength;
        if(b.data == null){
            ReflectLaserData data = new ReflectLaserData();
            data.reflected = new IntSet();
            b.data = data;
        }

        if(b.data instanceof ReflectLaserData data){
            float length = b.fdata;
            Vec2 pos = vec.trns(b.rotation(), length).add(b);
            p = 0;
            Utils.collideLineRawEnemy(collidesTeam ? null : b.team, b.x, b.y, pos.x, pos.y, width / 3f, collidesTiles, true, true, (x, y, h, direct) -> {
                boolean hit = p > pierceCap;
                if(direct){
                    if(h instanceof Teamc t){
                        if(t.team() != b.team){
                            data.hitX = x;
                            data.hitY = y;
                            data.hit = true;
                            if(h instanceof Hitboxc){
                                hitEntity(b, (Hitboxc)h, h.health());
                            }

                            hit(b, x, y);
                            if(!b.within(h, minimumTargetLength)) p++;
                        }else{
                            h.heal(h.maxHealth() * (healPercent / 100f));
                        }
                    }
                }

                if(h instanceof Building block && block.team != b.team){
                    hit |= block.block.absorbLasers;
                }
                return hit;
            });

            if(data.hit){
                Vec2 hit = Intersector.nearestSegmentPoint(b.x, b.y, pos.x, pos.y, data.hitX, data.hitY, Tmp.v2);

                float hx = hit.x;
                float hy = hit.y;
                b.fdata = b.dst(hx, hy);

                if(data.reflect < reflections){
                    float delay = lifetime * 0.2f;
                    Posc n = Units.closestTarget(b.team, hx, hy, reflectLength,
                        unit -> unit.isValid() && valid(hx, hy, b.rotation(), data.lastRot, unit, data.reflected),
                        building -> valid(hx, hy, b.rotation(), data.lastRot, building, data.reflected));

                    float nextAngle = n == null ? b.rotation() + 180f + Mathf.range(reflectRange / 2f, reflectRange) : n.angleTo(hx, hy) + 180f;
                    ReflectLaserData d = new ReflectLaserData();
                    d.reflect = data.reflect + 1;
                    d.lastRot = (b.rotation() + 360f) % 360f;
                    d.reflected = data.reflected;
                    Time.run(delay, () -> {
                        if(b.isAdded() && b.type == this){
                            hitReflect(b, hx, hy);
                            createAlt(b, hx, hy, nextAngle, reflectLength, d);
                        }
                    });
                }
            }
        }
    }

    boolean valid(float x, float y, float angle, float angle2, Posc pos, IntSet collided){
        float angleTo = pos.angleTo(x, y);
        return !pos.within(x, y, minimumTargetLength) && Angles.within(angleTo, angle, reflectRange) && (angle2 <= -1f || !Angles.within(angleTo + 180f, angle2, reflectRange / 2f)) && (collided.add(pos.id()));
    }

    void hitReflect(Bullet b, float x, float y){
        for(int i = 0; i < reflectLightning; i++){
            Lightning.create(b, lightningColor, lightningDamage < 0 ? damage : lightningDamage, x, y, b.rotation() + Mathf.range(lightningCone/2) + lightningAngle, lightningLength + Mathf.random(lightningLengthRand));
        }
    }

    @Override
    public void draw(Bullet b){
        if(b.data instanceof ReflectLaserData data){
            boolean hit = data.hit && data.reflect < reflections;
            float len = b.fdata;
            float f = Mathf.curve(b.fin(), 0f, 0.2f);
            float cl = len * f;
            float cw = width;
            Vec2 p = Tmp.v1.trns(b.rotation(), cl).add(b);

            Lines.line(b.x, b.y, p.x, p.y);
            for(Color color : colors){
                Draw.color(color);
                Lines.stroke(cw * b.fout());
                Lines.line(b.x, b.y, p.x, p.y, false);

                if(!hit){
                    Drawf.tri(p.x, p.y, Lines.getStroke() * 1.22f, cw * 2 + width / 2f, b.rotation());
                }else{
                    Fill.circle(p.x, p.y, cw * b.fout() / 2f);
                }

                Fill.circle(b.x, b.y, cw * b.fout());

                cw *= lengthFalloff;
            }

            Tmp.v2.set(p).sub(b).scl(1.1f).add(b);

            Drawf.light(b.x, b.y, Tmp.v2.x, Tmp.v2.y, width * 1.4f * b.fout(), colors[0], 0.6f);
        }
    }

    @Override
    public void drawLight(Bullet b){

    }

    void createAlt(Bullet s, float x, float y, float rotation, float length, ReflectLaserData data){
        Bullet b = Bullet.create();
        b.x = x;
        b.y = y;
        b.type = this;
        b.owner = s.owner;
        b.team = s.team;
        b.time = 0f;
        b.lifetime = lifetime;
        b.initVel(rotation, 0f);
        b.fdata = length;
        b.data = data;
        b.drag = 0f;
        b.hitSize = hitSize;
        b.damage = s.damage * reflectLoss;
        b.add();
    }

    static class ReflectLaserData{
        int reflect = 0;
        boolean hit = false;
        float hitX, hitY, lastRot = -1f;
        IntSet reflected;
    }
}
