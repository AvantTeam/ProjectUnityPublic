package unity.content.effects;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
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
    }).layer(Layer.blockOver-1);
}
