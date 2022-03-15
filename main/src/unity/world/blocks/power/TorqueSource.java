package unity.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;

public class TorqueSource extends GenericGraphBlock{
    public TorqueSource(String name){
        super(name);
        configurable = true;
        config(Float.class,(TorqueSourceBuild build, Float val)->{build.targetspeed = val;});
    }

    public class TorqueSourceBuild extends GenericGraphBuild{
        float targetspeed=10;

        @Override
        public void updateTile(){
            super.updateTile();
            getGraph(TorqueGraph.class).lastVelocity = targetspeed;
        }

        @Override
        public void buildConfiguration(Table table){
            super.buildConfiguration(table);
            table.add("Speed:");
            table.row();
            //placeholder.
            table.slider(0,90,1f,targetspeed, true,this::configure);
        }
        Color col = new Color();
        @Override
        public void draw(){
            Draw.rect(this.block.region, this.x, this.y, 0);
            float t = Mathf.clamp(Mathf.map(targetspeed,0,90,0,1) * Mathf.random(0.95f,1.05f));
            col.set(1f,0.5f,0.5f);
            col.lerp(Pal.heal,t);
            Lines.stroke(1f, col);
            Lines.lineAngle(x,y,Mathf.map(t ,0,1,225,-45), Vars.tilesize*0.25f);
            drawTeamTop();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(targetspeed);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            targetspeed = read.f();
        }

        @Override
        public Object config(){
            return targetspeed;
        }
    }
}
