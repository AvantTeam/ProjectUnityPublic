package unity.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.graphics.*;
import unity.gen.graph.*;
import unity.graphics.*;
import unity.util.*;
import unity.world.blocks.*;
import unity.world.graph.nodes.*;

public class HeatSource extends HeatBlock{
    public TextureRegion heatRegion, baseRegion;

    public HeatSource(String name){
        super(name);
        configurable = true;

        config(Float.class, (HeatSourceBuild build, Float val) -> build.targetTemp = val);
    }

    @Override
    public void load(){
        super.load();

        baseRegion = Core.atlas.find(name + "-base");
        heatRegion = Core.atlas.find(name + "-heat");
    }

    public class HeatSourceBuild extends HeatBuild{
        public float targetTemp = HeatNodeType.celsiusZero;
        public Color col = new Color();

        @Override
        public void updateTile(){
            super.updateTile();
            heatNode.setTemp(targetTemp);
        }

        @Override
        public void buildConfiguration(Table table){
            super.buildConfiguration(table);

            // Xelo: placeholder.
            table.label(() -> Core.bundle.format("bar.unity-temp", Strings.fixed(targetTemp - HeatNodeType.celsiusZero, 1)));

            table.row();
            table.slider(0, HeatNodeType.celsiusZero + 3000f, 10f, targetTemp, true, this::configure);
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            DrawUtils.drawHeat(heatRegion, x, y, rotdeg(), heatNode.getTemp());

            drawTeamTop();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(targetTemp);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            targetTemp = read.f();
        }

        @Override
        public Float config(){
            return targetTemp;
        }
    }
}
