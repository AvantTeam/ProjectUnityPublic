package unity.world.graph.connectors;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.GraphNodeType.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

@SuppressWarnings("unchecked")
public abstract class GraphConnectorType<T extends Graph<T>> implements GraphConnectorTypeI<T>{
    public final Prov<T> newGraph;
    public final int graphType;

    protected GraphConnectorType(Prov<T> newGraph){
        this.newGraph = newGraph;
        graphType = newGraph.get().type();
    }

    @Override
    public abstract GraphConnector<T> create(GraphNodeI<T> node);

    @Override
    public int graphType(){
        return graphType;
    }

    public static abstract class GraphConnector<T extends Graph<T>> implements GraphConnectorI<T>{
        public final int id;
        private static int lastId;

        public T graph;
        public GraphNode<T> node;
        public OrderedSet<GraphEdge<T>> connections = new OrderedSet<>();

        public boolean disconnectWhenRotate = true;

        {
            id = lastId++;
        }

        protected GraphConnector(GraphNode<T> node, T graph){
            this.node = node;
            graph.addVertex(this);
        }

        @Override
        public T graph(){
            return graph;
        }

        public void update(){}
        public void onProximityUpdate(){}

        public abstract void recalcPorts();
        public abstract void recalcNeighbours();

        public abstract boolean canConnect(Point2 pt, GraphConnector<T> conn);
        public abstract GraphEdge<T> tryConnect(Point2 pt, GraphConnector<T> conn);

        @Override
        public boolean isConnected(GraphConnectorI<T> t){
            for(GraphEdge<T> edge : connections){
                if(edge.other(this) == t) return true;
            }

            return false;
        }

        @Override
        public <E extends Building & GraphBuild> boolean isConnected(E t){
            for(GraphEdge<T> edge : connections){
                if(edge.other(this).node.build == t){
                    return true;
                }
            }
            return false;
        }

        @Override
        public <E extends GraphConnectorI<T>> void eachConnected(Cons<E> cons){
            for(GraphEdge<T> edge : connections){
                cons.get((E)edge.other(this));
            }
        }

        @Override
        public void disconnect(){
            graph.removeVertex(this);
            if(connections.size > 0){
                Log.info("[scarlet] disconnected vertex still has edges!");
            }
        }

        public void removeEdge(GraphEdge<T> ge){
            if(connections.remove(ge)){
                ge.valid = false;
                triggerConnectionChanged();
            }
        }

        public void triggerConnectionChanged(){
            this.node.build.onConnectionChanged(this);
        }

        public void write(Writes write){}
        public void read(Reads read){}

        public GraphEdge<T> addEdge(GraphConnector extconn){
            long edgeid = GraphEdge.getId(this, extconn);
            if(graph.edges.containsKey(edgeid)){
               if(!connections.contains((GraphEdge<T>)graph.edges.get(edgeid))){
                   var edge = (GraphEdge<T>)graph.edges.get(edgeid);
                   connections.add(edge);
                   edge.valid = true; // in case.
               }
               return (GraphEdge<T>)graph.edges.get(edgeid);
            }
            GraphEdge<T> edge = new GraphEdge<>(this, extconn);
            graph.addEdge(edge);
            connections.add(edge);
            triggerConnectionChanged();
            return edge;
        }


        @Override
        public String toString(){
            return "GraphConnector{" +
            "id=" + id +
            ", node=" + node.build().block +
            '}';
        }

        public T getGraph(){
            return graph;
        }

        public GraphNode<T> getNode(){
            return node;
        }
    }
}
