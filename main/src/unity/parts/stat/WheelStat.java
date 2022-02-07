package unity.parts.stat;

import unity.parts.*;
import unity.util.*;

public class WheelStat extends ModularPartStat{
    float wheelStrength;
    float nominalWeight; //max weight supported until speed penalties
    float maxSpeed;

    public WheelStat(float wheelStrength, float nominalWeight, float maxSpeed){
        super("wheel");
        this.wheelStrength = wheelStrength;
        this.nominalWeight = nominalWeight;
        this.maxSpeed = maxSpeed;
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(id.has("wheel")){
            ValueMap wheelstat = id.getOrCreate("wheel");
            Utils.add(wheelstat,"total strength",wheelStrength);
            Utils.add(wheelstat,"total speedpower",wheelStrength*maxSpeed);
            Utils.add(wheelstat,"weight capacity", nominalWeight);
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){
        if(id.has("wheel")){
            ValueMap wheelstat = id.getOrCreate("wheel");
            if(wheelstat.has("nominal speed")){
                return;
            }
            wheelstat.put("nominal speed", wheelstat.getFloat("total speedpower")/wheelstat.getFloat("total strength"));
        }
    }
}
