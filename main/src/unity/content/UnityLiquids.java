package unity.content;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.type.*;
import unity.graphics.*;

public class UnityLiquids {
    private static final Color temp = new Color(0f, 0f, 0f, 1f), temp2 = temp.cpy();
    public static Liquid lava;

    public static void load(){
        lava = new Liquid("lava", UnityPal.lava){{
            heatCapacity = 0f;
            viscosity = 0.7f;
            temperature = 1.5f;
            effect = UnityStatusEffects.molten;
            lightColor = UnityPal.lava2.cpy().mul(1f, 1f, 1f, 0.55f);
        }};

        //endregion
        if(!mindustry.Vars.headless){
            Events.run(EventType.Trigger.update, () -> {
                lava.color = temp.set(UnityPal.lava).lerp(UnityPal.lava2, Mathf.absin(Time.globalTime, 25f, 1f));
                lava.lightColor = temp2.set(temp).mul(1, 1, 1, 0.55f);
            });
        }
    }
}
