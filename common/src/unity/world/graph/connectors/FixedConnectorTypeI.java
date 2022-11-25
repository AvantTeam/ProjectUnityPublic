package unity.world.graph.connectors;

import unity.world.graph.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

public interface FixedConnectorTypeI<T extends GraphI<T>> extends GraphConnectorTypeI<T>{
    @Override
    FixedConnectorI<T> create(GraphNodeI<T> node);

    int[] connectionIndices();

    public interface FixedConnectorI<T extends GraphI<T>> extends GraphConnectorI<T>{
        int[] connectionIndices();
    }
}
