package unity.content.effects;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import unity.util.*;

import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.stroke;
import static arc.math.Angles.*;

public final class ShootFx{
    public static final Effect

    laserChargeShoot = new Effect(21f, e -> {
        color(e.color, Color.white, e.fout());

        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 4f * e.fout(), 29f, e.rotation + 90f * i + e.finpow() * 112f);
        }
    }),

    laserChargeShootShort = new Effect(15f, e -> {
        color(e.color, Color.white, e.fout());
        stroke(2f * e.fout());
        Lines.square(e.x, e.y, 0.1f + 20f * e.finpow(), 45f);
    }),

    laserFractalShoot = new Effect(40f, e -> {
        color(Tmp.c1.set(e.color).lerp(Color.white, e.fout()));

        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 4f * e.fout(), 29f, e.rotation + 90f * i + e.finpow() * 112f);
        }

        for(int h = 1; h <= 5; h++){
            //float rot = h * 180f + Mathf.randomSeedRange(e.id, 360) + e.finpow() * 262;
            float mul = h % 2;
            float rm = 1 + mul * 0.5f;
            float rot = 90 + (1 - e.finpow()) * Mathf.randomSeed(e.id + (long)(mul * 2f), 210 * rm, 360 * rm);
            for(int i = 0; i < 2; i++){
                float m = i == 0 ? 1 : 0.5f;
                float w = 8 * e.fout() * m;
                float length = 8 * 3 / (2 - mul);
                Vec2 fxPos = Tmp.v1.trns(rot, length - 4);
                length *= Utils.pow25Out.apply(e.fout());

                Drawf.tri(fxPos.x + e.x, fxPos.y + e.y, w, length * m, rot + 180);
                Drawf.tri(fxPos.x + e.x, fxPos.y + e.y, w , length / 3f * m, rot);

                Draw.alpha(0.5f);
                Drawf.tri(e.x, e.y, w, length * m,  rot + 360);
                Drawf.tri(e.x, e.y, w, length/3 * m, rot);
                Fill.square(fxPos.x + e.x, fxPos.y + e.y, 3 * e.fout(), rot + 45);
            }
        }
    }),

    laserBreakthroughShoot = new Effect(40f, e -> {
        color(e.color);

        stroke(e.fout() * 2.5f);
        Lines.circle(e.x, e.y, e.finpow() * 100f);

        stroke(e.fout() * 5f);
        Lines.circle(e.x, e.y, e.fin() * 100f);

        color(e.color, Color.white, e.fout());

        randLenVectors(e.id, 20, 80f * e.finpow(), (x, y) -> Fill.circle(e.x + x, e.y + y, e.fout() * 5f));


        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 9f * e.fout(), 170f, e.rotation + Mathf.randomSeed(e.id, 360f) + 90f * i + e.finpow() * (0.5f - Mathf.randomSeed(e.id)) * 150f);
        }
    }),

    shootSmallBlaze = new Effect(22f, e -> {
        color(Pal.lightFlame, Pal.darkFlame, Pal.gray, e.fin());
        randLenVectors(e.id, 16, e.finpow() * 60f, e.rotation, 18f, (x, y) -> Fill.circle(e.x + x, e.y + y, 0.85f + e.fout() * 3.5f));
    }),

    shootPyraBlaze = new Effect(32f, e -> {
        color(Pal.lightPyraFlame, Pal.darkPyraFlame, Pal.gray, e.fin());
        randLenVectors(e.id, 16, e.finpow() * 60f, e.rotation, 18f, (x, y) -> Fill.circle(e.x + x, e.y + y, 0.85f + e.fout() * 3.5f));
    }),

    sapPlasmaShoot = new Effect(25f, e -> {
        color(Color.white, Pal.sapBullet, e.fin());
        randLenVectors(e.id, 13, e.finpow() * 20f, e.rotation, 23f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout() * 5f);
            Fill.circle(e.x + x / 1.2f, e.y + y / 1.2f, e.fout() * 3f);
        });
    }),
    tonkCannon = new Effect(35f, e -> {
        color(Pal.accent);
        for(int sus : Mathf.signs){
            Drawf.tri(e.x, e.y, 8 * e.fout(Interp.pow3Out), 80, e.rotation + 20 * sus);
            Drawf.tri(e.x, e.y, 4 * e.fout(Interp.pow3Out), 30, e.rotation + 60 * sus); 
        };
    }),
    tonkCannonSmoke = new Effect(45f, e -> {
        color(Pal.lighterOrange, Color.lightGray, Color.gray, e.fin());
        randLenVectors(e.id, 14, 0f + 55f * e.finpow(), e.rotation, 25f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fout(Interp.pow5Out) * 4.5f);
        });
    });

    private ShootFx(){
        throw new AssertionError();
    }
}
