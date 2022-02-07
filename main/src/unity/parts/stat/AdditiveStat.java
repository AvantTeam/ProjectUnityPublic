package unity.parts.stat;

import arc.*;
import arc.scene.ui.layout.*;
import mindustry.world.meta.*;
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

    public void display(Table table){
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stattype."+name) + ": "+value).left().top();
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
