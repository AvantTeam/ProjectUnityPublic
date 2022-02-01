package unity.parts.stat;

import unity.parts.*;

public class HealthStat extends ModularPartStat{
    public float hpboost = 0;
    public boolean percentage = false;

    public HealthStat(float flat){
        super("health");
        hpboost = flat;
    }

    public HealthStat(float flat, boolean boost){
        super("health");
        percentage = boost;
        hpboost = flat;
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(!percentage && id.has(name)){
            var jo = id.getOrCreate(name);
            jo.put("value", jo.getFloat("value") + hpboost);
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){
        if(percentage && id.has(name)){
            var jo = id.getOrCreate(name);
            jo.put("value", jo.getFloat("value") * hpboost);
        }
    }
}
