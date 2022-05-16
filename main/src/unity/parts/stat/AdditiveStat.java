package unity.parts.stat;

import arc.*;
import arc.scene.ui.layout.*;
import mindustry.world.meta.*;
import unity.parts.*;

public class AdditiveStat extends ModularPartStat{
    float value  = 0;
    public AdditiveStat(String name, float value){
        super(name);
        this.value = value;
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(id.has(name)){
            var jo = id.getOrCreate(name);
            jo.put("value", jo.getFloat("value") + value);
        }

    }

    public void display(Table table){
        String valuestr = ": [accent]"+value;
        if(value%1<=0.001f){
            valuestr = ": [accent]"+(int)value;
        }
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stattype."+name) + valuestr).left().top();
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){

    }

    public static class PowerUsedStat extends AdditiveStat{
        public PowerUsedStat(float power){
            super("powerusage",power);
        }
    }
    public static class EngineStat extends AdditiveStat{
        public EngineStat(float power){
            super("power",power);
        }
    }
    public static class MassStat extends AdditiveStat{
        public MassStat(float power){
            super("mass",power);
        }
    }
    public static class WeaponSlotStat extends AdditiveStat{
        public WeaponSlotStat(float slot){
            super("weaponslots",slot);
        }
    }
    public static class WeaponSlotUseStat extends AdditiveStat{
        public WeaponSlotUseStat(float slot){
            super("weaponslotuse",slot);
        }
    }
    public static class AbilitySlotStat extends AdditiveStat{
       public AbilitySlotStat(float slot){
           super("abilityslots",slot);
       }
    }
   public static class AbilitySlotUseStat extends AdditiveStat{
       public AbilitySlotUseStat(float slot){
           super("abilityslotuse",slot);
       }
   }
    public static class ItemCapacityStat extends AdditiveStat{
        public ItemCapacityStat(float slot){
            super("itemcapacity",slot);
        }
    }

}
