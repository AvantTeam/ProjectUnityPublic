package unity.content;

import mindustry.type.*;

public final class UnityStatusEffects{
    public static StatusEffect disabled, weaken;

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
    }
}
