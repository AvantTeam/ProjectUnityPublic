package unity.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.pattern.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.ammo.*;
import mindustry.world.meta.*;
import unity.ai.*;
import unity.annotations.Annotations.*;
import unity.content.effects.*;
import unity.entities.*;
import unity.entities.bullet.energy.*;
import unity.entities.bullet.laser.*;
import unity.entities.bullet.physical.*;
import unity.gen.*;
import unity.graphics.*;
import unity.type.*;
import unity.type.weapons.*;

import static mindustry.Vars.*;

public final class UnityUnitTypes{
    // Global {unit, copter}.
    public static @EntityDef({Unitc.class, Copterc.class})
    UnitType caelifera, schistocerca, anthophila, vespula, lepidoptera;//, mantodea, meganisoptera;

    // Global T6-7 units:
    // - Reign.
    public static @EntityPoint(MechUnit.class)
    UnitType citadel, empire;
    // - Corvus + Toxopid.
    public static @EntityPoint(LegsUnit.class)
    UnitType cygnus, sagittarius, araneidae, theraphosidae;
    // - Eclipse.
    public static @EntityPoint(UnitEntity.class)
    UnitType mantle, aphelion;
    // - Oct.
    public static @EntityPoint(PayloadUnit.class)
    UnitType sedec, trigintaduo;
    // - Omura.
    public static @EntityPoint(UnitWaterMove.class)
    UnitType fin, blue;


    // Youngcha {unit, modular}.
    public static @EntityDef({Unitc.class, ModularUnitc.class})
    UnitType modularUnitSmall;

    private UnityUnitTypes(){
        throw new AssertionError();
    }

    public static void load(){
        caelifera = new UnityUnitType("caelifera"){{
            aiController = FlyingAI::new;
            circleTarget = false;

            speed = 5f;
            drag = 0.08f;
            accel = 0.04f;
            fallSpeed = 0.005f;
            health = 75;
            engineSize = 0f;
            flying = true;
            hitSize = 12f;
            range = 140f;

            weapons.add(new Weapon(name + "-gun"){{
                layerOffset = -0.01f;

                reload = 6f;
                x = 5.25f;
                y = 6.5f;
                shootY = 1.5f;
                shootSound = Sounds.pew;
                ejectEffect = Fx.casing1;

                bullet = new BasicBulletType(5f, 7f){{
                    lifetime = 30f;
                    shrinkY = 0.2f;
                }};
            }}, new Weapon(name + "-launcher"){{
                layerOffset = -0.01f;

                reload = 30f;
                x = 4.5f;
                y = 0.5f;
                shootY = 2.25f;
                shootSound = Sounds.shootSnap;
                ejectEffect = Fx.casing2;

                bullet = new MissileBulletType(3f, 1f){{
                    speed = 3f;
                    lifetime = 45f;
                    splashDamage = 40f;
                    splashDamageRadius = 8f;
                    drag = -0.01f;
                }};
            }});

            rotors.add(new Rotor(name + "-rotor"){{
                x = 0f;
                y = 6f;
            }});
        }};

        schistocerca = new UnityUnitType("schistocerca"){{
            aiController = FlyingAI::new;
            circleTarget = false;

            speed = 4.5f;
            drag = 0.07f;
            accel = 0.03f;
            fallSpeed = 0.005f;
            health = 150;
            engineSize = 0f;
            flying = true;
            hitSize = 13f;
            range = 165f;
            rotateSpeed = 4.6f;

            weapons.add(new Weapon(name + "-gun"){{
                layerOffset = -0.01f;

                top = false;
                x = 1.5f;
                y = 11f;
                shootX = -0.75f;
                shootY = 3f;
                shootSound = Sounds.pew;
                ejectEffect = Fx.casing1;
                reload = 8f;

                bullet = new BasicBulletType(4f, 5f){{
                    lifetime = 36;
                    shrinkY = 0.2f;
                }};
            }}, new Weapon(name + "-gun"){{
                top = false;
                x = 4f;
                y = 8.75f;
                shootX = -0.75f;
                shootY = 3f;
                shootSound = Sounds.shootSnap;
                ejectEffect = Fx.casing1;
                reload = 12f;

                bullet = new BasicBulletType(4f, 8f){{
                    width = 7f;
                    height = 9f;
                    lifetime = 36f;
                    shrinkY = 0.2f;
                }};
            }}, new Weapon(name + "-gun-big"){{
                top = false;
                x = 6.75f;
                y = 5.75f;
                shootX = -0.5f;
                shootY = 2f;
                shootSound = Sounds.shootSnap;
                ejectEffect = Fx.casing1;
                reload = 30f;

                bullet = new BasicBulletType(3.2f, 16, "bullet"){{
                    width = 10f;
                    height = 12f;
                    frontColor = Pal.lightishOrange;
                    backColor = Pal.lightOrange;
                    status = StatusEffects.burning;
                    hitEffect = new MultiEffect(Fx.hitBulletSmall, Fx.fireHit);

                    ammoMultiplier = 5;

                    splashDamage = 10f;
                    splashDamageRadius = 22f;

                    makeFire = true;
                    lifetime = 60f;
                }};
            }});

            for(int i : Mathf.signs){
                rotors.add(new Rotor(name + "-rotor"){{
                    x = 0f;
                    y = 6.5f;
                    bladeCount = 3;
                    ghostAlpha = 0.4f;
                    shadowAlpha = 0.2f;
                    shadeSpeed = 3f * i;
                    speed = 29f * i;
                }});
            }
        }};

        anthophila = new UnityUnitType("anthophila"){{
            aiController = FlyingAI::new;
            circleTarget = false;

            speed = 4f;
            drag = 0.07f;
            accel = 0.03f;
            fallSpeed = 0.005f;
            health = 450;
            engineSize = 0f;
            flying = true;
            hitSize = 15f;
            range = 165f;
            fallRotateSpeed = 2f;
            rotateSpeed = 3.8f;

            weapons.add(new Weapon(name + "-gun"){{
                layerOffset = -0.01f;

                x = 4.25f;
                y = 14f;
                shootX = -1f;
                shootY = 2.75f;
                reload = 15;
                shootSound = Sounds.shootBig;

                bullet = new BasicBulletType(6f, 60f){{
                    lifetime = 30f;
                    width = 16f;
                    height = 20f;
                    shootEffect = Fx.shootBig;
                    smokeEffect = Fx.shootBigSmoke;
                }};
            }}, new Weapon(name + "-tesla"){{
                x = 7.75f;
                y = 8.25f;
                shootY = 5.25f;
                reload = 30f;
                shoot.shots = 3;
                shootSound = Sounds.spark;

                bullet = new LightningBulletType(){{
                    damage = 15f;
                    lightningLength = 12;
                    lightningColor = Pal.surge;
                }};
            }});

            for(int i : Mathf.signs){
                rotors.add(new Rotor(name + "-rotor2"){{
                    x = 0f;
                    y = -13f;
                    bladeCount = 2;
                    ghostAlpha = 0.4f;
                    shadowAlpha = 0.2f;
                    shadeSpeed = 3f * i;
                    speed = 29f * i;
                }});
            }

            rotors.add(new Rotor(name + "-rotor1"){{
                mirror = true;
                x = 13f;
                y = 3f;
                bladeCount = 3;
            }});
        }};

        vespula = new UnityUnitType("vespula"){{
            aiController = FlyingAI::new;
            circleTarget = false;

            speed = 3.5f;
            drag = 0.07f;
            accel = 0.03f;
            fallSpeed = 0.003f;
            health = 4000;
            engineSize = 0f;
            flying = true;
            hitSize = 30f;
            range = 165f;
            lowAltitude = true;
            rotateSpeed = 3.5f;

            weapons.add(new Weapon(name + "-gun-big"){{
                layerOffset = -0.01f;

                x = 8.25f;
                y = 9.5f;
                shootX = -1f;
                shootY = 7.25f;
                reload = 12f;
                shootSound = Sounds.shootBig;

                bullet = new BasicBulletType(6f, 60f){{
                    lifetime = 30f;
                    width = 16f;
                    height = 20f;
                    shootEffect = Fx.shootBig;
                    smokeEffect = Fx.shootBigSmoke;
                }};
            }}, new Weapon(name + "-gun"){{
                layerOffset = -0.01f;

                x = 6.5f;
                y = 21.5f;
                shootX = -0.25f;
                shootY = 5.75f;
                reload = 20f;
                shoot.shots = 4;
                shoot.shotDelay = 2f;
                shootSound = Sounds.shootSnap;

                bullet = new BasicBulletType(4f, 29, "bullet"){{
                    width = 10f;
                    height = 13f;
                    shootEffect = Fx.shootBig;
                    smokeEffect = Fx.shootBigSmoke;
                    ammoMultiplier = 4;
                    lifetime = 60f;
                }};
            }}, new Weapon(name + "-laser-gun"){{
                x = 13.5f;
                y = 15.5f;
                shootY = 4.5f;
                reload = 60f;
                shootSound = Sounds.laser;

                bullet = new LaserBulletType(240f){{
                    sideAngle = 45f;
                    length = 200f;
                }};
            }});

            for(int i : Mathf.signs){
                rotors.add(new Rotor(name + "-rotor"){{
                    mirror = true;
                    x = 15f;
                    y = 6.75f;
                    speed = 29f * i;
                    ghostAlpha = 0.4f;
                    shadowAlpha = 0.2f;
                    shadeSpeed = 3f * i;
                }});
            }
        }};

        lepidoptera = new UnityUnitType("lepidoptera"){{
            aiController = FlyingAI::new;
            circleTarget = false;

            speed = 3f;
            drag = 0.07f;
            accel = 0.03f;
            fallSpeed = 0.003f;
            health = 9500;
            engineSize = 0f;
            flying = true;
            hitSize = 45f;
            range = 300f;
            lowAltitude = true;
            fallRotateSpeed = 0.8f;
            rotateSpeed = 2.7f;

            weapons.add(new Weapon(name + "-gun"){{
                layerOffset = -0.01f;

                x = 14f;
                y = 27f;
                shootY = 5.5f;
                shootSound = Sounds.shootBig;
                ejectEffect = Fx.casing3Double;
                reload = 10f;

                bullet = new BasicBulletType(7f, 80f){{
                    lifetime = 30f;
                    width = 18f;
                    height = 22f;
                    shootEffect = Fx.shootBig;
                    smokeEffect = Fx.shootBigSmoke;
                }};
            }}, new Weapon(name + "-launcher"){{
                x = 17f;
                y = 14f;
                shootY = 5.75f;
                shootSound = Sounds.shootSnap;
                ejectEffect = Fx.casing2;

                shoot = new ShootSpread(2, 2f);
                reload = 20f;

                bullet = new MissileBulletType(6f, 1f){{
                    width = 8f;
                    height = 14f;
                    trailColor = Pal.missileYellowBack;
                    weaveScale = 2f;
                    weaveMag = 2f;
                    lifetime = 35f;
                    drag = -0.01f;
                    splashDamage = 48f;
                    splashDamageRadius = 12f;
                    frontColor = Pal.missileYellow;
                    backColor = Pal.missileYellowBack;
                }};
            }}, new Weapon(name + "-gun-big"){{
                rotate = true;
                rotateSpeed = 3f;
                x = 8f;
                y = 3f;
                shootY = 6.75f;
                shootSound = Sounds.shotgun;
                ejectEffect = Fx.none;

                shoot = new ShootSpread(3, 15f);
                reload = 45f;

                bullet = new ShrapnelBulletType(){{
                    toColor = Pal.accent;
                    damage = 150f;
                    keepVelocity = false;
                    length = 150f;
                }};
            }});

            for(int i : Mathf.signs){
                rotors.add(new Rotor(name + "-rotor1"){{
                    mirror = true;
                    x = 22.5f;
                    y = 21.25f;
                    bladeCount = 3;
                    speed = 19f * i;
                    ghostAlpha = 0.4f;
                    shadowAlpha = 0.2f;
                    shadeSpeed = 3f * i;
                }}, new Rotor(name + "-rotor2"){{
                    mirror = true;
                    x = 17.25f;
                    y = 1f;
                    bladeCount = 2;
                    speed = 23f * i;
                    ghostAlpha = 0.4f;
                    shadowAlpha = 0.2f;
                    shadeSpeed = 4f * i;
                }});
            }
        }};

        citadel = new UnityUnitType("citadel"){{
            speed = 0.3f;
            hitSize = 49f;
            rotateSpeed = 1.5f;
            health = 48750f;
            armor = 15f;
            mechStepParticles = true;
            stepShake = 0.8f;
            canDrown = false;
            mechFrontSway = 2f;
            mechSideSway = 0.7f;
            mechStride = (4f + (hitSize - 8f) / 2.1f) / 1.25f;
            immunities.add(StatusEffects.burning);

            weapons.add(new Weapon(name + "-weapon"){{
                top = false;
                x = 31.5f;
                y = -6.25f;
                shootY = 30.25f;
                reload = 90f;
                recoil = 7f;
                shake = 3f;
                ejectEffect = Fx.casing4;
                shootSound = Sounds.railgun;

                bullet = new SlowRailBulletType(25f, 250f){{
                    lifetime = 13f;
                    trailSpacing = 25f;
                    splashDamage = 95f;
                    splashDamageRadius = 50f;
                    hitEffect = Fx.hitBulletBig;
                    shootEffect = Fx.instShoot;
                    trailEffect = TrailFx.coloredRailgunSmallTrail;
                    width = 9f;
                    height = 17f;
                    shrinkY = 0f;
                    shrinkX = 0f;
                    pierceCap = 7;
                    backColor = hitColor = trailColor = Pal.bulletYellowBack;
                    frontColor = Color.white;
                }};
            }}, new LimitedAngleWeapon(name + "-flamethrower"){{
                x = 17.75f;
                y = 11.25f;
                shootY = 5.5f;
                reload = 5f;
                recoil = 0.5f;
                shootSound = Sounds.flame;
                angleCone = 80f;
                rotate = true;

                bullet = UnityBullets.citadelFlame;
            }}, new LimitedAngleWeapon(name + "-flamethrower"){{
                x = 14f;
                y = -9f;
                shootY = 5.5f;
                reload = 4f;
                recoil = 0.5f;
                shootSound = Sounds.flame;
                angleCone = 80f;
                rotate = true;

                bullet = UnityBullets.citadelFlame;
            }});
        }};

        empire = new UnityUnitType("empire"){{
            speed = 0.2f;
            hitSize = 49f;
            rotateSpeed = 1.25f;
            health = 140000f;
            armor = 20f;
            mechStepParticles = true;
            stepShake = 0.83f;
            canDrown = false;
            mechFrontSway = 4f;
            mechSideSway = 0.7f;
            mechStride = (4f + (hitSize - 8f) / 2.1f) / 1.3f;
            immunities.addAll(StatusEffects.burning, StatusEffects.melting);

            weapons.add(new LimitedAngleWeapon(name + "-weapon"){{
                layerOffset = -0.01f;

                x = 36.5f;
                y = 2.75f;
                shootY = 19.25f;
                alternate = false;
                rotate = true;
                rotateSpeed = 1.2f;
                inaccuracy = 4f;
                reload = 3f;
                xRand = 4.5f; //TODO use something else instead? -Anuke
                shoot.shots = 2;
                angleCone = 20f;
                angleOffset = -15f;
                shootCone = 20f;
                shootSound = Sounds.flame;
                cooldownTime = 180f;

                bullet = new FlameBulletType(6.6f, 75f){{
                    lifetime = 42f;
                    pierceCap = 6;
                    pierceBuilding = true;
                    collidesAir = true;
                    reflectable = false;
                    incendChance = 0.2f;
                    incendAmount = 1;
                    particleAmount = 23;
                    particleSizeScl = 8f;
                    particleSpread = 11f;
                    hitSize = 9f;
                    layer = Layer.bullet - 0.001f;
                    status = StatusEffects.melting;
                    smokeColors = new Color[]{Pal.darkFlame, Color.darkGray, Color.gray};
                    colors = new Color[]{Color.white, Color.valueOf("fff4ac"), Pal.lightFlame, Pal.darkFlame, Color.gray};
                }};
            }}, new LimitedAngleWeapon(name + "-mount"){{
                x = 20.75f;
                y = 10f;
                shootY = 6.25f;
                rotate = true;
                rotateSpeed = 7f;
                angleCone = 60f;
                reload = 60f;
                shootCone = 30f;
                shootSound = Sounds.missile;

                bullet = new MissileBulletType(2.5f, 22f){{
                    lifetime = 40f;
                    drag = -0.005f;
                    width = 14f;
                    height = 15f;
                    shrinkY = 0f;

                    splashDamageRadius = 55f;
                    splashDamage = 85f;
                    homingRange = 90f;
                    weaveMag = 2f;
                    weaveScale = 8f;

                    hitEffect = despawnEffect = HitFx.hitExplosionLarge;

                    status = StatusEffects.blasted;
                    statusDuration = 60f;

                    fragBullets = 5;
                    fragLifeMin = 0.9f;
                    fragLifeMax = 1.1f;
                    fragBullet = new ShrapnelBulletType(){{
                        damage = 200f;
                        length = 60f;
                        width = 12f;
                        toColor = Pal.missileYellow;
                        hitColor = Pal.bulletYellow;
                        hitEffect = HitFx.coloredHitSmall;
                        serrationLenScl = 5f;
                        serrationSpaceOffset = 45f;
                        serrationSpacing = 5f;
                    }};
                }};
            }}, new Weapon(name + "-cannon"){{
                x = 20.75f;
                y = -4f;
                shootY = 9.75f;
                rotate = true;
                rotateSpeed = 4f;
                inaccuracy = 10f;
                shoot.shots = 8;
                velocityRnd = 0.2f;
                shootSound = Sounds.artillery;
                reload = 40f;

                bullet = new ArtilleryBulletType(3f, 15, "shell"){{
                    hitEffect = Fx.blastExplosion;
                    knockback = 0.8f;
                    lifetime = 125f;
                    width = height = 14f;
                    collides = true;
                    collidesTiles = true;
                    splashDamageRadius = 45f;
                    splashDamage = 95f;
                    backColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                }};
            }});
        }};

        cygnus = new UnityUnitType("cygnus"){{
            speed = 0.26f;
            health = 45000f;
            hitSize = 37f;
            armor = 10f;
            mechLandShake = 1.5f;
            rotateSpeed = 1.3f;

            legCount = 6;
            legLength = 29f;
            legBaseOffset = 8f;
            legMoveSpace = 0.7f;
            legForwardScl = 0.6f;
            hovering = true;
            shadowElevation = 0.23f;
            allowLegStep = true;
            ammoType = new PowerAmmoType(2000);
            groundLayer = Layer.legUnit;

            weapons.add(new Weapon(){{
                x = 0f;
                y = 8.25f;
                mirror = false;
                reload = 4f * 60f;
                recoil = 0f;
                shootSound = Sounds.lasershoot;
                shootStatus = StatusEffects.slow;
                shootStatusDuration = 80f;
                shoot.firstShotDelay = ChargeFx.greenLaserChargeParent.lifetime;

                bullet = new ReflectingLaserBulletType(500f){{
                    lifetime = 65f;
                    shootEffect = ChargeFx.greenLaserChargeParent;
                    healPercent = 6f;
                    splashDamage = 70f;
                    splashDamageRadius = 30f;
                    lightningDamage = 75f;
                    hitEffect = HitFx.coloredHitLarge;
                    hitColor = lightningColor = Pal.heal;
                    pierceCap = 3;
                    collidesTeam = true;
                    lightningLength = 12;
                    colors = new Color[]{Pal.heal.cpy().a(0.2f), Pal.heal.cpy().a(0.5f), Pal.heal.cpy().mul(1.2f), Color.white};
                }};
            }}, new Weapon(name + "-mount"){{
                x = 22.5f;
                y = -3f;
                shootY = 8.75f;
                rotate = true;
                alternate = true;
                rotateSpeed = 5f;
                reload = 25f;
                shootSound = UnitySounds.energyBolt;
                heatColor = Pal.heal;
                inaccuracy = 5f;

                bullet = new CygnusBulletType(){{
                    speed = 6f;
                    damage = 20f;
                    radius = 70f;
                    hitEffect = HitFx.empHit;
                    splashDamage = 5f;
                    splashDamageRadius = 70f;
                    backColor = Pal.heal;

                    shootEffect = Fx.hitEmpSpark;
                    smokeEffect = Fx.shootBigSmoke2;

                    trailLength = 15;
                    trailWidth = 6f;
                    trailColor = Pal.heal;
                    status = StatusEffects.electrified;
                    lightColor = Pal.heal;
                    powerSclDecrease = 0.5f;
                    timeIncrease = 1.25f;
                }};
            }});
        }};

        sagittarius = new UnityUnitType("sagittarius"){{
            speed = 0.25f;
            health = 102500;
            hitSize = 55f;
            armor = 12f;
            mechLandShake = 2f;
            rotateSpeed = 0.8f;

            legCount = 4;
            legLength = 34.36f;
            legBaseOffset = 11f;
            legMoveSpace = 0.7f;
            legForwardScl = 0.6f;
            hovering = true;
            shadowElevation = 0.23f;
            allowLegStep = true;
            ammoType = new PowerAmmoType(2000);
            groundLayer = Layer.legUnit;
            drawShields = false;

            abilities.add(new ForceFieldAbility(130f, 3f, 3500f, 60f * 7));

            weapons.add(new Weapon(name + "-laser"){{
                mirror = false;
                x = 0f;
                y = 0f;
                shootY = 16.75f;
                reload = 12f * 60f;
                shootSound = Sounds.beam;
                shoot.firstShotDelay = ChargeFx.sagittariusCharge.lifetime;
                shootStatus = UnityStatusEffects.sagittariusFatigue;
                shootStatusDuration = 10f * 60f + ChargeFx.sagittariusCharge.lifetime;
                continuous = true;
                cooldownTime = 280f;

                bullet = new SagittariusLaserBulletType(35f){{
                    shootEffect = ChargeFx.sagittariusCharge;
                    lifetime = 10f * 60f;
                    collidesTeam = true;
                    healPercent = 0.4f;
                    splashDamage = 4f;
                    splashDamageRadius = 25f;
                    knockback = 3f;
                    buildingDamageMultiplier = 0.6f;

                    status = StatusEffects.electrified;
                    statusDuration = 30f;
                }};
            }}, new AcceleratingWeapon(name + "-mount"){{
                x = 28.25f;
                y = -9.25f;
                shootY = 17f;
                reload = 30f;
                accelCooldownWaitTime = 31f;
                minReload = 5f;
                accelPerShot = 0.5f;
                rotateSpeed = 5f;
                inaccuracy = 5f;
                rotate = true;
                alternate = false;
                shoot.shots = 2;
                shootSound = UnitySounds.energyBolt;

                bullet = new ArrowBulletType(7f, 25f){{
                    lifetime = 60f;
                    pierce = true;
                    pierceBuilding = true;
                    pierceCap = 4;
                    backColor = trailColor = hitColor = lightColor = lightningColor = Pal.heal;
                    frontColor = Color.white;
                    trailWidth = 4f;
                    width = 9f;
                    height = 15f;
                    splashDamage = 15f;
                    splashDamageRadius = 25f;
                    healPercent = 3f;
                    homingRange = 70f;
                    homingPower = 0.05f;
                }};
            }});
        }};

        araneidae = new UnityUnitType("araneidae"){{
            groundLayer = Layer.legUnit + 0.01f;
            drag = 0.1f;
            speed = 0.42f;
            hitSize = 35.5f;
            health = 30000;
            rotateSpeed = 1.3f;

            legCount = 8;
            legMoveSpace = 0.76f;
            legPairOffset = 0.7f;
            legGroupSize = 2;
            legLength = 112f;
            legExtension = -8.25f;
            legBaseOffset = 8f;
            mechLandShake = 2.4f;
            legLengthScl = 1f;
            rippleScale = 2f;
            legSpeed = 0.2f;

            legSplashDamage = 80f;
            legSplashRange = 40f;
            hovering = true;

            armor = 13f;
            allowLegStep = true;
            shadowElevation = 0.95f;

            weapons.add(new Weapon("unity-araneidae-mount"){{
                x = 15f;
                y = -1.75f;
                shootY = 7.5f;
                reload = 30f;
                shake = 4f;
                rotateSpeed = 2f;
                rotate = true;
                shadow = 15f;
                shoot = new ShootSpread(3, 15f);
                shootSound = Sounds.laser;

                bullet = new LaserBulletType(80f){{
                    colors = new Color[]{Pal.sapBulletBack.cpy().a(0.4f), Pal.sapBullet, Color.white};
                    length = 150f;
                    width = 25f;
                    sideLength = sideWidth = 0f;
                    shootEffect = ShootFx.sapPlasmaShoot;
                    hitColor = lightColor = lightningColor = Pal.sapBullet;
                    status = StatusEffects.sapped;
                    statusDuration = 80f;
                    lightningSpacing = 17f;
                    lightningDelay = 0.12f;
                    lightningDamage = 15f;
                    lightningLength = 4;
                    lightningLengthRand = 2;
                    lightningAngleRand = 15f;
                }};
            }}, new MultiBarrelWeapon("unity-araneidae-cannon"){{
                mirror = false;
                x = 0f;
                y = -12.25f;
                shootY = 22f;
                reload = 120f;
                shake = 10f;
                recoil = 3f;
                rotateSpeed = 1f;
                ejectEffect = Fx.none;
                shootSound = Sounds.railgun;
                rotate = true;
                shadow = 40f;
                barrelSpacing = 11.25f;
                barrelOffset = 8.5f;
                barrelRecoil = 5f;
                barrels = 2;

                bullet = new SlowRailBulletType(15f, 95f){{
                    lifetime = 20f;
                    splashDamageRadius = 90f;
                    splashDamage = 90f;
                    hitEffect = Fx.sapExplosion;
                    ammoMultiplier = 4f;
                    trailEffect = TrailFx.coloredRailgunSmallTrail;
                    trailSpacing = 15f;
                    backColor = trailColor = Pal.sapBulletBack;
                    frontColor = lightningColor = Pal.sapBullet;
                    lightning = 3;
                    lightningLength = 20;
                    smokeEffect = Fx.shootBigSmoke2;
                    hitShake = 10f;
                    lightRadius = 40f;
                    lightColor = Pal.sap;
                    lightOpacity = 0.6f;
                    width = 12f;
                    height = 23f;
                    shrinkY = 0f;
                    collidesAir = false;
                    scaleLife = true;
                    pierceCap = 2;

                    status = StatusEffects.sapped;
                    statusDuration = 60f * 10;

                    fragLifeMin = 0.3f;
                    fragBullets = 4;

                    fragBullet = UnityBullets.sapArtilleryFrag;
                }};
            }});
        }};

        theraphosidae = new UnityUnitType("theraphosidae"){{
            speed = 0.4f;
            drag = 0.12f;
            hitSize = 49f;
            hovering = true;
            allowLegStep = true;
            health = 38750;
            armor = 16f;
            rotateSpeed = 1.3f;
            legCount = 8;
            legGroupSize = 2;
            legMoveSpace = 0.7f;
            legPairOffset = 0.2f;
            legLength = 176f;
            legExtension = -24f;
            legBaseOffset = 9f;
            shadowElevation = 1f;
            groundLayer = Layer.legUnit + 0.02f;
            rippleScale = 3.4f;
            legSplashDamage = 130f;
            legSplashRange = 60f;
            targetAir = false;

            weapons.add(new LimitedAngleWeapon(name + "-launcher"){{
                layerOffset = -0.01f;

                x = 33f;
                y = 8.5f;
                shootY = 6.25f - 1f;
                reload = 7f;
                recoil = 1f;
                rotate = true;
                shootCone = 20f;
                angleCone = 60f;
                angleOffset = 45f;
                inaccuracy = 25f;
                xRand = 2.25f; //TODO use something else instead? -Anuke
                shoot.shots = 2;
                shootSound = Sounds.missile;

                bullet = new MissileBulletType(3.7f, 15f){{
                    width = 10f;
                    height = 12f;
                    shrinkY = 0f;
                    drag = -0.01f;
                    splashDamageRadius = 30f;
                    splashDamage = 55f;
                    ammoMultiplier = 5f;
                    hitEffect = Fx.blastExplosion;
                    despawnEffect = Fx.blastExplosion;
                    backColor = trailColor = Pal.sapBulletBack;
                    frontColor = lightningColor = lightColor = Pal.sapBullet;
                    trailLength = 13;
                    homingRange = 80f;
                    weaveScale = 8f;
                    weaveMag = 2f;
                    lightning = 2;
                    lightningLength = 2;
                    lightningLengthRand = 1;
                    lightningCone = 15f;

                    status = StatusEffects.blasted;
                    statusDuration = 60f;
                }};
            }}, new LimitedAngleWeapon(name + "-mount"){{
                x = 26.75f;
                y = 7.5f;
                shootY = 10.25f - 5f;
                reload = 120f;
                angleCone = 60f;
                rotate = true;
                continuous = true;
                alternate = false;
                rotateSpeed = 1.5f;
                recoil = 5f;
                shootSound = UnitySounds.continuousLaserA;

                bullet = UnityBullets.continuousSapLaser;
            }}, new Weapon(name + "-railgun"){{
                x = 20.5f;
                y = -10f;
                shootY = 20.5f - 4f;
                shootSound = Sounds.railgun;
                rotate = true;
                alternate = true;
                rotateSpeed = 0.9f;
                cooldownTime = 90f;
                reload = 90f;
                shake = 6f;
                recoil = 8f;

                bullet = new SlowRailBulletType(15f, 95f){{
                    lifetime = 23f;
                    splashDamageRadius = 110f;
                    splashDamage = 90f;
                    hitEffect = Fx.sapExplosion;
                    ammoMultiplier = 4f;
                    trailEffect = TrailFx.coloredRailgunSmallTrail;
                    trailSpacing = 15f;
                    backColor = trailColor = Pal.sapBulletBack;
                    frontColor = lightningColor = Pal.sapBullet;
                    lightning = 3;
                    lightningLength = 20;
                    smokeEffect = Fx.shootBigSmoke2;
                    hitShake = 10f;
                    lightRadius = 40f;
                    lightColor = Pal.sap;
                    lightOpacity = 0.6f;
                    width = 13f;
                    height = 27f;
                    shrinkY = 0f;
                    collidesAir = false;
                    scaleLife = true;
                    pierceCap = 3;

                    status = StatusEffects.sapped;
                    statusDuration = 60f * 10;

                    fragLifeMin = 0.3f;
                    fragBullets = 4;

                    fragBullet = UnityBullets.sapArtilleryFrag;
                }};
            }});
        }};

        mantle = new UnityUnitType("mantle"){{
            health = 54000f;
            armor = 17f;
            speed = 0.45f;
            accel = 0.04f;
            drag = 0.04f;
            rotateSpeed = 0.9f;
            flying = true;
            lowAltitude = true;
            targetFlags = new BlockFlag[]{BlockFlag.reactor, null};
            hitSize = 80f;
            engineOffset = 42.75f;
            engineSize = 5.75f;

            BulletType b = UnitTypes.scepter.weapons.get(0).bullet.copy();
            b.speed = 6.5f;
            b.damage = 60f;
            b.lifetime = 47f;

            weapons.add(new Weapon(){{
                x = 0f;
                y = 0f;
                shootY = 4f;
                mirror = false;
                reload = 4f * 60f;
                continuous = true;
                recoil = 0f;
                shootStatus = StatusEffects.slow;
                shootStatusDuration = 180f;
                shootSound = Sounds.beam;

                bullet = new AcceleratingLaserBulletType(230f){{
                    lifetime = 180f;
                    maxLength = 380f;
                    maxRange = 330f;
                    oscOffset = 0.1f;
                    incendChance = 0.2f;
                    incendAmount = 2;
                    width = 27f;
                    collisionWidth = 10f;
                    pierceCap = 2;
                    hitEffect = HitFx.coloredHitLarge;
                    hitColor = Pal.meltdownHit;
                }};
            }}, new Weapon(name + "-mount"){{
                x = 30.75f;
                y = -6.25f;
                shootY = 10.5f;
                alternate = true;
                rotate = true;
                recoil = 5f;
                reload = 55f;
                shoot.shots = 4;
                shoot.shotDelay = 4f;
                rotateSpeed = 3f;
                shadow = 22f;
                shootSound = Sounds.shootBig;

                bullet = b;
            }}, new Weapon(name + "-mount"){{
                x = 19f;
                y = -18f;
                shootY = 10.5f;
                alternate = true;
                rotate = true;
                recoil = 5f;
                reload = 60f;
                shoot.shots = 4;
                shoot.shotDelay = 4f;
                rotateSpeed = 3f;
                shadow = 22f;
                shootSound = Sounds.shootBig;

                bullet = b;
            }});
        }};

        aphelion = new UnityUnitType("aphelion"){{
            health = 130000f;
            armor = 16f;
            speed = 0.44f;
            accel = 0.04f;
            drag = 0.03f;
            rotateSpeed = 0.7f;
            flying = true;
            lowAltitude = true;
            targetFlags = new BlockFlag[]{BlockFlag.reactor, null};
            hitSize = 96f;
            engineOffset = 46.5f;
            engineSize = 6.75f;

            BulletType b = UnitTypes.scepter.weapons.get(0).bullet.copy();
            b.speed = 6.5f;
            b.damage = 40f;
            b.lightning = 3;
            b.lightningDamage = 27f;
            b.lightningCone = 360f;
            b.lifetime = 50f;
            b.lightningLength = 14;
            b.lightningType = new BulletType(0f, 10f){
                {
                    lifetime = Fx.lightning.lifetime;
                    hitEffect = Fx.hitLancer;
                    despawnEffect = Fx.none;
                    status = StatusEffects.shocked;
                    statusDuration = 60f;
                    hittable = false;
                    lightningColor = b.lightningColor;
                    lightning = 1;
                    lightningCone = 65f;
                    lightningLength = 6;
                    lightningLengthRand = 3;
                }

                @Override
                public void init(Bullet b){
                    if(Mathf.chance(0.3f)) Lightning.create(b.team, lightningColor, damage, b.x, b.y, b.rotation() + Mathf.range(lightningCone), lightningLength + Mathf.random(lightningLengthRand));
                }

                @Override
                public void hit(Bullet b, float x, float y){

                }
            };

            weapons.add(new Weapon(name + "-laser"){{
                x = 0f;
                y = 0f;
                shootY = 34.25f;
                shootCone = 2f;
                mirror = false;
                reload = 7f * 60f;
                continuous = true;
                recoil = 0f;
                cooldownTime = 6f * 60f;
                shootSound = Sounds.beam;

                bullet = new AcceleratingLaserBulletType(320f){{
                    lifetime = 4f * 60f;
                    maxLength = 430f;
                    maxRange = 400f;
                    oscOffset = 0.2f;
                    incendChance = 0.3f;
                    incendAmount = 2;
                    width = 37f;
                    collisionWidth = 16f;
                    accel = 60f;
                    laserSpeed = 20f;
                    splashDamage = 40f;
                    splashDamageRadius = 50f;
                    pierceCap = 5;
                    hitEffect = HitFx.coloredHitLarge;
                    hitColor = Pal.meltdownHit;
                }};

                shootStatus = StatusEffects.slow;
                shootStatusDuration = bullet.lifetime;
            }}, new Weapon(name + "-mount"){{
                x = 30f;
                y = -9.5f;
                shootY = 14.25f;
                shadow = 32f;
                rotate = true;
                rotateSpeed = 2f;
                reload = 2f;
                xRand = 3f; //TODO use something else instead? -Anuke
                inaccuracy = 4f;
                shootSound = Sounds.shootBig;

                bullet = b;
            }});
        }};

        sedec = new UnityUnitType("sedec"){{
            controller = u -> new HealingDefenderAI();
            health = 45000f;
            armor = 20f;
            speed = 0.7f;
            rotateSpeed = 1f;
            accel = 0.04f;
            drag = 0.018f;
            flying = true;
            engineOffset = 48f;
            engineSize = 7.8f;
            faceTarget = false;
            hitSize = 85f;
            payloadCapacity = (6.2f * 6.2f) * tilePayload;
            buildSpeed = 5f;
            drawShields = false;
            buildBeamOffset = 29.5f;

            //ammo resupply mechanics removed in 129 until further notice; TODO remove or rework
            //ammoCapacity = 1700;
            //ammoResupplyAmount = 30;

            abilities.add(new ForceFieldAbility(190f, 6f, 8000f, 60f * 12), new RepairFieldAbility(180f, 60f * 2, 160f));

            weapons.add(new Weapon(name + "-laser"){{
                layerOffset = -0.01f;

                x = 0f;
                y = 0f;
                shootY = 42f - 3f;
                reload = 260f;
                recoil = 3f;
                continuous = rotate = true;
                mirror = false;
                rotateSpeed = 1.5f;
                shootSound = Sounds.tractorbeam;

                bullet = new HealingConeBulletType(3f){{
                    healPercent = 6f;
                    allyStatus = StatusEffects.overclock;
                    allyStatusDuration = 9f * 60f;
                    status = UnityStatusEffects.weaken;
                    statusDuration = 40f;
                    lifetime = 6f * 60f;
                }};
            }});
        }};

        trigintaduo = new UnityUnitType("trigintaduo"){{
            controller = u -> new HealingDefenderAI();
            health = 52500f;
            armor = 22f;
            speed = 0.6f;
            rotateSpeed = 1f;
            accel = 0.04f;
            drag = 0.018f;
            flying = true;
            engineOffset = 41.25f;
            engineSize = 6.5f;
            faceTarget = false;
            hitSize = 105.5f;
            payloadCapacity = (8.1f * 8.1f) * tilePayload;
            buildSpeed = 6f;
            drawShields = false;
            buildBeamOffset = 47.75f;

            //ammo resupply mechanics removed in 129 until further notice; TODO remove or rework
            //ammoCapacity = 2000;
            //ammoResupplyAmount = 35;

            weapons.add(new Weapon(name + "-heal-mount"){{
                x = 33.5f;
                y = -7.75f;
                shootY = 10.25f;
                reload = 220f;
                recoil = 3f;
                shadow = 22f;
                continuous = rotate = true;
                alternate = false;
                rotateSpeed = 3.5f;
                shootSound = Sounds.tractorbeam;

                bullet = new HealingConeBulletType(3f){{
                    healPercent = 3f;
                    cone = 15f;
                    scanAccuracy = 25;
                    allyStatus = StatusEffects.overclock;
                    allyStatusDuration = 9f * 60f;
                    status = UnityStatusEffects.weaken;
                    statusDuration = 40f;
                    lifetime = 6f * 60f;
                }};
            }}, new EnergyChargeWeapon(""){{
                mirror = false;
                x = 0f;
                y = 10.75f;
                shootY = 0f;

                reload = 30f * 60f;
                shootCone = 360f;
                ignoreRotation = true;

                drawCharge = (unit, mount, charge) -> {
                    float rotation = unit.rotation - 90f,
                        wx = unit.x + Angles.trnsx(rotation, x, y),
                        wy = unit.y + Angles.trnsy(rotation, x, y);

                    Draw.color(Pal.heal);
                    UnityDrawf.shiningCircle(unit.id, Time.time, wx, wy, 13f * charge, 5, 70f, 15f, 6f * charge, 360f);
                    Draw.color(Color.white);
                    UnityDrawf.shiningCircle(unit.id, Time.time, wx, wy, 6.5f * charge, 5, 70f, 15f, 4f * charge, 360f);
                };

                bullet = new HealingNukeBulletType(){{
                    allyStatus = StatusEffects.overclock;
                    allyStatusDuration = 15f * 60f;
                    status = UnityStatusEffects.disabled;
                    statusDuration = 120f;
                    healPercent = 20f;
                }};
            }});
        }};

        //endregion
        //region naval-units

        fin = new UnityUnitType("fin"){{
            health = 36250f;
            speed = 0.5f;
            drag = 0.18f;
            hitSize = 77.5f;
            armor = 17f;
            accel = 0.19f;
            rotateSpeed = 0.86f;
            faceTarget = false;

            trailLength = 70;
            waveTrailX = 18f;
            waveTrailY = -32f;
            trailScl = 3.5f;

            weapons.add(new Weapon(name + "-launcher"){{
                x = 19f;
                y = 14f;
                shootY = 8f;
                rotate = true;
                inaccuracy = 15f;
                reload = 7f;
                xRand = 2.25f; //TODO use something else instead? -Anuke
                shootSound = Sounds.missile;

                bullet = UnityBullets.basicMissile;
            }}, new Weapon(name + "-launcher"){{
                x = 24.5f;
                y = -39.25f;
                shootY = 8f;
                rotate = true;
                inaccuracy = 15f;
                reload = 7f;
                xRand = 2.25f; //TODO use something else instead? -Anuke
                shootSound = Sounds.missile;

                bullet = UnityBullets.basicMissile;
            }}, new MortarWeapon(name + "-mortar"){{
                x = 0f;
                y = -13.75f;
                shootY = 39.5f;
                mirror = false;
                rotate = true;
                rotateSpeed = 1f;
                shoot.shots = 3;
                inaccuracy = 3f;
                velocityRnd = 0.1f;
                reload = 60f * 2f;
                recoil = 2f;
                shootSound = Sounds.artillery;

                bullet = new MortarBulletType(7f, 4f){{
                    width = height = 22f;
                    splashDamageRadius = 160f;
                    splashDamage = 160f;
                    trailWidth = 7f;
                    trailColor = Pal.bulletYellowBack;
                    hitEffect = HitFx.hitExplosionMassive;
                    lifetime = 65f;
                    fragBullet = new ArtilleryBulletType(3f, 20, "shell"){{
                        hitEffect = Fx.flakExplosion;
                        knockback = 0.8f;
                        lifetime = 80f;
                        width = height = 11f;
                        collidesTiles = false;
                        splashDamageRadius = 25f * 0.75f;
                        splashDamage = 33f;
                    }};
                    fragBullets = 7;
                    fragLifeMax = 0.15f;
                    fragLifeMin = 0.15f;
                    despawnHit = true;
                    collidesAir = false;
                }};
            }});
        }};

        blue = new UnityUnitType("blue"){{
            health = 42500f;
            speed = 0.4f;
            drag = 0.18f;
            hitSize = 80f;
            armor = 18f;
            accel = 0.19f;
            rotateSpeed = 0.78f;
            faceTarget = false;

            trailLength = 70;
            waveTrailX = 26f;
            waveTrailY = -42f;
            trailScl = 4f;

            float spawnTime = 15f * 60f;

            abilities.add(new UnitSpawnAbility(schistocerca, spawnTime, 24.75f, -29.5f), new UnitSpawnAbility(schistocerca, spawnTime, -24.75f, -29.5f));

            weapons.addAll(new LimitedAngleWeapon(name + "-front-cannon"){{
                layerOffset = -0.01f;

                x = 22.25f;
                y = 30.25f;
                shootY = 9.5f;
                recoil = 5f;
                shoot.shots = 5;
                shoot.shotDelay = 3f;
                inaccuracy = 5f;
                shootCone = 15f;
                rotate = true;
                shootSound = Sounds.artillery;
                reload = 25f;

                bullet = new BasicBulletType(8f, 80, "bullet"){{
                    hitSize = 5;
                    width = 16f;
                    height = 23f;
                    shootEffect = Fx.shootBig;
                    pierceCap = 2;
                    pierceBuilding = true;
                    knockback = 0.7f;
                }};
            }}, new LimitedAngleWeapon(name + "-side-silo"){
                {
                    layerOffset = -0.01f;

                    x = 29.75f;
                    y = -13f;
                    shootY = 7f;
                    xRand = 9f; //TODO use something else instead? -Anuke
                    defaultAngle = angleOffset = 90f;
                    angleCone = 0f;
                    shootCone = 125f;
                    alternate = false;
                    rotate = true;
                    reload = 50f;
                    shoot.shots = 12;
                    shoot.shotDelay = 3f;
                    inaccuracy = 5f;
                    shootSound = Sounds.missile;

                    bullet = new GuidedMissileBulletType(3f, 20f){{
                        homingPower = 0.09f;
                        width = 8f;
                        height = 8f;
                        shrinkX = shrinkY = 0f;
                        drag = -0.003f;
                        keepVelocity = false;
                        splashDamageRadius = 40f;
                        splashDamage = 45f;
                        lifetime = 65f;
                        trailColor = Pal.missileYellowBack;
                        hitEffect = Fx.blastExplosion;
                        despawnEffect = Fx.blastExplosion;
                    }};
                }

                @Override
                public void handleBullet(Unit unit, WeaponMount mount, Bullet b){
                    if(b.type instanceof GuidedMissileBulletType){
                        b.data = mount;
                    }
                }
            }, new LimitedAngleWeapon(fin.name + "-launcher"){{
                x = 0f;
                y = 21f;
                shootY = 8f;
                rotate = true;
                mirror = false;
                inaccuracy = 15f;
                reload = 7f;
                xRand = 2.25f; //TODO use something else instead? -Anuke
                shootSound = Sounds.missile;
                angleCone = 135f;

                bullet = UnityBullets.basicMissile;
            }}, new PointDefenceMultiBarrelWeapon(name + "-flak-turret"){{
                x = 26.5f;
                y = 15f;
                shootY = 15.75f;
                barrels = 2;
                barrelOffset = 5.25f;
                barrelSpacing = 6.5f;
                barrelRecoil = 4f;
                rotate = true;
                mirrorBarrels = true;
                alternate = false;
                reload = 6f;
                recoil = 0.5f;
                shootCone = 7f;
                shadow = 30f;
                targetInterval = 20f;
                autoTarget = true;
                controllable = false;
                bullet = new AntiBulletFlakBulletType(8f, 6f){{
                    lifetime = 45f;
                    splashDamage = 12f;
                    splashDamageRadius = 60f;
                    bulletRadius = 60f;
                    explodeRange = 45f;
                    bulletDamage = 18f;
                    width = 8f;
                    height = 12f;
                    scaleLife = true;
                    collidesGround = false;
                    status = StatusEffects.blasted;
                    statusDuration = 60f;
                }};
            }}, new Weapon(name + "-railgun"){{
                x = 0f;
                y = 0f;
                shootY = 38.5f;
                mirror = false;
                rotate = true;
                rotateSpeed = 0.7f;
                shadow = 46f;
                reload = 60f * 2.5f;
                shootSound = Sounds.railgun;

                bullet = new SlowRailBulletType(70f, 2100f){{
                    lifetime = 10f;
                    width = 20f;
                    height = 38f;
                    splashDamage = 50f;
                    splashDamageRadius = 30f;
                    pierceDamageFactor = 0.15f;
                    pierceCap = -1;
                    fragBullet = new BasicBulletType(3.5f, 18){{
                        width = 9f;
                        height = 12f;
                        reloadMultiplier = 0.6f;
                        ammoMultiplier = 4;
                        lifetime = 60f;
                    }};
                    fragBullets = 2;
                    fragRandomSpread = 20f;
                    fragLifeMin = 0.4f;
                    fragLifeMax = 0.7f;
                    trailSpacing = 40f;
                    trailEffect = TrailFx.coloredArrowTrail;
                    backColor = trailColor = Pal.bulletYellowBack;
                    frontColor = Pal.bulletYellow;
                    collisionWidth = 12f;
                }};
            }});
        }};

        modularUnitSmall = new UnityUnitType("modularUnit"){{
            faceTarget = false;
            omniMovement = false;
            weapons.add(new Weapon("gun")); //blank weapon so mobile doesn't die
            //stats? what stats? :D
            templates.addAll("eJxjZmZg52PmY+dh52NgZAIiRgYGBkYwAgMACR4AXg==","eJxjZmHg5OPhYxbn4WPnY2BkYmBkAJFAgoERyIQDABF0AIw=");
        }};
    }
}
