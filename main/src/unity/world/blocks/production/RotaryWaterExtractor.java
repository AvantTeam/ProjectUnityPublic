package unity.world.blocks.production;


import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import unity.world.graph.*;

import static arc.Core.*;

public class RotaryWaterExtractor extends SolidPump implements GraphBlock{
    ////interface
    public GraphBlockConfig config = new GraphBlockConfig(this);
    @Override public Block getBuild(){
                return this;
            }
    @Override public GraphBlockConfig getConfig(){
            return config;
        }
    ////

    public final TextureRegion[] topRegions = new TextureRegion[4], bottomRegions = new TextureRegion[2], liquidRegions = new TextureRegion[2];
    public TextureRegion rotorRegion;

    public RotaryWaterExtractor(String name){
        super(name);
        solid = true;
        noUpdateDisabled = false;
        rotate = true;
        drawArrow = false;
        hasPower = false;
    }

    @Override
    public void load(){
        super.load();

        rotorRegion = atlas.find(name + "-rotor");

        for(int i = 0; i < 4; i++) topRegions[i] = atlas.find(name + "-top" + (i + 1));
        for(int i = 0; i < 2; i++){
            bottomRegions[i] = atlas.find(name + "-bottom" + (i + 1));
            liquidRegions[i] = atlas.find(name + "-liquid" + (i + 1));
        }
    }

    @Override public void setStats(){ super.setStats(); config.setStats(stats); }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
    }
    @Override public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
        super.drawPlanRegion(req,list);
        config.drawConnectionPoints(req,list); }


    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region};
    }

    public class RotaryWaterExtractorBuild extends SolidPumpBuild implements GraphBuild{
        float flowRate;

        ///////////
        OrderedMap<Class<? extends Graph>,GraphNode> graphNodes = new OrderedMap<>();
        int prevTileRotation = -1;
        boolean placed = false;
        @Override public Building create(Block block, Team team){ var b = super.create(block, team); if(b instanceof GraphBuild gb){gb.initGraph();} return b;}
        @Override public void created(){ if(!placed){ initGraph(); } }

        @Override
        public void placed(){
            super.placed();
            placed = true;
            connectToGraph();
        }

        @Override public void onRemoved(){ disconnectFromGraph();super.onRemoved(); }
        @Override public void onDestroyed(){ disconnectFromGraph(); super.onDestroyed(); }
        @Override public void pickedUp(){ disconnectFromGraph(); placed = false; super.pickedUp(); }

        @Override public OrderedMap<Class<? extends Graph>, GraphNode> getNodes(){ return graphNodes; }
        @Override public Building getBuild(){ return this; }
        @Override public int getPrevRotation(){ return prevTileRotation; }
        @Override public void setPrevRotation(int t){ prevTileRotation = t; }
        @Override public void displayBars(Table table){ super.displayBars(table); displayGraphBars(table); }
        @Override public void write(Writes write){ super.write(write);writeGraphs(write); }
        @Override public void read(Reads read, byte revision){ super.read(read, revision); readGraphs(read); }
        ////////

        @Override
        public void drawSelect(){
            super.drawSelect();
        }

        @Override
        public void updateConsumption(){
            super.updateConsumption();
            efficiency *= Mathf.clamp(getGraph(TorqueGraph.class).lastVelocity/torqueNode().maxSpeed);
        }

        @Override
        public void updateTile(){
            if(!placed){  placed = true; connectToGraph(); }
            super.updateTile();
            updateGraphs();
        }

        @Override
        public void draw(){
            float rot = getGraph(TorqueGraph.class).rotation;
            Draw.rect(bottomRegions[rotation % 2], x, y);
            if(liquids.currentAmount() > 0.001f) Drawf.liquid(liquidRegions[rotation % 2], x, y, liquids.currentAmount() / liquidCapacity, liquids.current().color);
            Drawf.shadow(rotorRegion, x - size / 2f, y - size / 2f, rot*0.2f);
            Draw.rect(rotorRegion, x, y, rot*0.2f);
            Draw.rect(topRegions[rotation], x, y);
            drawTeamTop();
        }

        @Override
        public float moveLiquidForward(boolean leaks, Liquid liquid){
            Building next = front();
            if(next == null) return 0f;
            return moveLiquid(next, liquid);
        }
    }
}
