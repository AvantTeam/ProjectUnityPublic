package unity.parts.stat;

import unity.parts.*;

public class DifferentialSteerStat extends ModularPartStat{
    public DifferentialSteerStat(){
        super("differentialSteer");
        mergePostPriority = MergePriorities.DEFAULT_POST + 1;
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(id instanceof ModularUnitStatMap map){
            map.differentialSteering = true; // todo maybe make it better then just a toggle? Perhaps levels of steering idk.
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){
        if(id instanceof ModularUnitStatMap map){
            map.speed *= 0.5; // you thought it was all positives? ha!
        }
    }
}
