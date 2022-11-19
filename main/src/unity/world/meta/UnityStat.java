package unity.world.meta;

import mindustry.world.meta.*;

public class UnityStat{
    public static final Stat

    friction = new Stat("unity-friction", UnityStatCat.torque),
    inertia = new Stat("unity-inertia", UnityStatCat.torque),
    maxSpeed = new Stat("unity-maxspeed", UnityStatCat.torque),
    maxTorque = new Stat("unity-maxtorque", UnityStatCat.torque),

    emissivity = new Stat("unity-emissiveness", UnityStatCat.heat),
    heatCapacity = new Stat("unity-heatcapacity", UnityStatCat.heat),
    heatConductivity = new Stat("unity-heatconductivity", UnityStatCat.heat),
    maxTemperature = new Stat("unity-maxtemp", UnityStatCat.heat),

    crucibleCapacity = new Stat("unity-cruciblecapacity", UnityStatCat.crucible),
    crucibleMeltingPoints = new Stat("unity-cruciblemelts", UnityStatCat.crucible);
}
