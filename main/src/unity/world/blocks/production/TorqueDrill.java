package unity.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import unity.graphics.*;
import unity.world.graph.*;

import static arc.Core.atlas;

public class TorqueDrill extends Drill implements GraphBlock{
    public GraphBlockConfig config = new GraphBlockConfig(this);
    public float maxEfficiency = 3.0f;

    public final TextureRegion[] bottomRegions = new TextureRegion[2], topRegions = new TextureRegion[2], oreRegions = new TextureRegion[2];
    public TextureRegion rotorRegion, rotorRotateRegion, mbaseRegion, wormDrive, gearRegion, rotateRegion, overlayRegion;

    public TorqueDrill(String name){
        super(name);
        rotate = true;
        drawArrow = false;
    }
    @Override
    public TextureRegion[] icons(){
            return new TextureRegion[]{atlas.find(name)};
        }

    @Override
    public void load(){
        super.load();

        rotorRegion = atlas.find(name + "-rotor");
        rotorRotateRegion = atlas.find(name + "-rotor-rotate");
        mbaseRegion = atlas.find(name + "-mbase");

        gearRegion = atlas.find(name + "-gear");
        overlayRegion = atlas.find(name + "-overlay");
        rotateRegion = atlas.find(name + "-moving");
        wormDrive = atlas.find(name + "-rotate");

        for(int i = 0; i < 2; i++){
            bottomRegions[i] = atlas.find(name + "-bottom" + (i + 1));
            topRegions[i] = atlas.find(name + "-top" + (i + 1));
            oreRegions[i] = atlas.find(name + "-ore" + (i + 1));
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
    @Override public Block getBuild(){
            return this;
        }
    @Override public GraphBlockConfig getConfig(){
        return config;
    }

    @Override public boolean outputsItems(){
        return true;
    }

    @Override public boolean rotatedOutput(int x, int y){
        return false;
    }


    public class TorqueDrillBuild extends DrillBuild implements GraphBuild{
        OrderedMap<Class<? extends Graph>,GraphNode> graphNodes = new OrderedMap<>();
        int prevTileRotation = -1;
        boolean placed = false;
        @Override public Building create(Block block, Team team){ var b = super.create(block, team); if(b instanceof GraphBuild gb){gb.initGraph();} return b;}
        @Override public void created(){ if(!placed){ initGraph(); } }
        @Override public void displayBars(Table table){ super.displayBars(table); displayGraphBars(table); }
        @Override public void write(Writes write){ super.write(write);writeGraphs(write); }
        @Override public void read(Reads read, byte revision){ super.read(read, revision); readGraphs(read); }
        @Override public void pickedUp(){ disconnectFromGraph(); placed = false; super.pickedUp(); }
        @Override
        public void placed(){
            super.placed();
            placed = true;
            connectToGraph();
        }

        @Override
        public void onRemoved(){
            disconnectFromGraph();
            super.onRemoved();
        }

        @Override
        public void updateTile(){
            if(!placed){
                placed = true;
                connectToGraph();
            }
            super.updateTile();
            updateGraphs();
        }

        @Override
        public void updateConsumption(){
            super.updateConsumption();
            efficiency *= Mathf.sqrt(Mathf.clamp(Mathf.map(getGraph(TorqueGraph.class).lastVelocity,0,torqueNode().maxSpeed,0,maxEfficiency),0,maxEfficiency));
        }

        @Override
        public void draw(){
            float rot = getGraph(TorqueGraph.class).rotation;
            float fixedRot = (rotdeg() + 90f) % 180f - 90f;

            int variant = rotation % 2;

            float deg = rotation == 0 || rotation == 3 ? rot : -rot;
            float shaftRot = rot * 0.75f;

            Point2 offset = Geometry.d4(rotation + 1);

            Draw.rect(bottomRegions[variant], x, y);

            //target region
            if(dominantItem != null && drawMineItem){
                Draw.color(dominantItem.color);
                Draw.rect(oreRegions[variant], x, y);
                Draw.color();
            }
            float bottomrotor = 360f - rot * 0.05f;
            //bottom rotor
            Draw.rect(rotorRegion, x, y, bottomrotor);

            UnityDrawf.drawRotRect(rotorRotateRegion, x, y, 24f, 3.5f, 3.5f, 90 + bottomrotor, shaftRot, shaftRot + 180f);
            UnityDrawf.drawRotRect(rotorRotateRegion, x, y, 24f, 3.5f, 3.5f, 90 + bottomrotor, shaftRot + 180f, shaftRot + 360f);

            //shaft
            Draw.rect(mbaseRegion, x, y, fixedRot);

            UnityDrawf.drawRotRect(wormDrive, x, y, 24f, 3.5f, 3.5f, fixedRot, rot, rot + 180f);
            UnityDrawf.drawRotRect(wormDrive, x, y, 24f, 3.5f, 3.5f, fixedRot, rot + 180f, rot + 360f);
            UnityDrawf.drawRotRect(rotateRegion, x, y, 24f, 3.5f, 3.5f, fixedRot, rot, rot + 180f);

            Draw.rect(overlayRegion, x, y, fixedRot);

            //gears
            Draw.rect(gearRegion, x + offset.x * 4f, y + offset.y * 4f, -deg/2);
            Draw.rect(gearRegion, x - offset.x * 4f, y - offset.y * 4f, deg/2);

            Draw.rect(topRegions[variant], x, y);
            drawTeamTop();
        }

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
