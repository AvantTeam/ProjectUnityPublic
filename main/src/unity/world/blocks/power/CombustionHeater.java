package unity.world.blocks.power;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;

import static arc.Core.atlas;

public class CombustionHeater extends GenericGraphBlock{
    public final TextureRegion[] baseRegions = new TextureRegion[4];
    TextureRegion heatRegion;
    public float baseTemp = 1000 + HeatGraphNode.celsiusZero;
    public float tempPerFlammability = 1750;

    public float minConsumeAmount = 0.005f;
    public float maxConsumeAmount = 0.015f;

    public CombustionHeater(String name){
        super(name);
        rotate = true;
    }

    @Override
    public void load(){
        super.load();
        for(int i = 0; i < 4; i++) baseRegions[i] = atlas.find(name + "-base" + (i + 1));
        heatRegion =  atlas.find(name + "-heat");
    }

    @Override
    public void init(){
        consume(new ConsumeItemFilter(item -> item.flammability >= 0.1f)).update(false).optional(true, false);
        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.productionTime, "@ - @ "+StatUnit.seconds.localized(), Strings.fixed((1 / minConsumeAmount)/60f,1),Strings.fixed((1/maxConsumeAmount)/60f,1) );
    }

    public class CombustionHeaterBuild extends GenericGraphBuild{
        float generateTime, productionEfficiency;

        @Override
        public void initGraph(){
            super.initGraph();
            heatNode().minGenerate = 0;
        }

        @Override
        public boolean productionValid(){
            return generateTime > 0f;
        }

        @Override
        public void update(){
            super.update();
            if(!canConsume()){
                productionEfficiency = 0f;
                return;
            }

            if(generateTime <= 0f && items.total() > 0f){
                Fx.generatespark.at(x + Mathf.range(3f), y + Mathf.range(3f));
                Item item = items.take();
                productionEfficiency = item.flammability;
                generateTime = 1f;
            }

            if(generateTime > 0f){
                float mul = Mathf.lerp(minConsumeAmount,maxConsumeAmount,Mathf.clamp(heatNode().lastEnergyInput*0.3f,0.1f,1.0f));
                float am = Math.min(delta() * mul, generateTime);;
                generateTime -= am;
            }else{
                productionEfficiency = 0f;
            }
            heatNode().targetTemp = baseTemp + Math.max(tempPerFlammability*(productionEfficiency-1),-baseTemp*0.5f);
            heatNode().efficency = productionEfficiency;
        }

        @Override
        public void draw(){
            Draw.rect(baseRegions[rotation], x, y);
            UnityDrawf.drawHeat(heatRegion, x, y, rotdeg(), heatNode().getTemp());

            drawTeamTop();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(productionEfficiency);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read,revision);
            productionEfficiency = read.f();
        }
    }
}