package unity.world.graph.nodes;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.connectors.GraphConnectorType.*;
import unity.world.graph.connectors.GraphConnectorTypeI.*;

public abstract class GraphNodeType<T extends Graph<T>> implements GraphNodeTypeI<T>{
    private static String[] levelNames = {
        "stat.unity-negligible",
        "stat.unity-small",
        "stat.unity-moderate",
        "stat.unity-significant",
        "stat.unity-major",
        "stat.unity-extreme"
    };

    public String getNamedLevel(float val, float level[]){
        for(int i = 0; i < level.length; i++){
            if(val <= level[i]) return levelNames[i];
        }
        return levelNames[levelNames.length-1];
    }

    public void addLevelStat(Stats stats, Stat stat, float val, float[] levels){
        stats.add(stat, Core.bundle.format(getNamedLevel(val, levels), Core.bundle.format("stat." + stat.name + ".info", val)));
    }

    public void addStat(Stats stats, Stat stat, Object val){
        stats.add(stat, Core.bundle.format("stat." + stat.name + ".info", val));
    }

    @Override
    public abstract <E extends Building & GraphBuild> GraphNode<T> create(E build);

    public static abstract class GraphNode<T extends Graph<T>> implements GraphNodeI<T>{
        public final GraphBuild build;
        public Seq<GraphConnector<T>> connectors = new Seq<>();
        public final int id = idAccum++;
        private static int idAccum = 0;

        protected GraphNode(GraphBuild build){
            this.build = build;
        }

        @Override
        public void update(){

        }

        @Override
        public void addSelf(){
            onConnect();
        }

        @Override
        public void removeSelf(){
            onDisconnect();
        }

        @Override
        public void onConnect(){
            for(GraphConnector<T> gc : connectors){
                gc.recalcNeighbors();
            }
        }

        @Override
        public void onDisconnect(){
            for(GraphConnector<T> gc : connectors) gc.disconnect();
        }

        @Override
        public void onRotate(){
            for(GraphConnector<T> gc : connectors){
                if(gc.disconnectWhenRotate){
                    gc.disconnect();
                    gc.recalcPorts();
                    gc.recalcNeighbors();
                }
            }
        }

        @Override
        public void displayBars(Table table){}

        @Override
        public Seq<GraphConnector<T>> connectors(){
            return connectors;
        }

        @Override
        public void addConnector(GraphConnectorI<T> connector){
            connectors.add(connector.<GraphConnector<T>>as());
        }

        @Override
        public void removeEdge(GraphNodeI<T> g){}

        @Override
        public void write(Writes write){
            connectors.each(con -> {
                if(con.graph().isRoot(con)){
                    write.bool(true);
                    con.graph.write(write);
                }else{
                    write.bool(false);
                }

                con.write(write);
            });
        }

        @Override
        public void read(Reads read){
            connectors.each(con -> {
                if(read.bool()) con.graph.read(read);
                con.read(read);
            });
        }

        @Override
        public Block block(){
            return build.block();
        }

        @Override
        public <E extends Building & GraphBuild> E build(){
            return build.as();
        }

        @Override
        public T graph(){
            return connectors.first().graph;
        }
    }
}
