package unity.content;

import arc.math.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import unity.annotations.Annotations.*;
import unity.entities.*;
import unity.gen.*;
import unity.type.*;

public final class UnityUnitTypes{
    // Global {unit, copter}.
    public static @EntityDef({Unitc.class, Copterc.class})
    UnitType caelifera, schistocerca, anthophila, vespula, lepidoptera;//, mantodea, meganisoptera;

    private UnityUnitTypes(){
        throw new AssertionError();
    }

    public static void load(){
        caelifera = new UnityUnitType("caelifera"){{
            defaultController = FlyingAI::new;
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
            defaultController = FlyingAI::new;
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
                x = 6.75f;
                y = 5.75f;
                shootX = -0.5f;
                shootY = 2f;
                shootSound = Sounds.shootSnap;
                ejectEffect = Fx.casing1;
                reload = 30f;

                bullet = Bullets.standardIncendiary;
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
            defaultController = FlyingAI::new;
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
                shots = 3;
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
            defaultController = FlyingAI::new;
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
                shots = 4;
                shotDelay = 2f;
                shootSound = Sounds.shootSnap;

                bullet = Bullets.standardThorium;
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
            defaultController = FlyingAI::new;
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
                shots = 2;
                spacing = 2f;
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
                shots = 3;
                spacing = 15f;
                shotDelay = 0f;
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
    }
}
