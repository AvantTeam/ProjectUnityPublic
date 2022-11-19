package unity.content;

import mindustry.type.*;
import unity.content.type.*;
import unity.mod.*;

import static unity.graphics.MonolithPal.*;
import static unity.mod.FactionRegistry.register;

/**
 * Defines all {@linkplain Faction#monolith monolith} liquids and gasses.
 * @author GlennFolker
 */
public final class MonolithFluids{
    public static Liquid
    eneraphyte;

    private MonolithFluids(){
        throw new AssertionError();
    }

    public static void load(){
        eneraphyte = register(Faction.monolith, new PUFluid("eneraphyte", monolithLighter){{
            gas = true;
            temperature = 0.7f;
            heatCapacity = 0.1f;
            viscosity = 0.2f;

            effect = MonolithStatusEffects.eneraphyteSupercharge;
            vaporEffect = MonolithFx.eneraphyteVapor;
            barColor = monolithLight.cpy();
        }});
    }
}
