package unity.parts.stat;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import unity.parts.*;

import static unity.parts.ModularPartStat.MergePriorities.*;

public class EngineStat extends ModularPartStat{
    float speed,power;
    public EngineStat(float power, float speed){
        super("power");
        mergePriority = UNIT_ENGINE;
        mergePostPriority = UNIT_ENGINE_POST;
        this.speed = speed;
        this.power = power;
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        float speedMul = 1;
        //check for surrounding gearbox.
        float powerActual = power/speedMul;
        float speedActual = speed * speedMul;
        if(id instanceof ModularUnitStatMap map){
            map.powerRpsMul += powerActual*speedActual;
            map.power += powerActual;
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){
        if(id instanceof ModularUnitStatMap map){
            map.rps = map.powerRpsMul/map.power;
            Log.info("rps: "+map.rps+" | "+map.powerRpsMul+","+map.power);
        }
    }

    @Override
    public void display(Table table){
        String powerStr = ": [accent]"+Strings.fixed(power,1);
        String speedStr = "[accent]"+Strings.fixed(speed,1);
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stattype.power") + powerStr).left().top();
        table.row();
        table.add("[lightgray]" + Core.bundle.format("ui.parts.stattype.enginespeed",speedStr)).left().top();
    }
}
