package unity.parts;

import arc.struct.*;
import arc.util.serialization.*;
import org.json.*;


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

}
