package unity.parts.stat;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import unity.parts.*;
import unity.ui.*;

import static unity.parts.ModularPartStat.MergePriorities.UNIT_POWER_CONS;

public class PowerConsumerStat extends ModularPartStat{
    float powerPerRps,maxRps;
    public PowerConsumerStat(float powerPerRps, float maxRps){
        super("power");
        this.powerPerRps = powerPerRps;
        this.maxRps = maxRps;
        mergePriority = UNIT_POWER_CONS;
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(id instanceof ModularUnitStatMap map){
            map.powerUsage += Math.min(map.rps,maxRps) * powerPerRps;
        }
    }
    @Override
    public void display(Table table){
        String powerStr = ": [accent]"+ Strings.fixed(powerPerRps,1);
        String speedStr = "[accent]"+Strings.fixed(maxRps,1);
        table.row();
        table.add("[lightgray]" + Core.bundle.format("ui.parts.stattype.powercons",powerStr)).left().top();
        table.row();
        table.add("[lightgray]" + Core.bundle.format("ui.parts.stattype.powerconsspeed",speedStr)).left().top();
    }
    public void displaySelected(Table table, PartsEditorElement builder, int index){
        if( builder.statmap instanceof ModularUnitStatMap map){
            table.row();
            table.add("[lightgray]" + Core.bundle.format("ui.parts.editor.powercons",Math.min(maxRps,map.rps) * powerPerRps)).left().top();
            table.add("[lightgray]" + Core.bundle.format("ui.parts.editor.speed",Strings.fixed(map.rps/Math.min(maxRps,map.rps) * 100,0))).left().top();
        }
    }
}
