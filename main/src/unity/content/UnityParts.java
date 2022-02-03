package unity.content;

import mindustry.content.*;
import mindustry.type.*;
import unity.parts.*;
import unity.parts.types.*;

public class UnityParts{
    public static ModularPartType panel,testroot,testgun;

    public static void load(){
        //region units
        panel = new ModularPartType("panel"){{
            requirements(PartCategories.miscUnit,ItemStack.with(Items.lead,10));
            health(100);
        }};
        testroot = new ModularPartType("testroot"){{
            requirements(PartCategories.miscUnit,ItemStack.with(Items.silicon,10));
            health(1000);
            root = true;
        }};
        testgun = new ModularWeaponMountType("testgun"){{
           requirements(PartCategories.miscUnit,ItemStack.with(Items.silicon,10));
           health(10);
           weapon(new Weapon("mount-weapon"){{
               rotate = true;
               reload = 13f;
               //insert bullet
           }});
        }};
        //endregion




    }
}
