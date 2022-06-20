package unity.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.content.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;

//add particle fx later
public class HeatRadiator extends GenericGraphBlock{
    TextureRegion[] rotateregions;
    TextureRegion heat;
    public HeatRadiator(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        rotateregions = new TextureRegion[2];
        rotateregions[0] = Core.atlas.find(name+""+1);
        rotateregions[1] = Core.atlas.find(name+""+2);
        heat = Core.atlas.find(name+"-heat");
    }

    public class HeatRadiatorBuild extends GenericGraphBuild{
        @Override
        public void updateTile(){
            super.updateTile();
            float likely = Mathf.map(heatNode().getTemp(), HeatGraphNode.celsiusZero,HeatGraphNode.celsiusZero+1800,0,0.01f);
            if(Mathf.random()<likely){
                Fx.steam.at(x + Mathf.range(8), y + Mathf.range(8));
            }
        }

        @Override
        public void draw(){
            Draw.rect(rotateregions[rotation%2],x,y);
            UnityDrawf.drawHeat(heat,x,y,rotdeg(),heatNode().getTemp());
            drawTeamTop();
        }
    }
}
