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
import unity.parts.*;
import unity.parts.types.*;
import unity.content.effects.*;
import unity.type.weapons.*;

public class UnityParts{
    //unit
    //misc
    public static ModularPartType panel, mediumPanel, smallRoot, mediumRoot, largeRoot, largePanel,storage;
    //weapons
    public static ModularPartType gun, cannon, howitzer, pointDefense, tankCannon;
    public static ModularPartType gunBridge;
    //movement
    public static ModularPartType smallEngine, engine, smallWheel, smallTracks, mediumWheel, largeWheel, largeTracks, tankTracks, tankTracksLarge;

    //unit
    public static Seq<PanelDoodadPalette> unitDoodads = new Seq<>();

    public static void load(){
        //region units
        ///DO NOT CHANGE ORDER (will break saves)
        panel = new ModularPartType("panel"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.titanium, 7 ,UnityItems.nickel, 5));
            health(40);
            mass(20);
            armor(1);
        }};
        smallRoot = new ModularPartType("root-small"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 10));
            health(100);
            mass(10);
            producesPower(15);
            addsWeaponSlots(2);
            itemCapacity(20); //the default
            root = true;
            hasCellDecal = true;
        }};
        gun = new ModularWeaponMountType("gun"){{
           requirements(PartCategories.weaponsUnit, ItemStack.with(Items.silicon, 10));
           health(10);
           mass(20);
           usesPower(5);
           weapon(1,new Weapon("unity-part-gun"){{
               rotate = true;
               reload = 18f;
               bullet = Bullets.standardCopper;
           }});
        }};
        cannon = new ModularWeaponMountType("cannon"){{
           requirements(PartCategories.weaponsUnit, ItemStack.with(Items.silicon, 40, Items.titanium, 50, Items.graphite, 20, UnityItems.nickel, 20));
           health(40);
           mass(100);
           usesPower(20);
           w = 2;
           h = 2;
           weapon(2, new Weapon("unity-part-cannon"){{
               rotate = true;
               rotateSpeed = 6f;
               reload = 60f;
               ejectEffect = Fx.casing2;
               shootSound = Sounds.bang;
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
        smallEngine = new ModularPartType("engine-small"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.lead, 10, Items.silicon, 5));
            health(10);
            mass(15);
            producesPower(20);
        }};
        smallWheel = new ModularWheelType("wheel-small"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 5));
            health(15);
            mass(15);
            wheel(1, 30, 1.5f);
            usesPower(7);
        }};
        smallTracks = new ModularWheelType("tracks-small"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 15,UnityItems.nickel, 10));
            w = 1;
            h = 3;
            health(60);
            mass(45);
            wheel(6, 180, 0.6f);
            usesPower(20);
        }};
        howitzer = new ModularWeaponMountType("howitzer"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.silicon, 80, Items.titanium, 120, Items.graphite, 80, UnityItems.nickel, 50));
            health(75);
            mass(550);
            usesPower(80);
            w = 3;
            h = 4;
            weapon(5, new Weapon("unity-part-howitzer"){{
                rotate = true;
                rotateSpeed = 4f;
                reload = 80f;
                ejectEffect = Fx.casing3;
                shots = 2;
                inaccuracy = 6;
                shotDelay = 2f;
                shootSound = Sounds.artillery;
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
                    fragBullets = 3;
                    fragBullet = Bullets.standardCopper;
               }};
            }});
        }};
        mediumPanel = new ModularPartType("medium-panel"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 20, Items.titanium, 50));
            w = 2;
            h = 2;
            health(200);
            mass(120);
            armor(15);
        }};
        gunBridge = new ModularPartType("gun-bridge"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.graphite, 10, UnityItems.nickel, 10));
            usesPower(20);
            mass(20);
            addsWeaponSlots(4);
            hasCellDecal = true;
        }};
        engine = new ModularPartType("engine"){{
            requirements(PartCategories.movementUnit, ItemStack.with(UnityItems.cupronickel, 90, Items.lead, 50, Items.silicon, 30));
            w = 3;
            h = 3;
            health(90);
            mass(500);
            producesPower(400);
        }};
        mediumRoot = new ModularPartType("root-medium"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 25, UnityItems.cupronickel, 15));
            w = 2;
            h = 2;
            health(250);
            mass(40);
            producesPower(50);
            addsWeaponSlots(6);
            itemCapacity(50);
            root = true;
            hasCellDecal = true;
        }};
        largeRoot = new ModularPartType("root-large"){{
            requirements(PartCategories.miscUnit,ItemStack.with(Items.silicon, 75, Items.titanium, 50, UnityItems.nickel, 30, UnityItems.superAlloy, 15));
            w = 3;
            h = 3;
            health(500);
            armor(2);
            mass(150);
            producesPower(100);
            addsWeaponSlots(10);
            itemCapacity(100);
            root = true;
            hasCellDecal = true;
        }};
        largePanel = new ModularPartType("large-panel"){{
            requirements(PartCategories.miscUnit, ItemStack.with(Items.silicon, 50, Items.titanium, 100, UnityItems.nickel, 50));
            w = 3;
            h = 3;
            health(600);
            mass(200);
            armor(1000);
        }};
        mediumWheel = new ModularWheelType("wheel-medium"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 50, Items.titanium, 25));
            w = 2;
            h = 4;
            health(75);
            mass(100);
            wheel(6,180,1.3f);
            usesPower(50);
        }};
        largeWheel = new ModularWheelType("wheel-large"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 400, Items.titanium, 150, Items.thorium, 90, UnityItems.cupronickel, 100));
            w = 3;
            h = 8;
            health(150);
            mass(200);
            wheel(15, 650, 1f);
            usesPower(250);
        }};
        pointDefense = new ModularWeaponMountType("point-defense"){{
            requirements(PartCategories.weaponsUnit, ItemStack.with(Items.silicon, 60, UnityItems.nickel, 30));
            w = 2;
            h = 3;
            health(60);
            mass(60);
            usesPower(15);
            weapon(3, new PointDefenseWeapon("unity-part-point-defense"){{
                rotate = true;
                reload = 18f;
                targetInterval = 0f;
                targetSwitchInterval = 0f;
                shootSound = Sounds.lasershoot;
                bullet = new BulletType(){{
                    smokeEffect = Fx.pointHit;
                    hitEffect = Fx.pointHit;
                    maxRange = 120f;
                    damage = 9f;
                    speed = 3f;
                }};
            }});
        }};
        largeTracks = new ModularWheelType("tracks-large"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 50, Items.thorium, 40, UnityItems.nickel, 30));
            w = 2;
            h = 9;
            health(180);
            mass(200);
            wheel(30, 950, 0.6f);
            usesPower(60);
        }};
        tankTracks = new ModularWheelType("tank-tracks"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 250, Items.titanium, 100, UnityItems.nickel, 100, UnityItems.cupronickel, 50));
            w = 3;
            h = 16;
            health(210);
            mass(250);
            wheel(60, 2000, 0.6f);
            usesPower(200);
        }};
        tankTracksLarge = new ModularWheelType("tank-tracks-large"){{
            requirements(PartCategories.movementUnit, ItemStack.with(Items.silicon, 700, Items.titanium, 500, UnityItems.nickel, 300, UnityItems.cupronickel, 200, UnityItems.superAlloy, 100));
            w = 5;
            h = 30;
            health(500);
            mass(500);
            wheel(300, 10000, 0.6f);
            usesPower(1200);
        }};
        tankCannon = new ModularWeaponMountType("arbiter"){{
           requirements(PartCategories.weaponsUnit,ItemStack.with(Items.silicon, 500, Items.titanium, 500, UnityItems.superAlloy, 200, UnityItems.nickel, 300, UnityItems.cupronickel, 150));
           health(750);
           mass(1800);
           usesPower(400);
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
                   pierceCap = 8;
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
            requirements(PartCategories.miscUnit,ItemStack.with(Items.silicon,75,Items.titanium,50,UnityItems.nickel,30,Items.thorium,15));
            w = 2;
            h = 2;
            itemCapacity(20);
            health(50);
            mass(150);
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
            pts[i] = new Point2(pos[i * 2] ,pos[i * 2 + 1]);
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
