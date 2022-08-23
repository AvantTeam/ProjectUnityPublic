package unity.entities.bullet.physical;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import unity.content.effects.*;
import unity.util.*;

/**
 * RailBulletType with modifiable speed.
 * @author EyeOfDarkness
 */
public class SlowRailBulletType extends BasicBulletType{
    public float trailSpacing = 5f;
    public float collisionWidth = 3f;
    public float pierceDamageFactor = 0f;
    private static boolean hit = false;

    public SlowRailBulletType(float speed, float damage){
        super(speed, damage);
        collides = collidesTiles = backMove = reflectable = false;
        pierce = pierceBuilding = true;
        trailEffect = TrailFx.coloredRailgunTrail;
    }

    @Override
    public void init(){
        super.init();
        drawSize = Math.max(drawSize, (Math.max(height, width) + (speed * lifetime * 0.75f)) * 2f);
        trailColor = backColor;
        trailLength = Math.max((int)(lifetime), 2);
    }

    @Override
    public void init(Bullet b){
        super.init(b);
        RailData data = new RailData();
        data.x = b.x;
        data.y = b.y;
        b.data = data;
    }

    @Override
    public void update(Bullet b){
        updateTrail(b);
        hit = false;
        Utils.collideLineRawEnemy(b.team, b.lastX, b.lastY, b.x, b.y, collisionWidth, collisionWidth, (building, direct) -> {
            if(direct && collidesGround && !b.collided.contains(building.id) && b.damage > 0f){
                float h = building.health;
                float sub = Math.max(building.health * pierceDamageFactor, 0);
                building.collision(b);
                hitTile(b, building, b.x, b.y, h, true);
                b.collided.add(building.id);
                b.damage -= sub;
            }
            return (hit = (building.block.absorbLasers || (pierceCap > 0 && b.collided.size >= pierceCap) || b.damage <= 0f));
        }, unit -> {
            if(unit.checkTarget(collidesAir, collidesGround) && !b.collided.contains(unit.id) && b.damage > 0f){
                float sub = Math.max(unit.health * pierceDamageFactor, 0);
                hitEntity(b, unit, unit.health);
                b.collided.add(unit.id);
                b.damage -= sub;
            }
            return (hit = ((pierceCap > 0 && b.collided.size >= pierceCap) || b.damage <= 0f));
        }, (x, y) -> {
            if(hit){
                Tmp.v1.trns(b.rotation(), Mathf.dst(b.lastX, b.lastY, x, y)).add(b.lastX, b.lastY);
                b.set(Tmp.v1);
                b.vel.setZero();
            }
            hit(b, x, y);
        }, true);
        if(b.data instanceof RailData data){
            data.lastLen += Mathf.dst(b.lastX, b.lastY, b.x, b.y);
            while(data.len < data.lastLen){
                Tmp.v1.trns(b.rotation(), data.len).add(data.x, data.y);
                trailEffect.at(Tmp.v1.x, Tmp.v1.y, b.rotation(), trailColor);
                data.len += trailSpacing;
            }
        }
    }

    @Override
    public void draw(Bullet b){
        drawTrail(b);
        float height = this.height * ((1f - shrinkY) + shrinkY * b.fout());
        float width = (this.width * ((1f - shrinkX) + shrinkX * b.fout())) / 1.5f;
        Tmp.v1.trns(b.rotation(), height / 2f);
        Draw.color(backColor);
        for(int s : Mathf.signs){
            Tmp.v2.trns(b.rotation() - 90f, width * s, -height);
            Draw.color(backColor);
            Fill.tri(Tmp.v1.x + b.x, Tmp.v1.y + b.y, -Tmp.v1.x + b.x, -Tmp.v1.y + b.y, Tmp.v2.x + b.x, Tmp.v2.y + b.y);
            Draw.color(frontColor);
            Fill.tri(Tmp.v1.x / 2f + b.x, Tmp.v1.y / 2f + b.y, -Tmp.v1.x / 2f + b.x, -Tmp.v1.y / 2f + b.y, Tmp.v2.x / 2f + b.x, Tmp.v2.y / 2f + b.y);
        }
    }

    static class RailData{
        float x, y, len, lastLen;
    }
}
