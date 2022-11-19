package unity.gensrc.entities;

import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.gen.entities.*;
import unity.mod.*;

@SuppressWarnings("unused")
@EntityComponent
abstract class FactionComp implements Entityc{
    abstract Faction faction();

    boolean sameFaction(Entityc other){
        Faction fac = faction();
        return
            (other instanceof Factionc e && e.faction() == fac) ||
            (other instanceof Building build && FactionRegistry.faction(build.block) == fac);
    }
}
