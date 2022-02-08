package unity.world.graph;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
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

    default void connectToGraph(){
        for(var entry : getNodes()){
            entry.value.onPlace();
        }
    }
    default void onRotate(){
        for(var entry : getNodes()){
            entry.value.onRotate();
        }
    }

    default void updateGraphs(){
        if(getPrevRotation()==-1){
            setPrevRotation(getBuild().rotation);
            connectToGraph();
        }

        if(getPrevRotation() != getBuild().rotation && getBuild().block.rotate){
            setPrevRotation(getBuild().rotation);
            onRotate();
        }

        getNodes().each((cls, graphNode) -> {
            //change later.
            graphNode.update();
            for(var graphConn : graphNode.connector){
                ((GraphConnector)graphConn).getGraph().update();
            }
        });
    }

    default void displayGraphBars(Table table){
        getNodes().each((cls, graphNode) -> {
            graphNode.displayBars(table);
        });
    }

    default void onConnectionChanged(GraphConnector g){
    }

    ///I pray they are in the same order.
    default void writeGraphs(Writes write){
        getNodes().each((cls, graphNode) -> {
            graphNode.write(write);
        });
    }

    default void readGraphs(Reads read){
        getNodes().each((cls, graphNode) -> {
            graphNode.read(read);
        });
    }

    //conv for js
    default TorqueGraphNode torqueNode(){
        return (TorqueGraphNode)getGraphNode(TorqueGraph.class);
    }
    default HeatGraphNode heatNode(){
        return (HeatGraphNode)getGraphNode(HeatGraph.class);
    }

    //conv for drawing
    default float getCorrectRotation(){
        return (getBuild().rotdeg() + 90f) % 180f - 90f;
    }
}
