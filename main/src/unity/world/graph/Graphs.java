package unity.world.graph;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import mindustry.*;

public class Graphs{
    //finally i use this class
    public static ObjectMap<Class<? extends Graph>, TextureRegion> graphIcons = new ObjectMap<>();

    public static void load(){
        if(Vars.headless){
            return;
        }
        graphIcons.put(TorqueGraph.class, Core.atlas.find("unity-graph-torque-icon"));
        graphIcons.put(HeatGraph.class, Core.atlas.find("unity-graph-heat-icon"));
        graphIcons.put(CrucibleGraph.class, Core.atlas.find("unity-graph-crucible-icon"));

    }
}
