package unity.content;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import unity.parts.*;
import unity.parts.types.*;

public class UnityParts{
    public static ModularPartType panel, smallRoot, gun,cannon,smallEngine,smallWheel,smallTracks;

    //unit
    public static Seq<PanelDoodadType> unitdoodads1x1 = new Seq<>();
    public static Seq<PanelDoodadType> unitdoodads2x2 = new Seq<>();

    public static void load(){
        //region units
        panel = new ModularPartType("panel"){{
            requirements(PartCategories.miscUnit,ItemStack.with(Items.titanium,10,UnityItems.nickel,10));
            health(50);
            mass(20);
            armor(1);
        }};
        smallRoot = new ModularPartType("root-small"){{
            requirements(PartCategories.miscUnit,ItemStack.with(Items.silicon,10));
            health(100);
            mass(10);
            producesPower(15);
            root = true;
        }};
        gun = new ModularWeaponMountType("gun"){{
           requirements(PartCategories.weaponsUnit,ItemStack.with(Items.silicon,10));
           health(10);
           mass(20);
           usesPower(5);
           weapon(new Weapon("mount-weapon"){{
               rotate = true;
               reload = 18f;
               bullet = Bullets.standardCopper;
               //insert bullet
           }});

        }};
        cannon = new ModularWeaponMountType("cannon"){{
           requirements(PartCategories.weaponsUnit,ItemStack.with(Items.silicon,50,Items.titanium,70,Items.graphite,20,UnityItems.nickel,20));
           health(40);
           mass(100);
           usesPower(20);
           w = 2;
           h = 2;
           weapon(new Weapon("unity-part-cannon"){{
               rotate = true;
               reload = 40f;
               ejectEffect = Fx.casing2;
               shootSound = Sounds.artillery;
               bullet = new BasicBulletType(7f, 40){{
                   width = 11f;
                   height = 20f;
                   lifetime = 27f;
                   shootEffect = Fx.shootBig;
                   splashDamage = 20;
                   splashDamageRadius = 25;
                   hitEffect = Fx.blastExplosion;
               }};
               //insert bullet
           }});

        }};
        smallEngine = new ModularPartType("engine-small"){{
            requirements(PartCategories.movementUnit,ItemStack.with(Items.lead,10,Items.silicon,5));
            health(10);
            mass(15);
            producesPower(20);
        }};
        smallWheel = new ModularWheelType("wheel-small"){{
            requirements(PartCategories.movementUnit,ItemStack.with(Items.silicon,5));
            health(15);
            mass(15);
            wheel(1,30,1.5f);
            usesPower(7);
        }};
        smallTracks = new ModularWheelType("tracks-small"){{
            requirements(PartCategories.movementUnit,ItemStack.with(Items.silicon,15,UnityItems.nickel,10));
            w = 1;
            h = 3;
            health(60);
            mass(45);
            wheel(6,180,0.7f);
            usesPower(20);
        }};
        //endregion
    }

    public static void loadDoodads(){
        for(int i = 0;i<12;i++){
            unitdoodads1x1.add(getUnitDoodad("1x1-"+(i+1), "1x1-outline-"+(i+1), 0 , 0));
        }
        for(int i = 0;i<5;i++){
            unitdoodads2x2.add(getUnitDoodad("2x2-"+(i+1), "2x2-outline-"+(i+1), 0 ,0,   1, 1,   0, 1,   1, 0));
        }
    }

    static PanelDoodadType getUnitDoodad(String name, String outlinename, int... pos){
        int w = 1,h = 1,x1=pos[0],x2=pos[0],y1=pos[1],y2=pos[1];
        Point2[] pts = new Point2[pos.length/2];
        for(int i = 0;i<pts.length;i++){
            pts[i] = new Point2(pos[i*2] ,pos[i*2+1]);
            x1 = Math.min(x1,pos[i*2]);
            x2 = Math.max(x2,pos[i*2]);
            y1 = Math.min(y1,pos[i*2+1]);
            y2 = Math.max(y2,pos[i*2+1]);
        }
        w = x2-x1+1;
        h = y2-y1+1;
        return new PanelDoodadType(pts,Core.atlas.find("unity-doodad-"+name),Core.atlas.find("unity-doodad-"+outlinename),w,h);
    }
}
