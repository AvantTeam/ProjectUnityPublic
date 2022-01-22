package unity.content.fx;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.*;
import mindustry.graphics.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;

public final class HitFx{
    public static final Effect

    coloredHitSmall = new Effect(14f, e -> {
        color(Color.white, e.color, e.fin());
        e.scaled(7f, s -> {
            stroke(0.5f + s.fout());
            circle(e.x, e.y, s.fin() * 5f);
        });

        stroke(0.5f + e.fout());
        randLenVectors(e.id, 5, e.fin() * 15f, (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 3f + 1f));
    }),

    coloredHitLarge = new Effect(21f, e -> {
        color(Color.white, e.color, e.fin());
        e.scaled(8f, s -> {
            stroke(0.5f + s.fout());
            circle(e.x, e.y, s.fin() * 11f);
        });

        stroke(0.5f + e.fout());
        randLenVectors(e.id, 6, e.fin() * 35f, e.rotation + 180f, 45f, (x, y) -> lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 7f + 1f));
    }),

    hitExplosionMassive = new Effect(70f, 370f, e -> {
        e.scaled(17f, s -> {
            color(Color.white, Color.lightGray, e.fin());
            stroke(s.fout() + 0.5f);
            circle(e.x, e.y, e.fin() * 185f);
        });

        color(Color.gray);

        randLenVectors(e.id, 12, 5f + 135f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 22f + 0.5f);
            Fill.circle(e.x + x / 2f, e.y + y / 2f, e.fout() * 9f);
        });

        color(Pal.lighterOrange, Pal.lightOrange, Color.gray, e.fin());
        stroke(1.5f * e.fout());

        randLenVectors(e.id + 1, 14, 1f + 160f * e.finpow(), (x, y) ->
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 3f));
    });

    private HitFx(){
        throw new AssertionError();
    }
}
