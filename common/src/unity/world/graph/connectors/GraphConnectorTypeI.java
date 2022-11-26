package unity.world.graph.connectors;

import arc.func.*;
import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

@GraphConnectorBase
public interface GraphConnectorTypeI<T extends GraphI<T>>{
    GraphConnectorI<T> create(GraphNodeI<T> node);

    int graphType();

    public interface GraphConnectorI<T extends GraphI<T>>{
        T graph();

        boolean isConnected(GraphConnectorI<T> t);
        <E extends Building & GraphBuild> boolean isConnected(E t);

        <E extends GraphConnectorI<T>> void eachConnected(Cons<E> cons);

        void disconnect();
    }
}
