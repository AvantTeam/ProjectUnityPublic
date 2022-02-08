package unity.world.graph;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;

//orginally GraphModule
public class GraphNode<T extends Graph>{
    public final GraphBuild build;
    public Seq<GraphConnector<T>> connector = new Seq<>();
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
            gc.disconnect();
            gc.recalcPorts();
            gc.recalcNeighbours();
        }
    }

    public void onRemove(){
        for(GraphConnector gc:connector){
            gc.disconnect();
        }
    }

    public void displayBars(Table table){}

    public void removeEdge(GraphNode g){
    }



    public void findEdges(){

    }
    public void addSelf(){
        onPlace();
    }

    //one node will be in charge of saving the graph, probably its root node.
    public void isGraphSaver(){

    }
    public void write(Writes write){
        connector.each(con->{
            if(con.getGraph().firstVertex()==con){
                write.bool(true);
                con.graph.write(write);
            }else{
                write.bool(false);
            }
        });
    }

    public void read(Reads read){
        connector.each(con->{
            if(read.bool()){
                con.graph.read(read);
            }
        });
    }


    public void removeSelf(){
        onRemove();
    }

    //convenience
    public Block block(){return build.getBuild().block();}
    public Building build(){return build.getBuild();}


}
