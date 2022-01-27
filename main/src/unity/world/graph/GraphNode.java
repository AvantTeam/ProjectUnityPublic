package unity.world.graph;

import arc.struct.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.world.blocks.*;

public class GraphNode<T extends Graph>{
    public final GraphBlock.IGraphBuild build;
    public Seq<GraphConnector<T>> connector = new Seq<>();
    public final int id = idAccum++;
    private static int idAccum = 0;

    public GraphNode(GraphBlock.IGraphBuild build){
        this.build = build;
    }


    public void onPlace(){
        for(GraphConnector gc:connector){
            gc.recalcNeighbours();
        }
    }

    public void onRotate(){
        for(GraphConnector gc:connector){
            gc.recalcNeighbours();
        }
    }

    public void onRemove(){

    }

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
    public void saveGraph(){
    }

    public void loadGraph(){
    }


    public void removeSelf(){
    }

    //convenience
    public Block block(){return build.getBuild().block();}
    public Building build(){return build.getBuild();}


}
