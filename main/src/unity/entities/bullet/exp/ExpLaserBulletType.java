package unity.entities.bullet.exp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.content.*;
import unity.graphics.*;
import unity.util.*;
import unity.world.blocks.exp.*;

public class ExpLaserBulletType extends ExpBulletType {
    /** Dimensions of laser */
    public float width = 1f, length;
    /** Length increase per owner level, if the owner can level up. */
    public float lengthInc;
    /** Widths of each color */
    public float[] strokes = {2.9f, 1.8f, 1};
    /** Exp gained on hit */
    public int buildingExpGain;
    public boolean hitMissed = false;
    public boolean blip = false;

    public ExpLaserBulletType(float length, float damage){
        super(0.01f, damage);
        this.length = length;
        ammoMultiplier = 1;
        drawSize = length * 2f;
        hitSize = 0f;
        hitEffect = Fx.hitLiquid;
        shootEffect = Fx.hitLiquid;
        lifetime = 18f;
        despawnEffect = Fx.none;
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
        expOnHit = false;
    }

    public ExpLaserBulletType(){
        this(120f, 1f);
    }

    public float getLength(Bullet b){
        return length + lengthInc * getLevel(b);
    }

    @Override
    public float calculateRange(){
        return Math.max(length, maxRange);
    }

    @Override
    public void init(Bullet b){
        super.init(b);
        despawnHit = false;

        setDamage(b);

        Healthc target = Utils.linecast(b, b.x, b.y, b.rotation(), getLength(b));
        b.data = target;

        if(target instanceof Hitboxc hit){
            hit.collision(b, hit.x(), hit.y());
            b.collision(hit, hit.x(), hit.y());
            handleExp(b, hit.x(), hit.y(), expGain);
        }else if(target instanceof Building tile && tile.collide(b)){
            tile.collision(b);
            hit(b, tile.x, tile.y);
            handleExp(b, tile.x, tile.y, expGain);
        }else{
            float dst = (b.lifetime / lifetime) * range;
            Vec2 v = new Vec2().trns(b.rotation(), scaleLife ? Math.min(dst, getLength(b)) : getLength(b)).add(b.x, b.y);
            b.data = v;
            if(hitMissed) hit(b, v.x, v.y);
        }
    }

    @Override
    public void draw(Bullet b){
        if(b.data instanceof Position point){
            Tmp.v1.set(point);

            Draw.color(getColor(b));

            Draw.alpha(0.4f);
            Lines.stroke(b.fout() * width * strokes[0]);
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);

            Draw.alpha(1);
            Lines.stroke(b.fout() * width * strokes[1]);
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);

            Draw.color(Color.white);
            Lines.stroke(b.fout() * width * strokes[2]);
            Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);

            if(blip){
                Draw.color(Color.white, Tmp.c2, b.fin());
                Lines.circle(Tmp.v1.x, Tmp.v1.y, b.finpow() * width * 5f);
            }
            Draw.reset();

            Drawf.light(b.x, b.y, Tmp.v1.x, Tmp.v1.y, width * 10 * b.fout(), Color.white, 0.6f);
        }
    }
    

    @Override
    public void drawLight(Bullet b){
        //no light drawn here
    }
}
