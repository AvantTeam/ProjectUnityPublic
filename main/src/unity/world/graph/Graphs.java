package unity.world.graph;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import mindustry.*;

public class Graphs{
    //finally i use this class
    public static ObjectMap<Class<? extends Graph>, GraphInfo> graphInfo = new ObjectMap<>();
    static {
        graphInfo.put(TorqueGraph.class, new GraphInfo(TorqueGraph.class,"torque"));
        graphInfo.put(HeatGraph.class, new GraphInfo(HeatGraph.class,"heat"));
        graphInfo.put(CrucibleGraph.class, new GraphInfo(CrucibleGraph.class,"crucible"));
    }
    public static void load(){
        if(Vars.headless){
            return;
        }
        graphInfo.each((k, v)->{
            v.load();
        });

    }

    public static class GraphInfo{
        TextureRegion icon;
        Class<? extends Graph> clazz;
        String name;

        public GraphInfo(Class<? extends Graph> clazz, String name){
            this.clazz = clazz;
            this.name = name;
        }

        public void load(){
            icon = Core.atlas.find("unity-graph-"+name+"-icon");
        }
    }
}
