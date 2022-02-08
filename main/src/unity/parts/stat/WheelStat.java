package unity.parts.stat;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import unity.parts.*;
import unity.util.*;

import static mindustry.Vars.tilesize;

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

    @Override
    public void display(Table table){
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stattype.weightcap") +": [accent]"+ nominalWeight).left().top();
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stattype.maxspeed")  +": [accent]"+ Core.bundle.format("ui.parts.stat.speed", Strings.fixed(maxSpeed * 60f / tilesize,1))).left().top();
    }
}
