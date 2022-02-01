package unity.parts;

import org.json.*;

public class ModularUnitStatMap extends ModularPartStatMap{
    public ModularUnitStatMap(){
        stats.put("health",(new JSONObject()).put("value",0f));
        stats.put("speed",(new JSONObject()).put("value",0f));
        stats.put("armour",(new JSONObject()).put("value",0f));
        stats.put("weapons",new JSONArray());
    }
}
