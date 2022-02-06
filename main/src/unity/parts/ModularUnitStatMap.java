package unity.parts;

import org.json.*;

public class ModularUnitStatMap extends ModularPartStatMap{
    public ModularUnitStatMap(){
        stats.put("health",(new JSONObject()).put("value",0f));
        stats.put("mass",(new JSONObject()).put("value",0f));
        stats.put("power",(new JSONObject()).put("value",0f));
        stats.put("powerusage",(new JSONObject()).put("value",0f));
        stats.put("speed",(new JSONObject()).put("value",0f));
        stats.put("armour",(new JSONObject()).put("value",0f));
        stats.put("weapons",new JSONArray());
        stats.put("wheel",(new JSONObject()));
    }
}
