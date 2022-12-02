package unity.content;

import mindustry.type.*;
import unity.content.type.*;
import unity.mod.*;

import static unity.graphics.MonolithPal.*;
import static unity.mod.FactionRegistry.*;

/**
 * Defines all {@linkplain Faction#monolith monolith} status effect types.
 * @author GlennFolker
 */
public final class MonolithStatusEffects{
    public static StatusEffect
    eneraphyteSupercharge;

    private MonolithStatusEffects(){
        throw new AssertionError();
    }

    public static void load(){
        eneraphyteSupercharge = register(Faction.monolith, new PUStatusEffect("eneraphyte-supercharge", monolithLighter));
    }
}
