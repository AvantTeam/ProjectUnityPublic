package unity.parts.stat;

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

}
