package unity.content;

import mindustry.world.meta.*;
import unity.mod.*;

import static mindustry.world.meta.Attribute.*;
import static unity.mod.FactionRegistry.*;

/**
 * Defines all {@linkplain Faction#monolith monolith} environmental block attributes.
 * @author GlennFolker
 */
public final class MonolithAttributes{
    /** Eneraphyte infusion in floors, usually marked in dark blue strands or shards. */
    public static Attribute
    eneraphyteInfusion,
    /** Eneraphyte emission in vent-like cavities. */
    eneraphyteEmission;

    private MonolithAttributes(){
        throw new AssertionError();
    }

    public static void load(){
        eneraphyteInfusion = register(Faction.monolith, add("unity-eneraphyte-infusion"));
        eneraphyteEmission = register(Faction.monolith, add("unity-eneraphyte-emission"));
    }
}
