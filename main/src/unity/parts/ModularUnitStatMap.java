package unity.parts;

import unity.util.*;

public class ModularUnitStatMap extends ModularPartStatMap{
    public ModularUnitStatMap(){
        stats.put("health",(new ValueMap()).put("value",0f));
        stats.put("mass",(new ValueMap()).put("value",0f));
        stats.put("power",(new ValueMap()).put("value",0f));
        stats.put("powerusage",(new ValueMap()).put("value",0f));
        stats.put("speed",(new ValueMap()).put("value",0f));
        stats.put("armour",(new ValueMap()).put("value",0f));
        stats.put("weaponslots",(new ValueMap()).put("value",0f));
        stats.put("weaponslotuse",(new ValueMap()).put("value",0f));
        stats.put("abilityslots",(new ValueMap()).put("value",0f));
        stats.put("abilityslotuse",(new ValueMap()).put("value",0f));
        stats.put("itemcapacity",(new ValueMap()).put("value",0f));
        stats.put("weapons",new ValueList());
        stats.put("abilities",new ValueList());
        stats.put("wheel",(new ValueMap()));
    }
}
