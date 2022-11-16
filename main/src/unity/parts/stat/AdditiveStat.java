package unity.parts.stat;

import arc.*;
import arc.scene.ui.layout.*;
import unity.parts.*;
import unity.util.*;

import java.lang.reflect.*;

public class AdditiveStat extends ModularPartStat{
    float value  = 0;
    public AdditiveStat(String name, float value){
        super(name);
        this.value = value;
    }


    public void display(Table table){
        String valuestr = ": [accent]"+value;
        if(value%1<=0.001f){
            valuestr = ": [accent]"+(int)value;
        }
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stattype."+name) + valuestr).left().top();
    }

    Field field = null;
    boolean searchedField = false;

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(!searchedField){
            field = ReflectUtils.getField(id,name);
            searchedField = true;
        }
        if(field==null){
            Utils.add(id.getOrCreate(name),"value",value);
        }else{
            try{
                var type = field.getType();
                if( type == float.class ){
                    field.set(id, field.getFloat(id) + value);
                }else if( type == int.class ){
                    field.set(id,  Math.round(field.getInt(id) + value));
                }else if( type == long.class ){
                    field.set(id,  field.getLong(id) + (long)value);
                }else if( type == double.class ){
                    field.set(id,  field.getDouble(id) + value);
                }else{
                    throw new IllegalArgumentException("Type"+type+" is not a number and cannot be used in a AdditiveStat");
                }

            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){

    }
    ///use field info?


    public static class MassStat extends AdditiveStat{
        public MassStat(float power){
            super("mass",power);
        }
    }
    public static class WeaponSlotStat extends AdditiveStat{
        public WeaponSlotStat(float slot){
            super("weaponSlots",slot);
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
