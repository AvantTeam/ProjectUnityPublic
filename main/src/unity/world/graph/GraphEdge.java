package unity.world.graph;

import arc.util.*;

public class GraphEdge{
    public GraphConnector n1,n2;
    public long id;
    boolean valid;//removednt
    public GraphEdge(GraphConnector n1, GraphConnector n2){
        this.n1 = n1;
        this.n2 = n2;
        id = getId(n1,n2);
        if(n1==n2){
            throw new IllegalStateException("vertexes cant self connect");
        }
        valid = true;
    }

    public GraphConnector other(Graph current){
        return (n1.getGraph() == current)?n2:n1;
    }
    public GraphConnector other(GraphConnector current){
            return (n1 == current)?n2:n1;
        }

    public static long getId(GraphConnector n1, GraphConnector n2){
        return n1.id<n2.id? Pack.longInt(n1.id,n2.id) : Pack.longInt(n2.id,n1.id);
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
