package unity.parts.stat;

import arc.math.*;
import arc.util.*;
import unity.parts.*;
import unity.util.*;

public class ArmourStat extends AdditiveStat{
    public ArmourStat(float value){
        super("armourPoints", value);
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){
        if(id instanceof ModularUnitStatMap unit){
            unit.armour =  Mathf.log(2, unit.armourPoints);
        }
    }
}
