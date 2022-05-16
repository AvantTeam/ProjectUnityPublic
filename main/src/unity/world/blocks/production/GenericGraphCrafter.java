package unity.world.blocks.production;

import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import unity.world.graph.*;

public class GenericGraphCrafter extends GenericCrafter implements GraphBlock{
    public GraphBlockConfig config = new GraphBlockConfig(this);
    public GenericGraphCrafter(String name){
        super(name);
        rotate = true;
        update = true;
    }

    @Override public Block getBuild(){
        return this;
    }
    @Override public GraphBlockConfig getConfig(){
        return config;
    }
    @Override public void setStats(){ super.setStats(); config.setStats(stats); }
    @Override public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
       super.drawPlanRegion(req,list);
       config.drawConnectionPoints(req,list); }
    @Override public boolean rotatedOutput(int x, int y){
            return false;
        }

    public class GenericGraphCrafterBuild extends GenericCrafterBuild implements GraphBuild{
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
    }
}
