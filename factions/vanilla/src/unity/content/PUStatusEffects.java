package unity.content;

import mindustry.type.*;
import unity.mod.*;

/**
 * Defines all {@linkplain Faction generic} status effects.
 * @author GlennFolker
 */
public final class PUStatusEffects{
    public static StatusEffect
    disabled;

    private PUStatusEffects(){
        throw new AssertionError();
    }

    public static void load(){
        disabled = new StatusEffect("disabled"){{
            reloadMultiplier = 0f;
            speedMultiplier = 0f;
            disarm = true;
        }};
    }
}
