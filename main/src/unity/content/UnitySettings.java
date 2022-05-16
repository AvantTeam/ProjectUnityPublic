package unity.content;

import arc.*;
import mindustry.*;

/**
  * Project Unity mod settings
  * @author sk7725
*/
public class UnitySettings{
    public static void addGraphicSetting(String key, boolean def){
        Vars.ui.settings.graphics.checkPref(key, Core.settings.getBool(key, def));
    }

    public static void init(){
        boolean tmp = Core.settings.getBool("uiscalechanged", false);
        Core.settings.put("uiscalechanged", false);

        addGraphicSetting("hitexpeffect", true);

        Core.settings.put("uiscalechanged", tmp);
    }
}
