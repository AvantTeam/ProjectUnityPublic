package unity.content.effects;

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

    empHit = new Effect(50f, 100f, e -> {
        float rad = 70f;
        e.scaled(7f, b -> {
            color(Pal.heal, b.fout());
            Fill.circle(e.x, e.y, rad);
        });

        color(Pal.heal);
        stroke(e.fout() * 3f);
        Lines.circle(e.x, e.y, rad);

        int points = 10;
        float offset = Mathf.randomSeed(e.id, 360f);
        for(int i = 0; i < points; i++){
            float angle = i* 360f / points + offset;
            Drawf.tri(e.x + Angles.trnsx(angle, rad), e.y + Angles.trnsy(angle, rad), 6f, 50f * e.fout(), angle);
        }

        Fill.circle(e.x, e.y, 12f * e.fout());
        color();
        Fill.circle(e.x, e.y, 6f * e.fout());
        Drawf.light(e.x, e.y, rad * 1.6f, Pal.heal, e.fout());
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

    hitExplosionLarge = new Effect(30f, 200f, e -> {
        color(Pal.missileYellow);
        e.scaled(12f, s -> {
            stroke(s.fout() * 2f + 0.5f);
            Lines.circle(e.x, e.y, s.fin() * 60f);
        });

        color(Color.gray);
        randLenVectors(e.id, 8, 2f + 42f * e.finpow(), (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 5f + 0.5f);
        });

        color(Pal.missileYellowBack);
        stroke(e.fout() * 1.5f);

        randLenVectors(e.id + 1, 5, 1f + 56f * e.finpow(), (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + e.fout() * 5f);
        });

        Drawf.light(e.x, e.y, 60f, Pal.missileYellowBack, 0.8f * e.fout());
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
    }),

    branchFragHit = new Effect(8f, e -> {
        color(Color.white, Pal.lancerLaser, e.fin());

        stroke(0.5f + e.fout());
        Lines.circle(e.x, e.y, e.fin() * 5f);

        stroke(e.fout());
        Lines.circle(e.x, e.y, e.fin() * 6f);
    });

    private HitFx(){
        throw new AssertionError();
    }
}
