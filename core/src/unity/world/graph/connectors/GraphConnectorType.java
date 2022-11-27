package unity.world.graph.connectors;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.GraphNodeType.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

/** @author Xelo */
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

        protected GraphConnector(GraphNode<T> node, T graph){
            this.node = node;
            graph.addVertex(this);

            id = lastId++;
        }

        @Override
        public int id(){
            return id;
        }

        @Override
        public T graph(){
            return graph;
        }

        @Override
        public <E extends GraphNodeI<T>> E node(){
            return node.as();
        }

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
                if(edge.other(this).node().build() == t){
                    return true;
                }
            }

            return false;
        }

        @Override
        public <E extends GraphConnectorI<T>> void eachConnected(Cons<E> cons){
            for(GraphEdge<T> edge : connections){
                cons.get(edge.other(this));
            }
        }

        @Override
        public void disconnect(){
            graph.removeVertex(this);
            if(connections.size > 0){
                Log.info("[scarlet] disconnected vertex still has edges!");
            }
        }

        @Override
        public GraphEdge<T> addEdge(GraphConnectorI<T> ext){
            long edgeId = GraphEdge.getId(this, ext);
            if(graph.edges.containsKey(edgeId)){
                var edge = graph.edges.get(edgeId);
                if(!connections.contains(edge)){
                    connections.add(edge);
                    edge.valid = true; // Xelo: in case.
                }

                return edge;
            }

            GraphEdge<T> edge = new GraphEdge<>(this, ext);
            graph.addEdge(edge);
            connections.add(edge);

            connectionChanged();
            return edge;
        }

        @Override
        public void removeEdge(GraphEdge<T> ge){
            if(connections.remove(ge)){
                ge.valid = false;
                connectionChanged();
            }
        }

        @Override
        public void connectionChanged(){
            node.build.onConnectionChanged(this);
        }

        @Override
        public String toString(){
            return "GraphConnector{" +
            "id=" + id +
            ", node=" + node.build().block +
            '}';
        }
    }
}
