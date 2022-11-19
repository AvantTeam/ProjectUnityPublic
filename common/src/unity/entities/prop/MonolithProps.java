package unity.entities.prop;

import mindustry.content.*;
import mindustry.type.*;
import unity.entities.type.PUUnitTypeCommon.*;
import unity.mod.*;

/**
 * Unit properties around the {@linkplain Faction#monolith monolith} faction.
 * @author GlennFolker
 */
public class MonolithProps extends Props{
    public int maxSouls, startupSouls = 1;
    public StatusEffect soulLackStatus = StatusEffects.none;
}
