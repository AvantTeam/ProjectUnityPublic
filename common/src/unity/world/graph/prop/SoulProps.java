package unity.world.graph.prop;

import arc.graphics.g2d.*;
import mindustry.entities.*;
import mindustry.gen.*;
import unity.world.graph.GraphBlock.*;

/** @author GlennFolker */
public class SoulProps{
    public void init(){}
    public void load(){}

    public SoulPropsEntity create(GraphBuild build){
        return new SoulPropsEntity(build);
    }

    public class SoulPropsEntity{
        public GraphBuild build;

        public SoulPropsEntity(GraphBuild build){
            this.build = build;
        }

        public void update(){}
        public void draw(){}

        public <E extends Building & GraphBuild> E build(){
            return build.as();
        }
    }
}
