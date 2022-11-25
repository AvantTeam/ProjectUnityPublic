package unity.world.graph.connectors;

import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

@GraphConnectorBase
public interface GraphConnectorTypeI<T extends GraphI>{
    GraphConnectorI<T> create(GraphNodeI<T> node);

    long graphType();

    public interface GraphConnectorI<T extends GraphI>{
        T graph();
    }
}
