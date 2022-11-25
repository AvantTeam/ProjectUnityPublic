package unity.world.graph;

import unity.annotations.Annotations.*;
import unity.world.graph.nodes.*;

@GraphDef(HeatNodeTypeI.class)
public interface HeatGraphI<T extends HeatGraphI<T>> extends GraphI<T>{
    
}
