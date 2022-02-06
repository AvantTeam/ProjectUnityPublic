package unity.content.fx;

import arc.graphics.*;
import arc.graphics.g2d.*;
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
    }).layer(Layer.debris);
}
