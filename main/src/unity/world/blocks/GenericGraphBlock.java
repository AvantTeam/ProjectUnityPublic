package unity.world.blocks;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import unity.graphics.*;
import unity.world.graph.*;

import static mindustry.Vars.*;

public class GenericGraphBlock extends Block implements GraphBlock{
    public static boolean debugGraph = false;
    public GraphBlockConfig config = new GraphBlockConfig(this);
    public GenericGraphBlock(String name){
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

    public class GenericGraphBuild extends Building implements GraphBuild{
        OrderedMap<Class<? extends Graph>,GraphNode> graphNodes = new OrderedMap<>();
        int prevTileRotation = -1;
        boolean placed = false;

        @Override public void created(){ if(!placed){ init(); } }

        @Override
        public void placed(){

            super.placed();
            if(!placed){
                placed = true;
                connectToGraph();
            }
        }

        @Override public void onRemoved(){ disconnectFromGraph();super.onRemoved(); }
        @Override public void onDestroyed(){ disconnectFromGraph(); super.onDestroyed(); }

        @Override
        public void updateTile(){
            if(!placed){  placed = true; connectToGraph(); }
            super.updateTile();
            updateGraphs();
        }
        @Override public OrderedMap<Class<? extends Graph>, GraphNode> getNodes(){ return graphNodes; }
        @Override public Building getBuild(){ return this; }
        @Override public int getPrevRotation(){ return prevTileRotation; }
        @Override public void setPrevRotation(int t){ prevTileRotation = t; }
        @Override public void displayBars(Table table){ super.displayBars(table); displayGraphBars(table); }
        @Override public void write(Writes write){ super.write(write);writeGraphs(write); }
        @Override public void read(Reads read, byte revision){ super.read(read, revision); readGraphs(read); }
        ///

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
        }

        @Override
        public void display(Table table){
            super.display(table);
        }



        @Override
        public void drawSelect(){
            //temp for debug
            super.drawSelect();
            if(debugGraph){
                getNodes().each((cls, graphNode) -> {
                    for(var con : graphNode.connector){
                        var cong = (GraphConnector)con;
                        cong.getGraph().each((c) -> {
                            GraphConnector extcon = (GraphConnector)c;
                            Draw.color(Pal.accent);
                            Drawf.circles(extcon.getNode().build().x, extcon.getNode().build().y, tilesize * 0.3f);
                        });
                        cong.getGraph().eachEdge(e -> {
                            GraphEdge edge = (GraphEdge)e;
                            UnityDrawf.line(Pal.accent, edge.n1.getNode().build().x, edge.n1.getNode().build().y, edge.n2.getNode().build().x, edge.n2.getNode().build().y);
                        });
                    }
                });
                Draw.reset();
            }
        }


    }
}
