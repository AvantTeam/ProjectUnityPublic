package unity.parts;

import arc.struct.*;
import arc.util.serialization.*;
import org.json.*;
import unity.util.*;


public class ModularPartStatMap{
    public JSONObject stats = new JSONObject();

    public JSONObject getOrCreate(String name){
        if(stats.get(name)==null){
            stats.put(name,new JSONObject());
        }
        return stats.getJSONObject(name);
    }
    public boolean has(String name){
        return stats.has(name);
    }

    public float getValue(String name){
        return stats.getJSONObject(name).getFloat("value");
    }
    public float getValue(String name,String subfield){
        return Utils.getFloat(stats.getJSONObject(name),subfield,0);
    }

}
