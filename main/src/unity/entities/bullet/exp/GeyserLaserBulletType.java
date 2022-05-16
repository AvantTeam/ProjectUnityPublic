package unity.entities.bullet.exp;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class GeyserLaserBulletType extends ExpLaserBulletType{
    public float widthInc = 0.05f;
    public BulletType geyser;

    public GeyserLaserBulletType(float length, float damage){
        super(length, damage);
        width = 3f;
        hitMissed = true;
    }

    public Liquid getLiquid(Bullet b){
        return b.data instanceof Liquid l ? l : Liquids.water;
    }

    @Override
    public void init(Bullet b){
        Liquid l = getLiquid(b); //b.data is overwritten during elbt's init!
        super.init(b);
        Position dest = (Position) b.data;
        b.rotation(b.angleTo(dest));
        b.fdata = b.dst(dest);
        b.data = l;

        if(geyser != null) geyser.create(b.owner, b.team, dest.getX(), dest.getY(), b.rotation(), -1f, 1f, 1f, l);
    }

    @Override
    public void draw(Bullet b){
        Tmp.v1.trns(b.rotation(), b.fdata).add(b);

        float width = this.width + widthInc * getLevel(b);
        Liquid l = getLiquid(b);
        Draw.color(l.color, 1f);

        Draw.alpha(0.4f);
        Lines.stroke(b.fout() * width * strokes[0]);
        Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);
        Fill.circle(b.x, b.y, b.fout() * width * 0.9f * strokes[0]);

        Draw.alpha(1);
        Lines.stroke(b.fout() * width * strokes[1]);
        Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);
        Fill.circle(b.x, b.y, b.fout() * width * 0.9f * strokes[1]);

        Draw.color(l.color, Color.white, 0.6f);
        Lines.stroke(b.fout() * width * strokes[2]);
        Lines.line(b.x, b.y, Tmp.v1.x, Tmp.v1.y);
        Fill.circle(b.x, b.y, b.fout() * width * 0.9f * strokes[2]);
        Draw.reset();

        Drawf.light(b.x, b.y, Tmp.v1.x, Tmp.v1.y, width * 10 * b.fout(), l.lightColor, l.lightColor.a);
    }
}
