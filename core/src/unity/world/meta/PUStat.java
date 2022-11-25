package unity.world.meta;

import mindustry.world.meta.*;

public class PUStat{
    public static final Stat

    friction = new Stat("unity-friction", PUStatCat.torque),
    inertia = new Stat("unity-inertia", PUStatCat.torque),
    maxSpeed = new Stat("unity-maxspeed", PUStatCat.torque),
    maxTorque = new Stat("unity-maxtorque", PUStatCat.torque),

    emissivity = new Stat("unity-emissiveness", PUStatCat.heat),
    heatCapacity = new Stat("unity-heatcapacity", PUStatCat.heat),
    heatConductivity = new Stat("unity-heatconductivity", PUStatCat.heat),
    maxTemperature = new Stat("unity-maxtemp", PUStatCat.heat),

    crucibleCapacity = new Stat("unity-cruciblecapacity", PUStatCat.crucible),
    crucibleMeltingPoints = new Stat("unity-cruciblemelts", PUStatCat.crucible);
}
