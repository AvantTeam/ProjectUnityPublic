package unity.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;
import unity.world.graph.GraphConnector.*;

public class SeebeckGenerator extends GenericGraphBlock{
    public float maxPower = 30;
    public float seebeckStrength = 200f;
    TextureRegion[] rotations;
    TextureRegion[] heat;
    public SeebeckGenerator(String name){
        super(name);
        flags = EnumSet.of(BlockFlag.generator);
        rotate = true;
        outputsPower = true;
        consumesPower = false;
    }

    @Override
    public void load(){
        super.load();

        rotations = new TextureRegion[4];
        rotations[0] = Core.atlas.find(name+"-1");
        rotations[1] = Core.atlas.find(name+"-2");
        rotations[2] = Core.atlas.find(name+"-3");
        rotations[3] = Core.atlas.find(name+"-4");
        heat = new TextureRegion[3];
        heat[0] = Core.atlas.find(name+"-heat-left");
        heat[1] = Core.atlas.find(name+"-heat-right");
        heat[2] = Core.atlas.find(name+"-heat-center");
    }

    public class SeebeckGeneratorBuild extends GenericGraphBuild{
        float leftheat,rightheat;
        @Override
        public float getPowerProduction(){

            var node = heatNode();
            leftheat = node.getTemp();
            rightheat = node.getTemp();
            float[] heatdiff = {0};
            ((FixedGraphConnector<HeatGraph>)node.connector.get(0)) .eachConnected((connector,port)->{
                if(connector.getNode() instanceof HeatGraphNode heatnode){
                    heatdiff[0] += Math.abs(node.getTemp()-heatnode.getTemp())*heatnode.conductivity; /// well to stop low conductivity from making huge temperature differentials
                    if(port.getOrdinal()!=0){
                        leftheat = heatnode.getTemp();
                    }else{
                        rightheat = heatnode.getTemp();
                    }
                }
            });
            return Mathf.clamp(seebeckStrength*node.flux,0,maxPower);
        }

        @Override
        public void draw(){
            var node = heatNode();
            Draw.rect(rotations[rotation],x,y);
            UnityDrawf.drawHeat(heat[0],x,y,rotdeg(),leftheat);
            UnityDrawf.drawHeat(heat[1],x,y,rotdeg(),rightheat);
            UnityDrawf.drawHeat(heat[2],x,y,rotdeg(),node.getTemp());
            drawTeamTop();
        }
    }
}
