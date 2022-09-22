package unity.content;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.weapons.*;
import unity.entities.abilities.*;
import unity.entities.bullet.physical.*;
import unity.parts.*;
import unity.parts.types.*;
import unity.content.effects.*;
import unity.type.weapons.*;

//TODO OH GOD THE INDENTING WHOS USING 3 SPACES 
public class UnityParts{
    //unit
    //misc
    public static ModularPartType panel, mediumPanel, smallRoot, mediumRoot, largeRoot, largePanel, storage;
    //weapons
    public static ModularPartType gun, flamethrower, grenadeLauncher, cannon, howitzer, pointDefense, tankCannon; //harpoon?
    public static ModularPartType gunBridge;
    //movement
    public static ModularPartType smallEngine, engine, smallWheel, smallTracks, mediumWheel, largeWheel, largeTracks, tankTracks, tankTracksLarge;
    public static ModularPartType differential;
    //abilities
    public static ModularPartType pipebomb, pretender;
    //unit
    public static Seq<PanelDoodadPalette> unitDoodads = new Seq<>();

    public static void load(){
        //region units
        ///DO NOT CHANGE ORDER (will break saves)
        panel = new ModularPartType("panel"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.copper, 5, UnityItems.nickel, 5));
            health(40);
            mass(20);
            armor(2);
        }};
        mediumPanel = new ModularPartType("medium-panel"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 20, Items.titanium, 50));
            w = 2;
            h = 2;
            health(200);
            mass(120);
            armor(100);
        }};
        largePanel = new ModularPartType("large-panel"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 50, Items.titanium, 100, UnityItems.nickel, 50));
            w = 3;
            h = 3;
            health(600);
            mass(200);
            armor(2000);
        }};
        smallRoot = new ModularPartType("root-small"){{
            requirements(PartCategories.miscUnit, ItemStack.with(UnityItems.nickel, 10));
            health(100);
            mass(10);
            producesPower(15, 5);
            addsWeaponSlots(2);
            itemCapacity(20); //the default
            addsAbilitySlots(1);
            root = true;
            hasCellDecal = true;
        }};
        mediumRoot = new ModularPartType("root-medium"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 30, Items.graphite, 20, UnityItems.cupronickel, 45));
            w = 2;
            h = 2;
            health(250);
            mass(40);
            producesPower(50, 5);
            addsWeaponSlots(4);
            itemCapacity(50);
            armor(100);
            addsAbilitySlots(2);
            root = true;
            hasCellDecal = true;
        }};
        largeRoot = new ModularPartType("root-large"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 75, Items.titanium, 50, UnityItems.nickel, 30, UnityItems.superAlloy, 15));
            w = 3;
            h = 3;
            health(900);
            armor(4000);
            mass(150);
            producesPower(100, 5);
            addsWeaponSlots(10);
            itemCapacity(100);
            addsAbilitySlots(4);
            root = true;
            hasCellDecal = true;
        }};
        gunBridge = new ModularPartType("gun-bridge"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.graphite, 10, UnityItems.nickel, 10));
            usesPower(20, 4, 4);
            mass(20);
            addsWeaponSlots(3);
            hasCellDecal = true;
        }};
        gun = new ModularWeaponMountType("gun"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.graphite, 10));
            health(10);
            mass(20);
            usesPower(5,3,6);
            weapon(1, new Weapon("unity-part-mount-gun"){{
                rotate = true;
                reload = 18f;
                bullet = new BasicBulletType(3f, 11){{
                    width = 7f;
                    height = 10f;
                    lifetime = 60f;
                }};
            }});
        }};
        cannon = new ModularWeaponMountType("cannon"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.silicon, 40, Items.graphite, 20, UnityItems.nickel, 30));
            health(40);
            mass(100);
            usesPower(20,2,4);
            w = 2;
            h = 2;
            weapon(2, new Weapon("unity-part-mount-cannon"){{
                rotate = true;
                rotateSpeed = 6f;
                reload = 60f;
                ejectEffect = Fx.casing2;
                shootSound = Sounds.bang;
                shootY = 4f;
                bullet = new BasicBulletType(7f, 40){{
                    width = 11f;
                    height = 20f;
                    lifetime = 24f;
                    shootEffect = Fx.shootBig;
                    splashDamage = 20;
                    splashDamageRadius = 25;
                    hitEffect = Fx.blastExplosion;
                }};
            }});
        }};

        howitzer = new ModularWeaponMountType("howitzer"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.silicon, 80, Items.titanium, 120, Items.graphite, 80, UnityItems.nickel, 50));
            health(75);
            mass(550);
            usesPower(80,1,2);
            w = 3;
            h = 4;
            weapon(5, new Weapon("unity-part-mount-howitzer"){{
                rotate = true;
                rotateSpeed = 4f;
                reload = 80f;
                ejectEffect = Fx.casing3;
                shoot.shots = 2;
                shoot.shotDelay = 2f;
                inaccuracy = 6;
                shootSound = Sounds.artillery;
                shootY = 7f;
                bullet = new ArtilleryBulletType(5f, 50){{
                    width = 15f;
                    height = 25f;
                    lifetime = 40;
                    shootEffect = Fx.shootBig;
                    splashDamage = 50;
                    splashDamageRadius = 55;
                    hitEffect = Fx.blastExplosion;
                    fragLifeMin = 0.1f;
                    fragLifeMax = 0.3f;
                    fragBullets = 5;
                    fragBullet = new BasicBulletType(4f, 29, "bullet"){{
                        width = 10f;
                        height = 13f;
                        shootEffect = Fx.shootBig;
                        smokeEffect = Fx.shootBigSmoke;
                        ammoMultiplier = 4;
                        lifetime = 60f;
                    }};
                }};
            }});
        }};

        flamethrower = new ModularWeaponMountType("flamethrower"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.coal, 10, Items.graphite, 30));
            health(10);
            mass(60);
            usesPower(10,4,6);
            w = 1;
            h = 2;
            weapon(2, new Weapon("unity-part-mount-flamethrower"){{
                rotate = true;
                reload = 2f;
                inaccuracy = 10;
                shootSound = Sounds.flame;
                shootY = 6f;
                recoilTime = 15f;
                bullet = new BulletType(3.35f, 12f){{
                    ammoMultiplier = 3f;
                    hitSize = 7f;
                    lifetime = 18f;
                    pierce = true;
                    collidesAir = false;
                    statusDuration = 60f * 4;
                    shootEffect = Fx.shootSmallFlame;
                    hitEffect = Fx.hitFlameSmall;
                    despawnEffect = Fx.none;
                    status = StatusEffects.burning;
                    keepVelocity = false;
                    hittable = false;
                }};
            }});
        }};

        grenadeLauncher = new ModularWeaponMountType("grenade-launcher"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.metaglass, 20, UnityItems.nickel, 30, Items.graphite, 30));
            health(20);
            mass(80);
            usesPower(20,2,6);
            w = 2;
            h = 2;
            weapon(2, new Weapon("unity-part-mount-grenade-launcher"){{
                rotate = true;
                reload = 22f;
                inaccuracy = 3;
                shootSound = Sounds.shootBig;
                shootY = 5f;
                bullet = new GrenadeBulletType(){{
                    damage = 5;
                    splashDamage = 45;
                    splashDamageRadius = 25;
                    lifetime = 130;
                    speed = 3;
                    fragLifeMin = 0.1f;
                    fragLifeMax = 0.2f;
                    fragBullets = 2;
                    fragBullet = new BasicBulletType(2.5f, 9){{
                        width = 7f;
                        height = 9f;
                        lifetime = 60f;
                        ammoMultiplier = 2;
                    }};
                    collidesAir = false;
                }};
            }});
        }};


        smallEngine = new ModularEngineType("engine-small"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.copper, 10, UnityItems.nickel, 5));
            health(10);
            mass(15);
            producesPower(20, 3);
        }};
        engine = new ModularEngineType("engine"){{
            requirements(PartCategories.movementUnit, ItemStack.with(UnityItems.cupronickel, 90, Items.lead, 50, Items.silicon, 30));
            w = 3;
            h = 3;
            health(90);
            mass(500);
            producesPower(400, 1.5f);
        }};
        smallWheel = new ModularWheelType("wheel-small"){{
            requirements(PartCategories.movementUnit, ItemStack.with(UnityItems.nickel, 5));
            health(15);
            mass(15);
            wheel(1, 1,30, 1.5f);
            usesPower(7,4,12); // uses 7 power at speed 4 rps with a maxspeed of 12 (21 power)
            isWheel = true;
        }};


        mediumWheel = new ModularWheelType("wheel-medium"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 50, Items.titanium, 25));
            w = 2;
            h = 4;
            health(75);
            mass(90);
            wheel(2, 2,240, 1.3f);
            usesPower(50, 2,12);
            isWheel = true;
        }};
        largeWheel = new ModularWheelType("wheel-large"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 400, Items.titanium, 150, Items.thorium, 90, UnityItems.cupronickel, 100));
            w = 3;
            h = 8;
            health(150);
            mass(200);
            wheel(4, 3,700, 1f);
            usesPower(250,1,6);
            isWheel = true;
        }};
        pointDefense = new ModularWeaponMountType("point-defense"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.silicon, 60, UnityItems.nickel, 30));
            w = 2;
            h = 3;
            health(60);
            mass(60);
            usesPower(15,4,4);
            weapon(3, new PointDefenseWeapon("unity-part-point-defense"){{
                rotate = true;
                reload = 18f;
                targetInterval = 0f;
                targetSwitchInterval = 0f;
                shootSound = Sounds.lasershoot;
                shootY = 4f;
                bullet = new BulletType(){{
                    smokeEffect = Fx.pointHit;
                    hitEffect = Fx.pointHit;
                    maxRange = 120f;
                    damage = 9f;
                    speed = 3f;
                }};
            }});
        }};

        differential = new ModularPartType("differential"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.graphite, 10, UnityItems.cupronickel, 5));
            w = 3;
            h = 1;
            health(10);
            mass(30);
            differentialSteer();
        }};
        smallTracks = new ModularWheelType("tracks-small"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 15, UnityItems.nickel, 10));
            w = 1;
            h = 3;
            health(60);
            mass(45);
            wheel(1, 5,180, 0.3f);
            usesPower(20, 1, 6);
        }};

        largeTracks = new ModularWheelType("tracks-large"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 50, Items.thorium, 40, UnityItems.nickel, 30));
            w = 2;
            h = 9;
            health(180);
            mass(200);
            wheel(2, 10,950, 0.1f);
            usesPower(60, 0.5f, 6);
        }};
        //are these necessary?
        tankTracks = new ModularWheelType("tank-tracks"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 250, Items.titanium, 100, UnityItems.nickel, 100, UnityItems.cupronickel, 50));
            w = 3;
            h = 16;
            health(210);
            mass(250);
            wheel(3, 30, 2000, 0.1f);
            usesPower(200, 0.25f, 6);
        }};
        tankTracksLarge = new ModularWheelType("tank-tracks-large"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 700, Items.titanium, 500, UnityItems.nickel, 300, UnityItems.cupronickel, 200, UnityItems.superAlloy, 100));
            w = 5;
            h = 30;
            health(500);
            mass(500);
            wheel(5, 100,10000, 0.1f);
            usesPower(1200, 0.125f, 6);
        }};
        tankCannon = new ModularWeaponMountType("arbiter"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.silicon, 500, Items.titanium, 500, UnityItems.superAlloy, 200, UnityItems.nickel, 300, UnityItems.cupronickel, 150));
            health(750);
            mass(1800);
            usesPower(400, 0.25f, 0.40f);
            w = 9;
            h = 12;
            weapon(30, new MultiBarrelWeapon("unity-part-tonk-cannon"){{
                rotate = true;
                rotateSpeed = 1.5f;
                recoil = 0f;
                reload = 160f;
                shootY = 36f;
                barrels = 1;
                barrelRecoil = 7f;
                ejectEffect = Fx.casing3Double;
                shootSound = Sounds.bang;
                cooldownTime = 120f;
                shake = 4f;
                bullet = new BasicBulletType(12f, 1000){{
                    width = 25f;
                    height = 80f;
                    lifetime = 40f;
                    pierce = true;
                    pierceBuilding = true;
                    pierceCap = 3;
                    shootEffect = ShootFx.tonkCannon;
                    smokeEffect = ShootFx.tonkCannonSmoke;
                    hitEffect = Fx.massiveExplosion;
                    despawnEffect = Fx.massiveExplosion;
                    trailChance = 0f;
                    trailLength = 10;
                    trailWidth = 3f;
                    trailColor = Pal.bulletYellowBack;
                }};
            }});
        }};

        storage = new ModularPartType("storage"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 55, UnityItems.cupronickel, 20, UnityItems.nickel, 30));
            w = 2;
            h = 2;
            itemCapacity(20);
            health(50);
            mass(150);
        }};


        pipebomb = new ModularUnitAbilityType("pipebomb"){{
            requirements(PartCategories.specialUnit, ItemStack.with(Items.coal, 20, Items.titanium, 10, Items.lead, 10));
            health(20);
            mass(80);
            w = 2;
            h = 1;
            ability(1, new SuicideExplosionAbility(){{
                radius = 45f;
                damage = 90f;
            }});
        }};


        //endregion
    }

    public static void loadDoodads(){
        unitDoodads.add(new PanelDoodadPalette(true, true, 1, 1, "1x1", 12));
        unitDoodads.add(new PanelDoodadPalette(false, true, 2, 2, "2x2", 5));
        unitDoodads.add(new PanelDoodadPalette(false, true, 3, 3, "3x3", 4));
        unitDoodads.add(new PanelDoodadPalette(true, true, 3, 2, "3x2", 3));

        for(int i = 0; i < unitDoodads.size; i++){
            unitDoodads.get(i).load();
        }
    }

    static PanelDoodadType getUnitDoodad(String name, String outlinename, int... pos){
        int w = 1, h = 1, x1 = pos[0], x2 = pos[0], y1 = pos[1], y2 = pos[1];
        Point2[] pts = new Point2[pos.length / 2];
        for(int i = 0; i < pts.length; i++){
            pts[i] = new Point2(pos[i * 2], pos[i * 2 + 1]);
            x1 = Math.min(x1, pos[i * 2]);
            x2 = Math.max(x2, pos[i * 2]);
            y1 = Math.min(y1, pos[i * 2 + 1]);
            y2 = Math.max(y2, pos[i * 2 + 1]);
        }
        w = x2 - x1 + 1;
        h = y2 - y1 + 1;
        return new PanelDoodadType(pts, Core.atlas.find("unity-doodad-" + name), Core.atlas.find("unity-doodad-" + outlinename), w, h);
    }
}
