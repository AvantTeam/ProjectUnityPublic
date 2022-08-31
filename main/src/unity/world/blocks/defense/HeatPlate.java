package unity.world.blocks.defense;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import unity.*;
import unity.world.blocks.*;
import unity.world.graph.*;
import unity.world.meta.*;
import unity.world.systems.*;

import static unity.world.blocks.distribution.HeatPipe.baseColor;

public class HeatPlate extends GenericGraphBlock{
    TextureRegion heat;
    float damagemul = 4.0f;
    public HeatPlate(String name){
        super(name);
        outputsPayload = true;
    }

    @Override
    public void load(){
        super.load();
        heat = loadTex("heat");
    }

    public class HeatPlateBuild extends GenericGraphBuild implements CustomGroundFluidTerrain{

        @Override
        public float terrainHeight(){
            return 0.1f;
        }
        //todo: replace with something that affects fluid on its thread
        @Override
        public void updateTile(){
            super.updateTile();
            var heat = heatNode();
            var fluidcontrol = Unity.groundFluidControl;
            int findex = fluidcontrol.tileIndexOf(tile.x,tile.y);
            var fluid = fluidcontrol.fluidType(tile.x,tile.y);
            if(fluid != null && fluid.association!=null){
                var liquid = CrucibleRecipes.liquids.get(fluid.association);
                float temp =  heat.getTemp();
                float energyAvailable = (temp-liquid.boilpoint)*heat.heatcapacity;
                float energyToLiquid = GroundFluidControl.UnitPerLiquid/(liquid.phaseChangeEnergy);
                float amountRemove = energyAvailable*energyToLiquid;
                amountRemove = Math.min(amountRemove,fluidcontrol.fluidAmount(findex));
                if(amountRemove>0){
                    fluidcontrol.removeFluid(findex, amountRemove);
                    heat.heatenergy-=amountRemove/energyToLiquid;
                    if(wasVisible && Mathf.random() < amountRemove*5f){
                        fluid.evaporateEffect.at(this);
                    }
                }
            }
        }

        @Override
        public void unitOn(Unit unit){
            super.unitOn(unit);
            if(timer(timerDump, dumpTime)){
                heatNode().affectUnit(unit,damagemul);
            }
        }

        @Override
        public void draw(){
            float temp = heatNode().getTemp();
            Draw.rect(region, x, y);
            if(temp < HeatGraphNode.celsiusZero || temp > HeatGraphNode.celsiusZero+150){
               heatNode().heatColor(Tmp.c1);
               Draw.color(Tmp.c1.add(baseColor));
               Draw.rect(heat, x, y);
               Draw.color();
            }
            drawTeamTop();
        }
    }
}
