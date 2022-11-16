package unity.content.effects;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.color;

public class OtherFx{
    public static final Effect
    dust = new Effect(70, e -> {
        color(e.color);
        Vec2 v = (Vec2)e.data;
        Fill.circle(e.x + e.finpow()*v.x, e.y + e.finpow()*v.y, (7f - e.fin() * 7f)*0.5f);
    }).layer(Layer.debris),

    smokePoof = new Effect(30, e -> {
        Draw.color(Color.white);
        Angles.randLenVectors((long)e.id, 6, 4.0F + 30.0F * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 3.0F);
            Fill.circle(e.x + x / 2.0F, e.y + y / 2.0F, e.fout());
        });
    }).layer(Layer.blockOver-1),

    steamSlow = new Effect(200, e -> {
        Draw.color(Color.white);
        Draw.alpha(Mathf.sqrt(e.fslope()));
        float ef = e.finpow()*10f;
        Angles.randLenVectors((long)e.id, 1, 4.0F + 10.0F * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x + ef, e.y + y+ ef, e.fout() * 8.0F);
            Fill.circle(e.x + x / 2.0F + ef, e.y + y / 2.0F + ef, e.fout() * 4.0F);
        });
        Draw.alpha(1);
    }).layer(Layer.blockOver-1),

    weldspark = new Effect(12, e->{
        if(e.fin()<0.5){
            Draw.color(Color.white, e.color, e.fin()*0.5f);
        }else{
            Draw.color(e.color, Tmp.c1.set(e.color).mul(0.5f), e.fin()*0.5f+0.5f);
        }
        Lines.stroke(e.fout() * 0.6f + 0.6f);

        Angles.randLenVectors(e.id, 3, 15 * e.finpow(), e.rotation, 3, (x, y) -> {
            Lines.lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fslope() * 5 + 0.5f);
        });
    });
}
