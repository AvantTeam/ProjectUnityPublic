package unity.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.Block;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import unity.graphics.UnityDrawf;
import unity.world.graph.*;

import static arc.Core.atlas;
import static mindustry.Vars.state;

public class TorquePowerGenerator extends PowerGenerator implements GraphBlock {
    public GraphBlockConfig config = new GraphBlockConfig(this);
    public float maxEfficiency = 2.0f;
    public float powerProduction;

    public final TextureRegion[] bottomRegions = new TextureRegion[2], topRegions = new TextureRegion[2];
    public TextureRegion mbaseRegion, wormDrive, gearRegion, bindingRegion, rotateRegion, overlayRegion;

    public TorquePowerGenerator(String name) {
        super(name);
        rotate = true;
        drawArrow = false;
        hasPower = true;
        flags = EnumSet.of(BlockFlag.generator);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(hasPower && outputsPower){
            addBar("power", (GeneratorBuild entity) -> new Bar(() ->
                    Core.bundle.format("bar.poweroutput",
                            Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale(), 1)),
                    () -> Pal.powerBar,
                    entity::getPowerProduction));
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{atlas.find(name)};
    }

    @Override
    public void load() {
        super.load();

        mbaseRegion = atlas.find(name + "-mbase");

        gearRegion = atlas.find(name + "-gear");
        bindingRegion = atlas.find(name + "-binding");
        overlayRegion = atlas.find(name + "-overlay");
        rotateRegion = atlas.find(name + "-moving");
        wormDrive = atlas.find(name + "-rotate");

        for (int i = 0; i < 2; i++) {
            bottomRegions[i] = atlas.find(name + "-bottom" + (i + 1));
            topRegions[i] = atlas.find(name + "-top" + (i + 1));
        }
    }

    @Override public void setStats(){
        super.setStats();
        config.setStats(stats);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
    }
    @Override public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
        super.drawPlanRegion(req,list);
        config.drawConnectionPoints(req,list); }

    @Override
    public Block getBuild() {return this;}

    @Override
    public GraphBlockConfig getConfig() {return config;}
    @Override public boolean outputsItems(){
        return false;
    }

    @Override public boolean rotatedOutput(int x, int y){
        return false;
    }
    public class TorquePowerGeneratorBuild extends GeneratorBuild implements GraphBuild{
        public float generateTime;
        /** The efficiency of the producer. An efficiency of 1.0 means 100% */
        OrderedMap<Class<? extends Graph>,GraphNode> graphNodes = new OrderedMap<>();
        public float productionEfficiency = 0.0f;
        int prevTileRotation = -1;
        boolean placed = false;
        @Override public Building create(Block block, Team team){ var b = super.create(block, team); if(b instanceof GraphBuild gb){gb.initGraph();} return b;}
        @Override public void created(){ if(!placed){ initGraph(); } }
        @Override public void displayBars(Table table){ super.displayBars(table); displayGraphBars(table); }
        @Override public void write(Writes write){ super.write(write);writeGraphs(write);write.f(productionEfficiency);write.f(generateTime); }
        @Override public void read(Reads read, byte revision){ super.read(read, revision); readGraphs(read);productionEfficiency = read.f();if(revision >= 1){generateTime = read.f();}}
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
            productionEfficiency = Mathf.clamp(Mathf.map(getGraph(TorqueGraph.class).lastVelocity, 0, torqueNode().maxSpeed, 0, maxEfficiency), 0, maxEfficiency);
        }

        @Override
        public float getPowerProduction() {
            return powerProduction * productionEfficiency;
        }

        @Override
        public void draw(){
            float rot = getGraph(TorqueGraph.class).rotation;
            float fixedRot = (rotdeg() + 90f) % 180f - 90f;

            int variant = rotation % 2;

            float deg = rotation == 0 || rotation == 3 ? rot : -rot;
            Point2 offset = Geometry.d4(rotation + 1);

            Draw.rect(bottomRegions[variant], x, y);
            //shaft
            Draw.rect(mbaseRegion, x, y, fixedRot);

            UnityDrawf.drawRotRect(wormDrive, x, y, 24f, 3.5f, 3.5f, fixedRot, rot, rot + 180f);
            UnityDrawf.drawRotRect(wormDrive, x, y, 24f, 3.5f, 3.5f, fixedRot, rot + 180f, rot + 360f);
            UnityDrawf.drawRotRect(rotateRegion, x, y, 24f, 3.5f, 3.5f, fixedRot, rot, rot + 180f);

            Draw.rect(overlayRegion, x, y, fixedRot);

            //gears
            Drawf.spinSprite(gearRegion, x + offset.x * 4f, y + offset.y * 4f, -deg/2);
            Drawf.spinSprite(gearRegion, x - offset.x * 4f, y - offset.y * 4f, deg/2);
            //Bindings
            Draw.rect(bindingRegion, x + offset.x * 4f, y + offset.y * 4f, 0);
            Draw.rect(bindingRegion, x - offset.x * 4f, y - offset.y * 4f, 0);

            Draw.rect(topRegions[variant], x, y);
            drawTeamTop();
        }
        @Override
        public float ambientVolume(){
            return Mathf.clamp(productionEfficiency);
        }

        @Override
        public OrderedMap<Class<? extends Graph>, GraphNode> getNodes() {
            return graphNodes;
        }

        @Override
        public Building getBuild() {
            return this;
        }

        @Override
        public int getPrevRotation() {
            return prevTileRotation;
        }

        @Override
        public void setPrevRotation(int t) {
            prevTileRotation = t;
        }

        @Override
        public byte version(){
            return 1;
        }
    }
}
