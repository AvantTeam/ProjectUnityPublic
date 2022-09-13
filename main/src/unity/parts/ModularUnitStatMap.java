package unity.parts;

import arc.struct.*;
import arc.util.*;
import unity.util.*;

public class ModularUnitStatMap extends ModularPartStatMap{
    public float health,mass,power,powerUsage,armour,armourPoints,rps,speed,turningspeed;
    public int itemcapacity,abilityslotuse,abilityslots, weaponSlots,weaponslotuse;
    public boolean differentialSteering;

    public ModularUnitStatMap(){
        stats.put("weapons",new ValueList());
        stats.put("abilities",new ValueList());
        stats.put("wheel",(new ValueMap()));
    }


    @Override
    public void calculatStat(ModularPart[][] partgrid, Seq<ModularPart> partseq){
        Log.info("begining stat calc, "+ partseq.size+" parts found");
        for(int i = 0; i < partseq.size; i++){
            partseq.get(i).type.appendStats(this, partseq.get(i), partgrid);
        }
        for(int i = 0; i < partseq.size; i++){
            partseq.get(i).type.appendStatsPost(this, partseq.get(i), partgrid);
        }
    }

    @Override
    public String toString(){
        return "ModularUnitStatMap{" +
        "health=" + health +
        ", mass=" + mass +
        ", power=" + power +
        ", powerUsage=" + powerUsage +
        ", armour=" + armour +
        ", armourPoints=" + armourPoints +
        ", rps=" + rps +
        ", speed=" + speed +
        ", turningspeed=" + turningspeed +
        ", itemcapacity=" + itemcapacity +
        ", abilityslotuse=" + abilityslotuse +
        ", abilityslots=" + abilityslots +
        ", weaponSlots=" + weaponSlots +
        ", weaponslotuse=" + weaponslotuse +
        ", differentialSteering=" + differentialSteering +
        '}';
    }
}
