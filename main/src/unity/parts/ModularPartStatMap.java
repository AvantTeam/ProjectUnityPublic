package unity.parts;

import arc.struct.*;
import arc.util.serialization.*;
import unity.ui.*;
import unity.util.*;


public abstract class ModularPartStatMap{
    public ValueMap stats = new ValueMap();
    ModularPart[][] partgrid;

    public ValueMap getOrCreate(String name){
        if(!stats.has(name)){
            stats.put(name,new ValueMap());
        }
        return stats.getValueMap(name);
    }
    public boolean has(String name){
        return stats.has(name);
    }

    public float getValue(String name){
        return stats.getValueMap(name).getFloat("value");
    }
    public float getValue(String name,String subfield){
        return Utils.getFloat(stats.getValueMap(name),subfield,0);
    }

    public void initStat(ModularPart[][] partgrid, Seq<ModularPart> partseq){
        this.partgrid = partgrid;
        calculateStat(partseq);

    }
    public abstract void calculateStat(Seq<ModularPart> partseq);

    public ModularPart[][] partgrid(){
        return partgrid;
    }

    public abstract void drawEditor(PartsEditorElement editor);
}
