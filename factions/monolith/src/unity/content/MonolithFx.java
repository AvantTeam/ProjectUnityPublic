package unity.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.entities.*;
import unity.entities.type.*;
import unity.gen.entities.*;
import unity.graphics.*;
import unity.graphics.MultiTrail.*;
import unity.mod.*;
import unity.util.*;

import static arc.Core.atlas;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.randLenVectors;
import static arc.math.Interp.*;
import static mindustry.Vars.state;
import static unity.graphics.MonolithPal.*;

/**
 * Defines all {@linkplain Faction#monolith monolith} effect types.
 * @author GlennFolker
 */
public final class MonolithFx{
    private static final Color col = new Color();
    private static final Rand rand = new Rand();

    private static final Vec2 v1 = new Vec2(), v2 = new Vec2(), v3 = new Vec2();

    public static final Effect
    trailFadeLow = CoreFx.trailFadeLow,

    eneraphyteVapor = new Effect(140f, e -> {
        color(monolithLight, monolithLighter, monolithDarker, e.finpow());
        alpha(Interp.smoother.apply(Mathf.curve(e.fin(), 0f, 0.17f)) - Interp.pow3Out.apply(Mathf.curve(e.fin(), 0.17f)));

        randLenVectors(e.id, 2, 2f + e.finpow() * 10f, (x, y) ->
            Fill.circle(e.x + x, e.y + y, 0.6f + e.finpow() * 4f)
        );
    }),

    spark = new Effect(60f, e -> randLenVectors(e.id, 2, e.rotation, (x, y) -> {
        color(monolithLight, monolithMid, e.fin());

        float w = 1f + e.fout() * 4f;
        Fill.rect(e.x + x, e.y + y, w, w, 45f);
    })),

    soulMed = new Effect(27f, e -> {
        if(!(e.data instanceof Position data)) return;

        blend(Blending.additive);
        color(monolithLight, monolithMid, Color.black, e.finpow());

        float
        time = Time.time - e.rotation, vx = data.getX() * time, vy = data.getY() * time,
        fin = 1f - e.fin(pow2In);

        randLenVectors(e.id, 1, 3f + e.finpowdown() * 5f, (x, y) -> {
            alpha(1f);
            Fill.circle(e.x + x + vx, e.y + y + vy, fin * 1.4f);

            alpha(0.67f);
            Draw.rect("circle-shadow", e.x + x + vx, e.y + y + vy, fin * 8f, fin * 5.2f);
        });

        blend();
    }).layer(Layer.flyingUnitLow - 0.01f),

    soulLarge = new Effect(48f, e -> {
        if(!(e.data instanceof Position data)) return;

        blend(Blending.additive);
        color(monolithLight, monolithMid, Color.black, e.finpow());

        float
        time = Time.time - e.rotation, vx = data.getX() * time, vy = data.getY() * time,
        fin = 1f - e.fin(pow2In);

        randLenVectors(e.id, 1, 5f + e.finpowdown() * 8f, (x, y) -> {
            alpha(1f);
            Fill.circle(e.x + x + vx, e.y + y + vy, fin * 2f);

            alpha(0.67f);
            Draw.rect("circle-shadow", e.x + x + vx, e.y + y + vy, fin * 8f, fin * 8f);
        });

        blend();
    }).layer(Layer.flyingUnitLow - 0.01f),

    soulLargeAbsorb = new Effect(32f, e -> {
        if(!(e.data instanceof Position data)) return;

        v1
            .trns(Angles.angle(e.x, e.y, data.getX(), data.getY()) - 90f, Mathf.randomSeedRange(e.id, 3f))
            .scl(pow3Out.apply(e.fslope()));
        v2
            .trns(Mathf.randomSeed(e.id + 1, 360f), e.fin(pow4Out));
        v3
            .set(data).sub(e.x, e.y).scl(e.fin(pow4In))
            .add(v2).add(v1).add(e.x, e.y);

        float fin = 0.3f + e.fin() * 1.4f;

        blend(Blending.additive);
        color(Color.black, monolithMid, e.fin());

        alpha(1f);
        Fill.circle(v3.x, v3.y, fin);

        alpha(0.67f);
        Draw.rect("circle-shadow", v3.x, v3.y, fin + 6f, fin + 6f);

        blend();
    }).layer(Layer.flyingUnitLow),

    soulLargeTransfer = new Effect(64f, e -> {
        if(!(e.data instanceof Position data)) return;

        v1.set(data).sub(e.x, e.y).scl(e.fin(pow2In)).add(e.x, e.y);

        color(monolithMid, monolithLight, e.fslope());
        randLenVectors(e.id, 5, pow3Out.apply(e.fslope()) * 8f, 360f, 0f, 8f, (x, y) ->
            Fill.circle(v1.x + x, v1.y + y, 0.5f + e.fslope() * 2.7f)
        );

        float size = e.fin(pow10Out) * e.foutpowdown();

        color(monolithLight);
        Fill.circle(v1.x, v1.y, size * 4.8f);

        color(monolithLighter);
        for(int i = 0; i < 4; i++){
            Drawf.tri(v1.x, v1.y, size * 6.4f, size * 27f, e.rotation + 90f * i + e.finpow() * 45f * Mathf.sign(e.id % 2 == 0));
        }
    }),

    soulLargeDeath = new Effect(64f, e -> {
        color(monolithLight, monolithMid, e.fin());
        randLenVectors(e.id, 27, e.finpow() * 56f, (x, y) ->
            Fill.circle(e.x + x, e.y + y, 0.5f + e.fout() * 2.5f)
        );

        e.scaled(48f, i -> {
            stroke(i.fout() * 2.5f, monolithLighter);
            Lines.circle(e.x, e.y, i.fin(pow10Out) * 32f);

            float thick = i.foutpowdown() * 4f;

            Fill.circle(e.x, e.y, thick / 2f);
            for(int t = 0; t < 4; t++){
                Drawf.tri(e.x, e.y, thick, thick * 14f,
                    Mathf.randomSeed(e.id + 1, 360f) + 90f * t + i.finpow() * 60f * Mathf.sign(e.id % 2 == 0)
                );
            }
        });
    }),

    soulLargeJoin = new Effect(72f, e -> {
        if(!(e.data instanceof MonolithSoul soul)) return;

        stroke(1.5f, monolithLight);

        TextureRegion reg = atlas.find("unity-monolith-chain");
        Quat rot = MathUtils.q1.set(Vec3.Z, e.rotation + 90f).mul(MathUtils.q2.set(Vec3.X, 75f));
        float
            t = e.foutpowdown(), rad = t * 25f, a = Mathf.curve(t, 0.25f),
            w = (Mathf.PI2 * rad) / (reg.width * scl * 0.5f), h = w * ((float)reg.height / reg.width);

        alpha(a);
        DrawUtils.panningCircle(reg,
            e.x, e.y, w, h,
            rad, 360f, Time.time * 6f * Mathf.sign(soul.id % 2 == 0) + soul.id * 30f,
            rot, Layer.flyingUnitLow - 0.01f, Layer.flyingUnit
        );

        color(Color.black, monolithMid, 0.67f);
        alpha(a);

        blend(Blending.additive);
        DrawUtils.panningCircle(atlas.find("unity-line-shade"),
            e.x, e.y, w + 6f, h + 6f,
            rad, 360f, 0f,
            rot, true, Layer.flyingUnitLow - 0.01f, Layer.flyingUnit
        );

        blend();
    }).layer(Layer.flyingUnit),

    strayShoot = new Effect(12f, e -> {
        color(monolithLighter, monolithLight, monolithMid, e.finpowdown());
        stroke(e.fout() * 1.2f + 0.5f);

        randLenVectors(e.id, 2, 22f * e.finpow(), e.rotation, 50f, (x, y) ->
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fin() * 5f + 2f)
        );
    }).followParent(false),

    tendenceShoot = new Effect(32f, e -> {
        TextureRegion reg = atlas.find("unity-monolith-chain");
        MathUtils.q1.set(Vec3.Z, e.rotation + 90f).mul(MathUtils.q2.set(Vec3.X, 75f));
        float
            t = e.finpow(), rad = 9f + t * 8f,
            w = (Mathf.PI2 * rad) / (reg.width * scl * 0.4f), h = w * ((float)reg.height / reg.width);

        color(monolithLight);
        alpha(e.foutpowdown());

        DrawUtils.panningCircle(reg,
            e.x, e.y, w, h,
            rad, 360f, e.fin(pow2Out) * 90f * Mathf.sign(e.id % 2 == 0) + e.id * 30f,
            MathUtils.q1, Layer.flyingUnitLow - 0.01f, Layer.flyingUnit
        );

        color(Color.black, monolithMid, 0.67f);
        alpha(e.foutpowdown());

        blend(Blending.additive);
        DrawUtils.panningCircle(atlas.find("unity-line-shade"),
            e.x, e.y, w + 6f, h + 6f,
            rad, 360f, 0f,
            MathUtils.q1, true, Layer.flyingUnitLow - 0.01f, Layer.flyingUnit
        );

        blend();
    }).layer(Layer.flyingUnit),

    tendenceCharge = new PUEffect(() -> {
        class State extends EffectState{
            @Override
            public void remove(){
                if(data instanceof TrailHold[] data) for(TrailHold trail : data) Fx.trailFade.at(x, y, trail.width, monolithLighter, trail.trail.copy());
                super.remove();
            }
        }
        return Pools.obtain(State.class, State::new);
    }, () -> {
        TrailHold[] trails = new TrailHold[12];
        for(int i = 0; i < trails.length; i++){
            v1.trns(Mathf.random(360f), Mathf.random(24f, 64f));
            MultiTrail trail = MonolithTrails.soul(26);
            if(trail.trails[trail.trails.length - 1].trail instanceof TexturedTrail tr) tr.trailChance = 0.1f;

            trails[i] = new TrailHold(trail, v1.x, v1.y, Mathf.random(1f, 2f));
        }

        return trails;
    }, 40f, e -> {
        if(!(e.data instanceof TrailHold[] data)) return;

        color(monolithLight, monolithLighter, e.fin());
        randLenVectors(e.id, 8, 8f + e.foutpow() * 32f, (x, y) ->
            Fill.circle(e.x + x, e.y + y, 0.5f + e.fin() * 2.5f)
        );

        color();
        for(TrailHold hold : data){
            v1.set(hold.x, hold.y);
            v2.trns(v1.angle() - 90f, Mathf.sin(hold.width * 2.6f, hold.width * 8f * pow2Out.apply(e.fslope())));
            v1.scl(e.foutpowdown()).add(v2).add(e.x, e.y);

            float w = hold.width * e.fin();
            if(!state.isPaused()) hold.trail.update(v1.x, v1.y, w);

            col.set(monolithLight).lerp(monolithLighter, e.finpowdown());
            hold.trail.drawCap(col, w);
            hold.trail.draw(col, w);
        }

        stroke(Mathf.curve(e.fin(), 0.5f) * 1.4f, monolithLighter);
        Lines.circle(e.x, e.y, e.fout() * 64f);
    }),

    tendenceHit = new Effect(52f, e -> {
        color(monolithLighter, monolithLight, monolithMid, e.fin());
        for(int sign : Mathf.signs){
            randLenVectors(e.id + sign, 3, e.fin(pow5Out) * 32f, e.rotation, 30f, 16f, (x, y) ->
                Fill.square(e.x + x, e.y + y, e.foutpowdown() * 2.5f, e.id * 30f + e.finpow() * 90f * sign)
            );
        }
    }),

    erodedEneraphyteSteam = new Effect(100f, e -> {
        color(monolithLight, monolithDark, monolithDarker, e.fin(smoother));
        alpha(e.fslope() * 0.5f);

        float len = 1f + e.finpow() * 4f;
        rand.setSeed(e.id);

        int amount = rand.random(1, 3);
        for(int i = 0; i < amount; i++){
            v1.trns(rand.random(360f), rand.random(len)).add(e.x, e.y);
            Fill.circle(v1.x, v1.y, rand.random(0.6f, 1.7f) + smooth.apply(e.fslope()) * 0.75f);
        }
    }),

    eneraphyteSteam = new Effect(120f, e -> {
        color(monolithLighter, monolithMid, monolithDark, e.fin(smoother));
        alpha(e.fslope() * 0.7f);

        float len = 1f + e.finpow() * 5f;
        rand.setSeed(e.id);

        int amount = rand.random(1, 3);
        for(int i = 0; i < amount; i++){
            v1.trns(rand.random(360f), rand.random(len)).add(e.x, e.y);
            Fill.circle(v1.x, v1.y, rand.random(0.8f, 2f) + smooth.apply(e.fslope()) * 0.9f);
        }
    });

    private MonolithFx(){
        throw new AssertionError();
    }
}
