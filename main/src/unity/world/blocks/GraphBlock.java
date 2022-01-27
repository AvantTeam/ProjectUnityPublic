package unity.world.blocks;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.world.graph.*;

public class GraphBlock extends Block implements IGraphBlock{
    public GraphBlockConfig config = new GraphBlockConfig(this);
    public GraphBlock(String name){
        super(name);
        update = true;
    }

    @Override
    public void load(){
        super.load();
    }

    @Override
    public void setStats(){
        super.setStats();
    }

    @Override public Block getBuild(){
        return this;
    }

    @Override public GraphBlockConfig getConfig(){
        return config;
    }

    public static interface IGraphBuild{
        OrderedMap<Class<? extends Graph>, GraphNode> getNodes();
        Building getBuild();
        int getPrevRotation();
        void setPrevRotation(int t);
        default <T extends Graph> GraphNode<T> getGraphNode(Class<T> c){
            return (GraphNode<T>)getNodes().get(c);
        }
        default <T extends Graph> T getGraph(Class<T> c){
            return getGraph(c,0);
        }
        default <T extends Graph> T getGraph(Class<T> c, int index){
            return this.getGraphNode(c).connector.get(index).getGraph();
        }

        default void init(){
            setPrevRotation(getBuild().rotation);
            if(getBuild().block instanceof IGraphBlock graphBlock){
                var cfg = graphBlock.getConfig();
                //generics make me cry
                //fishes all relevant things from GraphBlockConfig in the GraphBlock
                //and populates the build using the settings.
                for(var connectionConfig: cfg.connections){
                    if(!getNodes().containsKey(connectionConfig.getGraphType())){
                        var gnode =  cfg.nodeConfig.get(connectionConfig.getGraphType()).get(this);
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
            for(var entry: getNodes()){
                entry.value.removeSelf();
            }
        }
        default void reconnectToGraph(){
            for(var entry: getNodes()){
                entry.value.addSelf();
            }
        }
        default void updateGraphs(){
            getNodes().each((cls, graphNode) -> {
                //change later.
                for(var graphConn:graphNode.connector){
                    ((GraphConnector)graphConn).getGraph().update();
                }
            });
            if(getPrevRotation()!=getBuild().rotation){
                setPrevRotation(getBuild().rotation);
                disconnectFromGraph();
                reconnectToGraph();
            }
        }
        default void onConnectionChanged(GraphConnector g){}
    }

    public class GraphBuild extends Building implements IGraphBuild{
        OrderedMap<Class<? extends Graph>,GraphNode> graphNodes = new OrderedMap<>();
        int prevTileRotation = -1;

        @Override
        public void created(){
            init();
        }

        @Override
        public void placed(){
            super.placed();
            reconnectToGraph();
        }

        @Override
        public void onRemoved(){
            disconnectFromGraph();
            super.onRemoved();
        }

        @Override
        public void updateTile(){
            super.updateTile();
            updateGraphs();
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
        }

        @Override
        public void display(Table table){
            super.display(table);
        }

        @Override
        public void displayBars(Table table){
            super.displayBars(table);
        }

        @Override
        public void write(Writes write){
            super.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
        }

        @Override
        public void drawSelect(){}

        @Override public OrderedMap<Class<? extends Graph>, GraphNode> getNodes(){
            return graphNodes;
        }

        @Override public Building getBuild(){
            return this;
        }
        @Override public int getPrevRotation(){
            return prevTileRotation;
        }

        @Override public void setPrevRotation(int t){
            prevTileRotation = t;
        }
    }
}
