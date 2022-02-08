package unity.world.graph;

import arc.func.*;
import arc.struct.*;
import mindustry.world.*;

import java.lang.reflect.*;

//the uh block settings part of graphs ig
public class GraphBlockConfig{
    public ObjectMap<Class<? extends Graph>,Func<GraphBuild,GraphNode<?>>> nodeConfig = new ObjectMap<>();
    public Seq<ConnectionConfig> connections = new Seq<>();
    Block block;

    public GraphBlockConfig(Block block){
        this.block = block;
    }

    public void addConnectionConfig(ConnectionConfig cfg){
        //this is basically an assert right
        if(cfg.config!=null){
            throw new IllegalArgumentException("GraphBlockConfig cannot accept an already-bound ConnectionConfig");
        }
        connections.add(cfg);
        cfg.setConfig(this);
    }

    public <T extends Graph> void fixedConnection(Class<T> tClass, int... connectionIndexes){
        try{
            var constructor = tClass.getConstructor();
            addConnectionConfig(new FixedConnectionConfig(tClass,()-> {
                try{ return constructor.newInstance(); } catch(Exception e) { e.printStackTrace(); }
                return null;
            },connectionIndexes));
        }catch(Exception e){
            throw new IllegalStateException("Graph doesn't have a empty constructor or constructor is invalid/inaccessible");
        }

    }

    public static abstract class ConnectionConfig<T extends Graph>{
        public GraphBlockConfig config;
        Class<T> graphType;

        public abstract GraphConnector getConnector(GraphNode gn);
        public Prov<T> newGraph;

        public ConnectionConfig( Class<T> tClass,Prov<T> newGraph){
            this.newGraph = newGraph;
            graphType = tClass;
        }

        public void setConfig(GraphBlockConfig config){
            this.config = config;
        }

        public Class<T> getGraphType(){
            return graphType;
        }
    }

    //for connections on the sides, most probably the most common type.
    public static class FixedConnectionConfig<T extends Graph>  extends ConnectionConfig<T>{
        int[] connectionIndexes;
        public FixedConnectionConfig( Class<T> tClass,Prov<T> newGraph, int... connectionIndexes){
            super( tClass,newGraph);
            this.connectionIndexes = connectionIndexes;
        }

        @Override public GraphConnector getConnector(GraphNode gn){
            return new GraphConnector.FixedGraphConnector(gn, newGraph.get(),connectionIndexes);
        }

        @Override public void setConfig(GraphBlockConfig config){
            super.setConfig(config);
            if(connectionIndexes.length != config.block.size*4){
                throw  new IllegalStateException(config.block.name+": Number of connectionIndexes do not match the size of the block x 4, was"+connectionIndexes.length+", expected: "+(config.block.size*4));
            }
        }
    }

    public Block getBlock(){
        return block;
    }
}
