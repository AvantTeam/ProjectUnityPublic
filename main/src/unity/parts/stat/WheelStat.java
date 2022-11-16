package unity.parts.stat;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import unity.parts.*;
import unity.util.*;

import static mindustry.Vars.tilesize;

public class WheelStat extends ModularPartStat{
    float speedPerRps;
    float traction;
    float nominalWeight; //max weight supported until speed penalties
    float steeringAngle;

    public WheelStat(float speedPerRps,float traction, float nominalWeight, float steeringAngle){
        super("wheel");
        this.speedPerRps = speedPerRps;
        this.traction = traction;
        this.nominalWeight = nominalWeight;
        this.steeringAngle = steeringAngle;
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(id instanceof ModularUnitStatMap map){
            map.weightCapacity += nominalWeight;
            float pow = traction * map.rps * speedPerRps;
            map.speedPower += pow;
            map.turningPower += pow * steeringAngle;
            map.tractionTotal += traction;
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){
        if(id instanceof ModularUnitStatMap map){
            map.speed = map.speedPower/map.tractionTotal;
            map.turningspeed = map.turningPower/map.speedPower;
        }
    }

    @Override
    public void display(Table table){
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stattype.weightcap") +": [accent]"+ nominalWeight).left().top();
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stattype.wheelspeed")  +": [accent]"+ Core.bundle.format("ui.parts.stat.speedRatio", Strings.fixed(speedPerRps * 60f / tilesize,1))).left().top();
    }
}
