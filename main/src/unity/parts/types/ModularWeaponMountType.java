package unity.parts.types;

import mindustry.type.*;
import unity.parts.*;
import unity.parts.stat.*;

public class ModularWeaponMountType extends ModularPartType{
    public ModularWeaponMountType(String name){
        super(name);
    }

    public void weapon(Weapon weapon){
        stats.add(new WeaponMountStat(weapon));
    }

    @Override
    public void appendStats(ModularPartStatMap statmap, ModularPart part, ModularPart[][] grid){
        super.appendStats(statmap, part, grid);
    }


}
