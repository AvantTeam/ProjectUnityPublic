package unity.parts.types;

import mindustry.entities.abilities.*;
import unity.parts.*;
import unity.parts.stat.*;
import unity.parts.stat.AdditiveStat.*;

public class ModularUnitAbilityType extends ModularPartType{
    public ModularUnitAbilityType(String name){
        super(name);
    }
    public void ability(int abilityslots, Ability ability){
        stats.add(new AbilitySlotUseStat(abilityslots));
        stats.add(new AbilityStat(ability));
    }
}
