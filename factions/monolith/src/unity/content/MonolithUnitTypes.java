package unity.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.ammo.*;
import mindustry.world.*;
import unity.assets.list.*;
import unity.entities.prop.*;
import unity.entities.type.*;
import unity.entities.type.bullet.energy.*;
import unity.entities.type.bullet.laser.*;
import unity.entities.weapons.*;
import unity.gen.entities.*;
import unity.graphics.*;
import unity.graphics.MultiTrail.*;
import unity.graphics.trail.*;
import unity.mod.*;
import unity.util.*;

import static arc.Core.atlas;
import static mindustry.Vars.content;
import static mindustry.Vars.headless;
import static unity.gen.entities.EntityRegistry.content;
import static unity.graphics.MonolithPal.*;
import static unity.mod.FactionRegistry.register;

/**
 * Defines all {@linkplain Faction#monolith monolith} unit types.
 * @author GlennFolker
 */
public final class MonolithUnitTypes{
    public static final MonolithSoulType[] souls = new MonolithSoulType[MonolithSoul.constructors.length];

    public static PUUnitType
    stele, pedestal, pilaster, pylon, monument, colossus, bastion,
    stray, tendence, liminality, calenture, hallucination, escapism, fantasy;

    private MonolithUnitTypes(){
        throw new AssertionError();
    }

    public static void load(){
        souls[0] = register(Faction.monolith, content("monolith-soul-0", MonolithSoul.class, n -> new MonolithSoulType(n, new MonolithSoulProps(){{
            transferAmount = 1;
            formAmount = 1;
            formDelta = 0.2f;
        }}){
            {
                health = 80f;
                range = maxRange = 48f;

                hitSize = 7.2f;
                speed = 3.6f;
                rotateSpeed = 7.5f;
                drag = 0.04f;
                accel = 0.18f;

                formTileChance = 0.17f;
                formAbsorbChance = 0f;

                engineColor = trailColor = monolithLighter;
                engineSize = 2f;
                engineOffset = 3.5f;

                trail(18, unit -> new MultiTrail(
                BaseTrail.rot(unit),
                new TrailHold(MonolithTrails.phantasmal(BaseTrail.rot(unit), 24, 0), 0f, engineOffset, monolithLighter)
                ));

                corporealTrail = soul -> new MultiTrail(
                new TrailHold(MonolithTrails.phantasmal(BaseTrail.rot(soul), 18, 0), monolithLighter),
                new TrailHold(MonolithTrails.singleSoul(6), monolithLight)
                );
            }

            @Override
            public void drawBase(MonolithSoul soul){
                draw(soul.x, soul.y, soul.rotation, 4f, 0.5f, 4.5f, 1.8f, -4.5f);

                Draw.color(monolithLight);
                for(int i = 0; i < 3; i++){
                    Tmp.v1.trns(
                    Time.time * 4.6f * MathUtils.randomSeedSign(soul.id) + 360f * i / 3f,
                    6f + Mathf.sin(Time.time + Mathf.randomSeed(soul.id, Mathf.PI2 * 8f) + (Mathf.PI2 * 8f) * ((float)i / 3), 8f, 1f)
                    ).add(soul);

                    Fill.circle(Tmp.v1.x, Tmp.v1.y, 1f);
                }

                Draw.reset();
            }

            @Override
            public void drawEyes(MonolithSoul soul){
                Tmp.v1.trns(soul.rotation, 1.8f).add(soul);

                Draw.color(0f, 0f, 0f, 0.14f);
                Draw.rect(softShadowRegion, Tmp.v1.x, Tmp.v1.y, 8f, 8f);

                Draw.color(monolithLighter, Color.white, 0.5f);
                Fill.circle(Tmp.v1.x, Tmp.v1.y, 1f);

                Draw.reset();
            }

            @Override
            public void drawForm(MonolithSoul soul){
                Draw.z(Layer.effect - 0.01f);
                Tmp.v1.trns(soul.rotation, 1.8f).add(soul);

                Lines.stroke(4f + Mathf.absin(12f, 0.1f));
                for(Tile tile : soul.forms){
                    long seed = soul.id + tile.pos();
                    Tmp.v2.trns(Mathf.randomSeed(seed, 360f) + Time.time * MathUtils.randomSeedSign(seed), 3f).add(tile);

                    DrawUtils.lineFalloff(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, Tmp.c1.set(monolithMid).a(0.4f), monolithLighter, 4, 0.67f);
                }

                Draw.reset();
            }

            @Override
            public void drawJoin(MonolithSoul soul){
                float z = Draw.z();
                if(soul.joinTarget != null){
                    Draw.z(Layer.effect - 0.01f);
                    Tmp.v1.trns(soul.rotation, 1.8f).add(soul);

                    Lines.stroke(2f + Mathf.absin(12f, 0.1f));
                    DrawUtils.lineFalloff(Tmp.v1.x, Tmp.v1.y, soul.joinTarget.getX(), soul.joinTarget.getY(), Tmp.c1.set(monolithMid).a(0.4f), monolithLighter, 4, 0.67f);
                    Draw.reset();
                }

                Draw.z(z);
                Lines.stroke(1.5f, monolithLight);

                TextureRegion reg = atlas.white();
                Quat rot = MathUtils.q1.set(Vec3.Z, soul.ringRotation() + 90f).mul(MathUtils.q2.set(Vec3.X, 75f));
                float
                t = Interp.pow5Out.apply(soul.joinTime()),
                rad = t * 7.2f, a = Mathf.curve(t, 0.25f),
                s = Lines.getStroke();

                Draw.alpha(a);
                DrawUtils.panningCircle(reg,
                soul.x, soul.y, s, s,
                rad, 360f, Time.time * 4f * MathUtils.randomSeedSign(soul.id) + soul.id * 30f,
                rot, true, Layer.flyingUnitLow - 0.01f, Layer.flyingUnit
                );

                Draw.color(Color.black, monolithMid, 0.67f);
                Draw.alpha(a);

                Draw.blend(Blending.additive);
                DrawUtils.panningCircle(atlas.find("unity-line-shade"),
                soul.x, soul.y, s + 6f, s + 6f,
                rad, 360f, 0f,
                rot, true, Layer.flyingUnitLow - 0.01f, Layer.flyingUnit
                );

                Draw.blend();
            }
        }));

        souls[1] = register(Faction.monolith, content("monolith-soul-1", MonolithSoul.class, n -> new MonolithSoulType(n, new MonolithSoulProps(){{
            transferAmount = 2;
            formAmount = 3;
            formDelta = 0.2f;
        }}){
            {
                health = 180f;
                range = maxRange = 72f;

                hitSize = 9.6f;
                speed = 3.2f;
                rotateSpeed = 7.8f;
                drag = 0.04f;
                accel = 0.18f;

                trailChance = 0.8f;
                trailEffect = MonolithFx.soulMed;
                //formTileChance = 0.17f;
                //formAbsorbChance = 0f;

                engineColor = trailColor = monolithLighter;
                engineSize = 2f;
                engineOffset = 3.5f;

                trail(20, unit -> new MultiTrail(
                BaseTrail.rot(unit),
                new TrailHold(MonolithTrails.phantasmal(BaseTrail.rot(unit), 32, 3, 3.2f, 4.8f, speed, 0f), 0f, engineOffset, monolithLighter)
                ));

                corporealTrail = soul -> new MultiTrail(
                new TrailHold(MonolithTrails.phantasmal(BaseTrail.rot(soul), 20, 1, 4.8f, 3.2f, speed, 2f), monolithLighter),
                new TrailHold(MonolithTrails.singleSoul(6), monolithLight)
                );
            }

            @Override
            public void drawBase(MonolithSoul soul){
                draw(soul.x, soul.y, soul.rotation, 5.2f, 1f, 6f, 1f, -7f);

                for(int i = 0; i < 6; i++){
                    float rotation = Time.time * 3.8f * MathUtils.randomSeedSign(soul.id) + 360f * i / 6f;
                    Tmp.v1.trns(
                    rotation - 90f,
                    7f + Mathf.sin(Time.time + Mathf.randomSeed(soul.id, Mathf.PI2 * 6f) + (Mathf.PI2 * 6f) * ((float)i / 6), 6f, 1.2f),
                    0.5f
                    ).add(soul);

                    if(i % 2 == 0){
                        Draw.color(monolithMid, monolithLight, 0.5f);
                        Fill.circle(Tmp.v1.x, Tmp.v1.y, 1.4f);
                    }else{
                        Draw.color(monolithLight);
                        Draw.rect(atlas.find("hcircle"), Tmp.v1.x, Tmp.v1.y, 2f, 2f, rotation + 90f);
                        Drawf.tri(Tmp.v1.x, Tmp.v1.y, 2f, 5f, rotation - 90f);
                    }
                }

                Draw.reset();
            }

            @Override
            public void drawEyes(MonolithSoul soul){
                float s = 2f;
                Tmp.v1.trns(soul.rotation, 2.5f).add(soul);
                Tmp.v2.trns(soul.rotation, 2.5f).add(Tmp.v1);

                Lines.stroke(s + 6f, Tmp.c1.set(0f, 0f, 0f, 0.14f));
                DrawUtils.line(
                atlas.find("circle-mid"), atlas.find("unity-circle-end"),
                Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y
                );

                Lines.stroke(s, Tmp.c1.set(monolithLighter).lerp(Color.white, 0.5f));
                DrawUtils.line(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
                Draw.reset();
            }
        }));

        souls[2] = register(Faction.monolith, content("monolith-soul-2", MonolithSoul.class, n -> new MonolithSoulType(n, new MonolithSoulProps(){{
            transferAmount = 4;
            formAmount = 5;
            formDelta = 0.2f;

            joinEffect = MonolithFx.soulLargeJoin;
            transferEffect = MonolithFx.soulLargeTransfer;
        }}){
            {
                health = 300f;
                range = maxRange = 96f;

                hitSize = 12f;
                speed = 2.8f;
                rotateSpeed = 8.2f;
                drag = 0.08f;
                accel = 0.2f;

                trailChance = 0.9f;
                formTileChance = 0.17f;
                formAbsorbChance = 0.67f;
                joinChance = 0.33f;
                trailEffect = MonolithFx.soulLarge;
                formTileEffect = MonolithFx.spark;
                formAbsorbEffect = MonolithFx.soulLargeAbsorb;
                joinEffect = MonolithFx.soulLargeAbsorb;

                deathExplosionEffect = MonolithFx.soulLargeDeath;
                //deathSound = PUSounds.soulDeath;

                engineColor = trailColor = monolithLighter;
                trail(24, unit -> new MultiTrail(
                BaseTrail.rot(unit),
                new TrailHold(MonolithTrails.phantasmal(BaseTrail.rot(unit), 30, 2, 5.6f, 8.4f, speed, 4f, null), monolithLighter),
                new TrailHold(MonolithTrails.soul(BaseTrail.rot(unit), 48, speed), 4.8f, 6f, 0.56f, monolithLighter),
                new TrailHold(MonolithTrails.soul(BaseTrail.rot(unit), 48, speed), -4.8f, 6f, 0.56f, monolithLighter)
                ));

                corporealTrail = soul -> MonolithTrails.soul(BaseTrail.rot(soul), trailLength, speed);
            }

            @Override
            public void drawBase(MonolithSoul soul){
                draw(soul.x, soul.y, soul.rotation, 7f, 1f, 8f, 1f, -7f);
                Lines.stroke(1f, monolithMid);

                float rotation = Time.time * 3f * MathUtils.randomSeedSign(soul.id);
                for(int i = 0; i < 5; i++){
                    float
                    r = rotation + 72f * i, sect = 60f,
                    rad = 10f + Mathf.sin(Time.time + Mathf.randomSeed(soul.id, Mathf.PI2 * 5f) + (Mathf.PI2 * 5f) * ((float)i / 5), 5f, 0.8f);

                    Lines.arc(soul.x, soul.y, rad, sect / 360f, r - sect / 2f);

                    Tmp.v1.trns(r, rad).add(soul);
                    Drawf.tri(Tmp.v1.x, Tmp.v1.y, 2.5f, 6f, r);
                }

                Draw.reset();
            }

            @Override
            public void drawEyes(MonolithSoul soul){
                float s = 2f, f = 2.2f, o = 2f;
                for(int sign : Mathf.signs){
                    Tmp.v1.trns(soul.rotation - 90f, f * sign, o).add(soul);
                    Tmp.v2.trns(soul.rotation, 3f).add(Tmp.v1);

                    Lines.stroke(s + 6f, Tmp.c1.set(0f, 0f, 0f, 0.14f));
                    DrawUtils.line(
                    atlas.find("circle-mid"), atlas.find("unity-circle-end"),
                    Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y
                    );

                    Lines.stroke(s, Tmp.c1.set(monolithLighter).lerp(Color.white, 0.5f));
                    DrawUtils.line(
                    atlas.white(), atlas.find("clear"), atlas.find("hcircle"),
                    Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y
                    );

                    Draw.color(Tmp.c1.set(monolithLighter).lerp(Color.white, 0.5f));
                    if(sign == -1){
                        Tmp.v3.trns(soul.rotation - 90f, -f - s / 2f, o).add(soul);
                        DrawUtils.fillSector(Tmp.v3.x, Tmp.v3.y, s, soul.rotation + 180f, 0.25f);
                    }else if(sign == 1){
                        Tmp.v3.trns(soul.rotation - 90f, f + s / 2f, o).add(soul);
                        DrawUtils.fillSector(Tmp.v3.x, Tmp.v3.y, s, soul.rotation + 90f, 0.25f);
                    }
                }

                Draw.reset();
            }

            @Override
            public void drawJoin(MonolithSoul soul){
                Lines.stroke(1.5f, monolithLight);

                TextureRegion reg = atlas.find("unity-monolith-chain");
                Quat rot = MathUtils.q1.set(Vec3.Z, soul.ringRotation() + 90f).mul(MathUtils.q2.set(Vec3.X, 75f));
                float
                t = Interp.pow3Out.apply(soul.joinTime()),
                rad = t * 25f, a = Mathf.curve(t, 0.33f),
                w = (Mathf.PI2 * rad) / (reg.width * Draw.scl * 0.5f), h = w * ((float)reg.height / reg.width);

                Draw.alpha(a);
                DrawUtils.panningCircle(reg,
                soul.x, soul.y, w, h,
                rad, 360f, Time.time * 4f * MathUtils.randomSeedSign(soul.id) + soul.id * 30f,
                rot, Layer.flyingUnitLow - 0.01f, Layer.flyingUnit
                );

                Draw.color(Color.black, monolithMid, 0.67f);
                Draw.alpha(a);

                Draw.blend(Blending.additive);
                DrawUtils.panningCircle(atlas.find("unity-line-shade"),
                soul.x, soul.y, w + 6f, h + 6f,
                rad, 360f, 0f,
                rot, true, Layer.flyingUnitLow - 0.01f, Layer.flyingUnit
                );

                Draw.blend();
            }
        }));

        souls[3] = register(Faction.monolith, content("monolith-soul-3", MonolithSoul.class, n -> new MonolithSoulType(n, new MonolithSoulProps(){{
            transferAmount = 8;
            formAmount = 7;
            formDelta = 0.2f;
        }}){{

        }}));

        {
            StringBuilder msg = null;
            for(int i = 0; i < souls.length; i++){
                MonolithSoulType type = souls[i];
                if(type == null){
                    if(msg == null){
                        msg = new StringBuilder("Missing soul type for index ").append(i);
                    }else{
                        msg.append(", ").append(i);
                    }
                }else{
                    MonolithSoul.constructors[i] = type::create;
                }
            }

            if(msg != null) throw new IllegalStateException(msg.append("!").toString());
        }

        stele = register(Faction.monolith, content("stele", MonolithMechUnit.class, n -> new PUUnitType(n){{

        }}));

        pedestal = register(Faction.monolith, content("pedestal", MonolithMechUnit.class, n -> new PUUnitType(n){{

        }}));

        pilaster = register(Faction.monolith, content("pilaster", MonolithMechUnit.class, n -> new PUUnitType(n){{

        }}));

        pylon = register(Faction.monolith, content("pylon", MonolithLegsUnit.class, n -> new PUUnitType(n){{

        }}));

        monument = register(Faction.monolith, content("monument", MonolithLegsUnit.class, n -> new PUUnitType(n){{

        }}));

        colossus = register(Faction.monolith, content("colossus", MonolithLegsUnit.class, n -> new PUUnitType(n){{

        }}));

        bastion = register(Faction.monolith, content("bastion", MonolithLegsUnit.class, n -> new PUUnitType(n){{

        }}));

        stray = register(Faction.monolith, content("stray", MonolithUnit.class, n -> new PUUnitType(n){{
            health = 300f;
            faceTarget = false;
            lowAltitude = true;
            flying = true;

            hitSize = 12f;
            speed = 5f;
            rotateSpeed = 8f;
            drag = 0.045f;
            accel = 0.08f;

            outlineColor = monolithOutline;
            ammoType = new PowerAmmoType(500f);

            engineColor = monolithLighter;
            engineSize = 2.5f;
            engineOffset = 12.5f;
            setEnginesMirror(new PUUnitEngine(4.5f, -10f, 1.8f, -90f));

            trail(unit -> {
                TexturedTrail right = MonolithTrails.singlePhantasmal(20, new VelAttrib(0.14f, 0f, (t, v) -> unit.rotation, 0.15f));
                right.trailChance = 0f;
                right.gradientInterp = Interp.pow3Out;
                right.fadeInterp = e -> (1f - Interp.pow2Out.apply(Mathf.curve(e, 0.84f, 1f))) * Interp.pow2In.apply(Mathf.curve(e, 0f, 0.56f)) * 0.6f;
                right.sideFadeInterp = e -> (1f - Interp.pow3Out.apply(Mathf.curve(e, 0.7f, 1f))) * Interp.pow3In.apply(Mathf.curve(e, 0f, 0.7f)) * 0.6f;

                TexturedTrail left = right.copy();
                left.attrib(VelAttrib.class).velX *= -1f;

                return new MultiTrail(
                BaseTrail.rot(unit),
                new TrailHold(MonolithTrails.phantasmal(BaseTrail.rot(unit), 16, 2, 3.6f, 6f, speed, 2f, null), monolithLighter),
                new TrailHold(right, 4.5f, 2.5f, 0.3f, monolithLighter),
                new TrailHold(left, -4.5f, 2.5f, 0.3f, monolithLighter)
                );
            });

            prop(new MonolithProps(){{
                maxSouls = 3;
                soulLackStatus = content.getByName(ContentType.status, "unity-disabled");
            }});

            weapons.add(new EnergyRingWeapon(){{
                rings.add(new Ring(){{
                    radius = 5.5f;
                    thickness = 1f;
                    spikes = 4;
                    spikeOffset = 1.5f;
                    spikeWidth = 2f;
                    spikeLength = 4f;
                    color = monolithLighter;
                }}, new Ring(){{
                    shootY = radius = 2.5f;
                    rotate = false;
                    thickness = 1f;
                    divisions = 2;
                    divisionSeparation = 30f;
                    angleOffset = 90f;
                    color = monolithLight;
                }});

                x = y = 0f;
                mirror = false;
                rotate = true;
                reload = 60f;
                inaccuracy = 30f;
                layerOffset = 10f;
                eyeRadius = 1.8f;

                shootSound = PUSounds.energyBolt;
                shoot = new ShootPattern(){{
                    shots = 6;
                    shotDelay = 1f;
                }};

                bullet = new BasicBulletType(1f, 6f, "shell"){
                    {
                        drag = -0.08f;
                        lifetime = 35f;
                        width = 8f;
                        height = 13f;

                        homingDelay = 6f;
                        homingPower = 0.09f;
                        homingRange = 160f;
                        weaveMag = 6f;
                        keepVelocity = false;

                        frontColor = trailColor = monolithLight;
                        backColor = monolithMid;
                        trailChance = 0.3f;
                        trailParam = 1.5f;
                        trailWidth = 2f;
                        trailLength = 12;

                        shootEffect = MonolithFx.strayShoot;
                        hitEffect = despawnEffect = Fx.hitLancer;
                    }

                    @Override
                    public void updateTrail(Bullet b){
                        if(!headless && b.trail == null){
                            TexturedTrail trail = MonolithTrails.singlePhantasmal(trailLength);
                            trail.forceCap = true;
                            trail.kickstart(b.x, b.y);

                            b.trail = trail;
                        }

                        super.updateTrail(b);
                    }

                    @Override
                    public void removed(Bullet b){
                        super.removed(b);
                        b.trail = null;
                    }
                };
            }});
        }}));

        tendence = register(Faction.monolith, content("tendence", MonolithUnit.class, n -> new PUUnitType(n){{
            health = 1200f;
            faceTarget = false;
            lowAltitude = true;
            flying = true;

            hitSize = 16f;
            speed = 4.2f;
            rotateSpeed = 7.2f;
            drag = 0.045f;
            accel = 0.08f;

            outlineColor = monolithOutline;
            ammoType = new PowerAmmoType(1000f);

            prop(new MonolithProps(){{
                maxSouls = 4;
                soulLackStatus = content.getByName(ContentType.status, "unity-disabled");
            }});

            engineOffset = 8f;
            engineSize = 2.5f;
            engineColor = monolithLight;
            setEnginesMirror(new PUUnitEngine(5f, -11.5f, 2.5f, -90f));

            trail(unit -> {
                VelAttrib vel = new VelAttrib(-0.18f, 0f, (t, v) -> unit.rotation);
                return new MultiTrail(
                BaseTrail.rot(unit),
                new TrailHold(MonolithTrails.soul(BaseTrail.rot(unit), 24, speed, vel), 5f, -3.5f, 1f, monolithLighter),
                new TrailHold(MonolithTrails.soul(BaseTrail.rot(unit), 24, speed, vel.flip()), -5f, -3.5f, 1f, monolithLighter)
                );
            });

            parts.add(new RegionPart("-top"){{
                outline = false;
                layer = Layer.bullet - 0.02f;
            }});

            weapons.add(new EnergyRingWeapon(){{
                rings.add(new Ring(){{
                    radius = 6.5f;
                    thickness = 1f;
                    spikes = 8;
                    spikeOffset = 1.5f;
                    spikeWidth = 2f;
                    spikeLength = 4f;
                    color = monolithLighter;
                }}, new Ring(){{
                    shootY = radius = 3f;
                    rotate = false;
                    thickness = 1f;
                    divisions = 2;
                    divisionSeparation = 30f;
                    angleOffset = 90f;
                    color = monolithLight;
                }});

                x = 0f;
                y = 1f;
                mirror = false;
                rotate = true;
                reload = 72f;
                inaccuracy = 15f;
                layerOffset = 10f;
                eyeRadius = 1.8f;
                parentizeEffects = true;

                chargeSound = PUSounds.energyCharge;
                shootSound = PUSounds.energyBlast;
                shoot = new ShootPattern(){{
                    firstShotDelay = 35f;
                }};

                bullet = new RevolvingRingsBulletType(4.8f, 72f, "shell"){
                    {
                        lifetime = 48f;
                        width = 16f;
                        height = 25f;
                        keepVelocity = false;
                        homingPower = 0.03f;
                        homingRange = range() * 2f;

                        lightning = 3;
                        lightningColor = monolithLighter;
                        lightningDamage = 12f;
                        lightningLength = 12;

                        frontColor = trailColor = monolithLight;
                        backColor = monolithMid;
                        trailEffect = MonolithFx.spark;
                        trailChance = 0.4f;
                        trailParam = 6f;
                        trailWidth = 5f;
                        trailLength = 32;

                        radius = new float[]{10f, 14f};
                        thickness = new float[]{2f, 2f};
                        colors = new Color[]{monolithLight, monolithMid};
                        glows = new Color[]{Color.black.cpy().lerp(monolithMid, 0.5f), Color.black.cpy().lerp(monolithMid, 0.25f)};

                        hitEffect = despawnEffect = MonolithFx.tendenceHit;
                        chargeEffect = MonolithFx.tendenceCharge;
                        shootEffect = MonolithFx.tendenceShoot;
                    }

                    @Override
                    public void updateTrail(Bullet b){
                        if(!headless && b.trail == null){
                            MultiTrail trail = MonolithTrails.soul(trailLength, 6f, trailWidth - 0.3f, speed);
                            trail.forceCap = true;
                            trail.kickstart(b.x, b.y);

                            b.trail = trail;
                        }

                        super.updateTrail(b);
                    }

                    @Override
                    public void removed(Bullet b){
                        super.removed(b);
                        b.trail = null;
                    }
                };
            }});
        }}));

        liminality = register(Faction.monolith, content("liminality", MonolithUnit.class, n -> new PUUnitType(n){{
            health = 2000f;
            faceTarget = false;
            lowAltitude = true;
            flying = true;

            strafePenalty = 0.1f;
            hitSize = 36f;
            speed = 3.5f;
            rotateSpeed = 3.6f;
            drag = 0.06f;
            accel = 0.08f;

            outlineColor = monolithOutline;
            ammoType = new PowerAmmoType(2000f);

            prop(new MonolithProps(){{
                maxSouls = 5;
                soulLackStatus = content.getByName(ContentType.status, "unity-disabled");
            }});

            engineOffset = 22.25f;
            engineSize = 4f;
            engineColor = monolithLighter;
            setEnginesMirror(
            new PUUnitEngine(17.875f, -16.25f, 3f, -90f),
            new PUUnitEngine(9f, -11f, 3.5f, -45f, monolithLight)
            );

            trail(unit -> {
                VelAttrib velInner = new VelAttrib(0.2f, 0f, (t, v) -> unit.rotation, 0.25f);
                VelAttrib velOuter = new VelAttrib(0.24f, 0.1f, (t, v) -> unit.rotation, 0.2f);
                return new MultiTrail(
                BaseTrail.rot(unit),
                new TrailHold(MonolithTrails.phantasmal(BaseTrail.rot(unit), 32, 2, 5.6f, 8f, speed, 0f, null), monolithLighter),
                new TrailHold(MonolithTrails.soul(BaseTrail.rot(unit), 48, 6f, 3.2f, speed, velInner), 9f, 11.25f, 0.75f, monolithLighter),
                new TrailHold(MonolithTrails.soul(BaseTrail.rot(unit), 48, 6f, 3.2f, speed, velInner.flip()), -9f, 11.25f, 0.75f, monolithLighter),
                new TrailHold(MonolithTrails.singlePhantasmal(10, velOuter), 17.875f, 6f, 0.6f, monolithLighter),
                new TrailHold(MonolithTrails.singlePhantasmal(10, velOuter.flip()), -17.875f, 6f, 0.6f, monolithLighter)
                );
            });

            parts.add(
            new RegionPart("-middle"){{
                outline = false;
                layer = Layer.bullet - 0.02f;
            }}, new RegionPart("-top"){{
                outline = false;
                layer = Layer.effect + 0.0199f;
            }}
            );

            weapons.add(new EnergyRingWeapon(){{
                rings.add(new Ring(){{
                    radius = 9f;
                    thickness = 1f;
                    spikes = 6;
                    spikeOffset = 1.5f;
                    spikeWidth = 2f;
                    spikeLength = 4f;
                    color = monolithLighter;
                }}, new Ring(){{
                    shootY = radius = 5.6f;
                    rotate = false;
                    thickness = 1f;
                    divisions = 2;
                    divisionSeparation = 30f;
                    angleOffset = 90f;
                    color = monolithLight;
                }}, new Ring(){{
                    radius = 2f;
                    thickness = 1f;
                    spikes = 4;
                    spikeOffset = 0.4f;
                    spikeWidth = 1.5f;
                    spikeLength = 1.5f;
                    flip = true;
                    color = monolithMid;
                }});

                x = 0f;
                y = 5f;
                mirror = false;
                rotate = true;
                reload = 56f;
                layerOffset = 10f;
                eyeRadius = 2f;

                shootSound = Sounds.laser;
                bullet = new HelixLaserBulletType(240f){{
                    sideWidth = 1.4f;
                    sideAngle = 30f;
                }};
            }});
        }}));

        calenture = register(Faction.monolith, content("calenture", MonolithUnit.class, n -> new PUUnitType(n){{
            health = 14400f;
            faceTarget = false;
            lowAltitude = true;
            flying = true;

            strafePenalty = 0.3f;
            hitSize = 48f;
            speed = 3.5f;
            rotateSpeed = 3.6f;
            drag = 0.06f;
            accel = 0.08f;

            outlineColor = monolithOutline;
            ammoType = new PowerAmmoType(3000f);

            prop(new MonolithProps(){{
                maxSouls = 7;
                soulLackStatus = content.getByName(ContentType.status, "unity-disabled");
            }});

            parts.add(
            new RegionPart(name + "-middle"){{
                outline = false;
                layer = Layer.bullet - 0.02f;
            }}, new RegionPart(name + "-top"){{
                outline = false;
                layer = Layer.effect + 0.0199f;
            }}
            );

            weapons.add(new EnergyRingWeapon(){{
                rings.add(new Ring(){{
                    color = monolithLighter;
                    radius = 14f;
                    rotateSpeed = 4f;
                    spikes = 8;
                    spikeOffset = 2f;
                    spikeWidth = 3f;
                    spikeLength = 4.5f;
                }}, new Ring(){{
                    color = monolithMid.cpy().lerp(monolithLight, 0.5f);
                    thickness = 1f;
                    radius = 12f;
                    rotateSpeed = 3.2f;
                    flip = true;
                    divisions = 2;
                    divisionSeparation = 30f;
                }}, new Ring(){{
                    color = monolithLight;
                    shootY = radius = 8.5f;
                    rotate = false;
                    angleOffset = 90f;
                    divisions = 2;
                    divisionSeparation = 30f;
                }}, new Ring(){{
                    color = monolithMid;
                    thickness = 1f;
                    radius = 4f;
                    rotateSpeed = 2.4f;
                    spikes = 6;
                    spikeOffset = 0.4f;
                    spikeWidth = 2f;
                    spikeLength = 2f;
                }});

                x = 0f;
                y = 10f;
                mirror = false;
                rotate = true;
                reload = 120f;
                layerOffset = 10f;
                eyeRadius = 2f;
            }});
        }}));

        hallucination = register(Faction.monolith, content("hallucination", MonolithUnit.class, n -> new PUUnitType(n){{

        }}));

        escapism = register(Faction.monolith, content("escapism", MonolithUnit.class, n -> new PUUnitType(n){{

        }}));

        fantasy = register(Faction.monolith, content("fantasy", MonolithUnit.class, n -> new PUUnitType(n){{

        }}));
    }
}
