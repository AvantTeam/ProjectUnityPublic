package unity.world.blocks.power;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import unity.world.blocks.*;

public class CrankShaft extends GenericGraphBlock{
    TextureRegion[] base = new TextureRegion[4];
    TextureRegion handle;
    public CrankShaft(String name){
        super(name);
        configurable = true;
        config(Point2.class,(CrankShaftBuild b,Point2 p)->{
            if(b.lastCrank<=0){
                b.push = 1;
                b.lastCrank = 100;
            }
        });

    }

    @Override
    public void load(){
        super.load();
        for(int i = 0;i<4;i++){
            base[i] = loadTex("bottom"+(i+1));
        }
        handle = loadTex("handle");
    }

    public class CrankShaftBuild extends GenericGraphBuild{
        float lastCrank = 0;
        float push = 0;

        @Override
        public void buildConfiguration(Table table){
            super.buildConfiguration(table);
            var bu  = table.button(Icon.rotate,()->{configure(new Point2());}).get();
            bu.setDisabled(()->lastCrank>0);
        }

        @Override
        public void updateTile(){
            super.updateTile();
            lastCrank -= Time.delta;
            if(push>0){
                torqueNode().baseForce = Mathf.sqrt(push);
                push-=0.02*Time.delta;
            }else{
                torqueNode().baseForce = 0;
            }
        }

        public <T extends Integer> void f(T t){

        }
        public <T> void f(T t){

        }

        @Override
        public void draw(){
            Draw.rect(base[rotation],x,y);
            Draw.rect(handle,x,y,torqueNode().getGraph().rotation);
            drawTeamTop();
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            lastCrank = read.f();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(lastCrank);
        }
    }
}
