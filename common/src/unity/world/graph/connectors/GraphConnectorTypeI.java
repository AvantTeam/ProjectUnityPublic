package unity.world.graph.connectors;

import arc.func.*;
import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

/**
 * Define public methods for graph connectors here.
 * @author GlennFolker
 */
@GraphConnectorBase
@SuppressWarnings("unchecked")
public interface GraphConnectorTypeI<T extends GraphI<T>>{
    GraphConnectorI<T> create(GraphNodeI<T> node);

    int graphType();

    default <E extends GraphConnectorTypeI<T>> E as(){
        return (E)this;
    }

    public interface GraphConnectorI<T extends GraphI<T>>{
        int id();

        T graph();
        <E extends GraphNodeI<T>> E node();

        boolean isConnected(GraphConnectorI<T> t);
        <E extends Building & GraphBuild> boolean isConnected(E t);

        <E extends GraphConnectorI<T>> void eachConnected(Cons<E> cons);

        void disconnect();
        void removeEdge(GraphEdge<T> ge);

        default <E extends GraphConnectorI<T>> E as(){
            return (E)this;
        }
    }
}
