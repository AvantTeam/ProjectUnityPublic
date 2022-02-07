package unity.parts.stat;

import arc.math.*;
import arc.util.*;
import unity.parts.*;
import unity.util.*;

public class ArmourStat extends AdditiveStat{
    public ArmourStat(float value){
        super("armour", value);
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){
        ValueMap armor = id.getOrCreate("armour");
        if(armor.has("realValue")){
            return;
        }
        float a  = id.getValue("armour");
        Log.info("armorVal:"+a);
        a = Mathf.log(2, a);
        Log.info("armor:"+a);
        armor.put("realValue",a);
    }
}
