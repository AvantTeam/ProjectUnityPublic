package unity.world.graph.connectors;

import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

@GraphConnectorBase
public interface GraphConnectorTypeI<T extends GraphI<T>>{
    GraphConnectorI<T> create(GraphNodeI<T> node);

    int graphType();

    public interface GraphConnectorI<T extends GraphI<T>>{
        T graph();
    }
}
