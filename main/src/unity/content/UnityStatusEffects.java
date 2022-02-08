package unity.content;

import arc.graphics.*;
import mindustry.type.*;

public final class UnityStatusEffects{
    public static StatusEffect disabled, weaken, speedFatigue, sagittariusFatigue;

    private UnityStatusEffects(){
        throw new AssertionError();
    }

    public static void load(){
        disabled = new StatusEffect("disabled"){{
            reloadMultiplier = 0f;
            speedMultiplier = 0f;
            disarm = true;
        }};

        weaken = new StatusEffect("weaken"){{
            damageMultiplier = 0.75f;
            healthMultiplier = 0.75f;
            speedMultiplier = 0.5f;
        }};

        speedFatigue = new StatusEffect("speed-fatigue"){{
            speedMultiplier = 0.6f;
        }};

        sagittariusFatigue = new StatusEffect("sagittarius-fatigue"){{
            speedMultiplier = 0.1f;
            healthMultiplier = 0.6f;
            Color.valueOf(color, "62ae7f");
        }};
    }
}
