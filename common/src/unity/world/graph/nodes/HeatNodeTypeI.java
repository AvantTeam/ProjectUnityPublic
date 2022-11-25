package unity.world.graph.nodes;

import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;

@GraphNodeDef
public interface HeatNodeTypeI<T extends HeatGraphI> extends GraphNodeTypeI<T>{
    @Override
    <E extends Building & GraphBuild> HeatNodeI<T> create(E build);

    public interface HeatNodeI<T extends HeatGraphI> extends GraphNodeI<T>{

    }
}
