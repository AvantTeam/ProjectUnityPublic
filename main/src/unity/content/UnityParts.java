package unity.content;

import mindustry.type.*;
import unity.parts.*;
import unity.parts.types.*;

public class UnityParts{
    public static ModularPartType panel,testroot,testgun;

    public static void load(){
        panel = new ModularPartType("panel"){{
            health(100);
        }};
        testroot = new ModularPartType("testroot"){{
           health(1000);
           root = true;
        }};
        testgun = new ModularWeaponMountType("testgun"){{
           health(10);
           weapon(new Weapon("mount-weapon"){{
               rotate = true;
               reload = 13f;
               //insert bullet
           }});
        }};




    }
}
