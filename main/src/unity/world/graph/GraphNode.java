package unity.world.graph;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.world.meta.*;

//orginally GraphModule
public class GraphNode<T extends Graph>{
    public final GraphBuild build;
    public Seq<GraphConnector<T>> connector = new Seq<>();
    int connectors = 0;
    public final int id = idAccum++;
    private static int idAccum = 0;

    public GraphNode(GraphBuild build){
        this.build = build;
    }

    public void update(){

    }

    public void onPlace(){
        for(GraphConnector gc:connector){
            gc.recalcNeighbours();
        }
    }

    public void onRotate(){
        for(GraphConnector gc:connector){
            if(gc.disconnectWhenRotate){
                gc.disconnect();
                gc.recalcPorts();
                gc.recalcNeighbours();
            }
        }
    }

    public void onRemove(){
        for(GraphConnector gc:connector){
            gc.disconnect();
        }
    }

    public <W extends GraphConnector<T>> W getConnectorOfType(Class<W> cls){
        for(GraphConnector<T> gc:connector){
            if(cls.isAssignableFrom(gc.getClass())){
                return (W)gc;
            }
        }
        return null;
    }

    public void setStats(Stats stats){}

    public void displayBars(Table table){}
    private static String[] levelNames = {
        "stat.unity-negligible",
        "stat.unity-small",
        "stat.unity-moderate",
        "stat.unity-significant",
        "stat.unity-major",
        "stat.unity-extreme"
    };
    public String getNamedLevel(float val, float level[]){
        for(int i =0;i<level.length;i++){
            if(val<=level[i]){
                return levelNames[i];
            }
        }
        return levelNames[levelNames.length-1];
    }

    public void addLevelStat(Stats stats, Stat stat, float val, float[] levels){
        stats.add(stat, Core.bundle.format(getNamedLevel(val, levels), Core.bundle.format("stat." + stat.name + ".info", val)));
    }

    public void addStat(Stats stats, Stat stat, Object val){
        stats.add(stat, Core.bundle.format("stat." + stat.name + ".info", val));
    }

    public void removeEdge(GraphNode g){
    }

    public void addSelf(){
        onPlace();
    }

    public void write(Writes write){
        connector.each(con->{
            if(con.getGraph().isRoot(con)){
                write.bool(true);
                con.graph.write(write);
            }else{
                write.bool(false);
            }
            con.write(write);
        });
    }

    public void read(Reads read){
        connector.each(con->{
            if(read.bool()){
                con.graph.read(read);
            }
            con.read(read);
        });
    }


    public void removeSelf(){
        onRemove();
    }

    //convenience
    public Block block(){return build.getBuild().block();}
    public Building build(){return build.getBuild();}
    public T getGraph(){return connector.first().graph;}

}
