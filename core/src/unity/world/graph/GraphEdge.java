package unity.world.graph;

import arc.util.*;
import unity.world.graph.connectors.GraphConnectorType.*;

public class GraphEdge<T extends Graph<T>>{
    public GraphConnector<T> n1, n2;
    public long id;
    /** Do not modify directly! */
    public boolean valid;

    public GraphEdge(GraphConnector<T> n1, GraphConnector<T> n2){
        this.n1 = n1;
        this.n2 = n2;
        id = getId(n1,n2);

        if(n1 == n2) throw new IllegalStateException("vertexes cant self connect");
        valid = true;
    }

    public GraphConnector<T> other(Graph<T> current){
        return (n1.graph() == current) ? n2 : n1;
    }

    public GraphConnector<T> other(GraphConnector<T> current){
        return (n1 == current) ? n2 : n1;
    }

    public static <T extends Graph<T>> long getId(GraphConnector<T> n1, GraphConnector<T> n2){
        return n1.id < n2.id ? Pack.longInt(n1.id, n2.id) : Pack.longInt(n2.id, n1.id);
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
