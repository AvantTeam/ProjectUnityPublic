package unity.world.graph;

import arc.util.*;
import unity.world.graph.connectors.GraphConnectorTypeI.*;

/** @author Xelo */
public class GraphEdge<T extends GraphI<T>>{
    public GraphConnectorI<T> n1, n2;
    public long id;
    /** Do not modify directly! */
    public boolean valid;

    public GraphEdge(GraphConnectorI<T> n1, GraphConnectorI<T> n2){
        this.n1 = n1;
        this.n2 = n2;
        id = getId(n1, n2);

        if(n1 == n2) throw new IllegalStateException("Vertices cant self-connect.");
        valid = true;
    }

    public <E extends GraphConnectorI<T>> E other(GraphI<T> current){
        return ((n1.graph() == current) ? n2 : n1).as();
    }

    public <E extends GraphConnectorI<T>> E other(GraphConnectorI<T> current){
        return ((n1 == current) ? n2 : n1).as();
    }

    public void setN1(GraphConnectorI<T> current){
        if(current == n2){
            var conn = n1;
            n1 = current;
            n2 = conn;

            n1.sideChanged(this, false);
            n2.sideChanged(this, true);
        }
    }

    public void setN2(GraphConnectorI<T> current){
        if(current == n1){
            var conn = n2;
            n2 = current;
            n1 = conn;

            n1.sideChanged(this, false);
            n2.sideChanged(this, true);
        }
    }

    public static <T extends GraphI<T>> long getId(GraphConnectorI<T> n1, GraphConnectorI<T> n2){
        int id1 = n1.id(), id2 = n2.id();
        return id1 < id2 ? Pack.longInt(id1, id2) : Pack.longInt(id2, id1);
    }

    @Override
    public String toString(){
        return "GraphEdge{" +
        "n1=" + n1 +
        ", n2=" + n2 +
        ", id=" + id +
        '}';
    }
}
