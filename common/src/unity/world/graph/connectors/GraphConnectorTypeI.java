package unity.world.graph.connectors;

import arc.func.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

/**
 * Define public methods for graph connectors here. Derivatives of graph connectors don't need a common interface, since
 * they're not used by the annotation processor and the concrete classes are most likely to be already known anyway.
 * @author GlennFolker
 */
@GraphConnectorBase
@SuppressWarnings("unchecked")
public interface GraphConnectorTypeI<T extends GraphI<T>>{
    GraphConnectorI<T> create(GraphNodeI<T> node);

    int graphType();

    default void drawConnectionPoint(BuildPlan req, Eachable<BuildPlan> list){}

    default <E extends GraphConnectorTypeI<? extends T>> E as(){
        return (E)this;
    }

    public interface GraphConnectorI<T extends GraphI<T>>{
        int id();

        T graph();
        <E extends GraphNodeI<T>> E node();

        default void update(){}
        
        void recalcPorts();
        void recalcNeighbors();

        boolean isConnected(GraphConnectorI<T> t);
        <E extends Building & GraphBuild> boolean isConnected(E t);

        boolean canConnect(Point2 pt, GraphConnectorI<T> conn);
        GraphEdge<T> tryConnect(Point2 pt, GraphConnectorI<T> conn);

        <E extends GraphConnectorI<T>> void eachConnected(Cons<E> cons);

        void disconnect();

        GraphEdge<T> addEdge(GraphConnectorI<T> ext);
        void removeEdge(GraphEdge<T> ge);

        void connectionChanged();

        default void write(Writes write){}
        default void read(Reads read){}

        default <E extends GraphConnectorI<? extends T>> E as(){
            return (E)this;
        }
    }
}
