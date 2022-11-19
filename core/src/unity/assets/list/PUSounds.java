package unity.assets.list;

import arc.audio.*;

import static mindustry.Vars.tree;

/** Lists all {@link Sound}s the mod has to load. */
public final class PUSounds{
    public static Sound
    chainyShot,
    energyCharge, energyBlast, energyBolt;

    private PUSounds(){
        throw new AssertionError();
    }

    public static void load(){
        chainyShot = tree.loadSound("chainy-shot");

        energyCharge = tree.loadSound("energy-charge");
        energyBlast = tree.loadSound("energy-blast");
        energyBolt = tree.loadSound("energy-bolt");
    }
}
