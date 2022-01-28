package unity.world.graph;

import arc.func.*;
import arc.struct.*;
import mindustry.gen.*;

public interface GraphBuild{
    OrderedMap<Class<? extends Graph>, GraphNode> getNodes();

    Building getBuild();

    int getPrevRotation();

    void setPrevRotation(int t);

    default <T extends Graph> GraphNode<T> getGraphNode(Class<T> c){
        return (GraphNode<T>)getNodes().get(c);
    }

    default <T extends Graph> T getGraph(Class<T> c){
        return getGraph(c, 0);
    }

    default <T extends Graph> T getGraph(Class<T> c, int index){
        return this.getGraphNode(c).connector.get(index).getGraph();
    }

    default void init(){
        setPrevRotation(getBuild().rotation);
        if(getBuild().block instanceof GraphBlock graphBlock){
            var cfg = graphBlock.getConfig();
            //generics make me cry
            //fishes all relevant things from GraphBlockConfig in the GraphBlock
            //and populates the build using the settings.
            for(var connectionConfig : cfg.connections){
                if(!getNodes().containsKey(connectionConfig.getGraphType())){
                    var gnode = cfg.nodeConfig.get(connectionConfig.getGraphType()).get(this);
                    getNodes().put(connectionConfig.getGraphType(), (GraphNode)gnode);
                }
                var node = getGraphNode(connectionConfig.getGraphType());
                node.connector.add(connectionConfig.getConnector(node));
            }
        }
    }


    default <T extends Graph> void addNode(Class<T> c, Prov<GraphNode<T>> prov){
        getNodes().put(c, prov.get());
    }

    default void disconnectFromGraph(){
        for(var entry : getNodes()){
            entry.value.removeSelf();
        }
    }

    default void reconnectToGraph(){
        for(var entry : getNodes()){
            entry.value.onRotate();
        }
    }

    default void updateGraphs(){
        getNodes().each((cls, graphNode) -> {
            //change later.
            graphNode.update();
            for(var graphConn : graphNode.connector){
                ((GraphConnector)graphConn).getGraph().update();
            }
        });
        if(getPrevRotation() != getBuild().rotation){
            setPrevRotation(getBuild().rotation);
            disconnectFromGraph();
            reconnectToGraph();
        }
    }

    default void onConnectionChanged(GraphConnector g){
    }

    //conv for js
    default GraphNode<TorqueGraph> torqueNode(){
        return getGraphNode(TorqueGraph.class);
    }
}
