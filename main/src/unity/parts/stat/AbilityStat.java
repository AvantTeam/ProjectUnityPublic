package unity.parts.stat;

import mindustry.entities.abilities.*;
import mindustry.type.*;
import unity.parts.*;
import unity.util.*;

public class AbilityStat extends ModularPartStat{
    public Ability ability;
    public AbilityStat(Ability ability){
        super("abilities");
        this.ability=ability;
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(id.has("abilities")){
            var weaponsarr = id.stats.getList("abilities");
            ValueMap abilityMap = new ValueMap();
            abilityMap.put("part", part);
            Ability copy = ability.copy();
            abilityMap.put("ability", copy);
            weaponsarr.add(abilityMap);
        }
    }

}
