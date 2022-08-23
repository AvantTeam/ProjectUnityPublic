package unity.world.blocks;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.game.*;
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


    @Override public void setStats(){ super.setStats(); config.setStats(stats); }
    @Override public Block getBuild(){
        return this;
    }
    @Override public GraphBlockConfig getConfig(){
        return config;
    }
    @Override public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
        super.drawPlanRegion(req,list);
        config.drawConnectionPoints(req,list); }


    public class GenericGraphBuild extends Building implements GraphBuild{
        OrderedMap<Class<? extends Graph>,GraphNode> graphNodes = new OrderedMap<>();
        int prevTileRotation = -1;
        boolean placed = false;

        @Override public Building create(Block block, Team team){ var b = super.create(block, team); if(b instanceof GraphBuild gb){gb.initGraph();} return b;}

        @Override public void created(){ initGraph();}

        @Override
        public void placed(){

            super.placed();
            if(!placed){
                placed = true;
                connectToGraph();
            }
        }
        @Override public void pickedUp(){ disconnectFromGraph(); placed = false; super.pickedUp(); }
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
                        if(cong.getGraph() instanceof TorqueGraph tg){
                            tg.propagate(g->{
                                if(g==cong.getGraph()){
                                    return;
                                }
                                g.each((c) -> {
                                    GraphConnector extcon = (GraphConnector)c;
                                    Draw.color(Pal.reactorPurple);
                                    Drawf.circles(extcon.getNode().build().x, extcon.getNode().build().y, tilesize * 0.3f);
                                });
                            });
                        }


                        cong.getGraph().eachEdge(e -> {
                            GraphEdge edge = (GraphEdge)e;
                            UnityDrawf.line(Pal.accent, edge.n1.getNode().build().x, edge.n1.getNode().build().y, edge.n2.getNode().build().x, edge.n2.getNode().build().y);
                        });
                        if(con instanceof GraphConnector.FixedGraphConnector fg){
                            for(var port : fg.connectionPoints){
                                Draw.color(port.edge==null? Color.red:Color.green);
                                Drawf.tri((port.getRelpos().x+tile.x + port.getDir().x*0.5f)*tilesize , (port.getRelpos().y+tile.y + port.getDir().y*0.5f)*tilesize, 4,4,0);
                            }
                        }
                    }
                });
                Draw.reset();
            }
        }


    }
}
