package unity.world.blocks.power;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.power.PowerGenerator.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;
import unity.world.graph.GraphConnector.*;

public class SeebeckGenerator extends GenericGraphBlock{
    public float maxPower = 10;
    public float seebeckStrength = 2f;
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

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.basePowerGeneration, Core.bundle.get("stat.unity-seebeckStrength"),seebeckStrength * 60.0f);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("power", (SeebeckGeneratorBuild entity) -> new Bar(() ->
        Core.bundle.format("bar.poweroutput",
        Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale(), 1)),
        () -> Pal.powerBar,
        () -> entity.getPowerProduction()/maxPower));
    }

    public class SeebeckGeneratorBuild extends GenericGraphBuild{
        float leftheat,rightheat;
        float[] heatdiff = {0};
        float powergen = 0;
        @Override
        public float getPowerProduction(){
            return Mathf.clamp(powergen,0,maxPower);
        }


        @Override
        public void updateTile(){
            super.updateTile();

            var node = heatNode();
            leftheat = node.getTemp();
            rightheat = node.getTemp();
            heatdiff[0] = 0;
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
            powergen+=(seebeckStrength*heatdiff[0]-powergen)*Mathf.clamp(0.1f* Time.delta);
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
