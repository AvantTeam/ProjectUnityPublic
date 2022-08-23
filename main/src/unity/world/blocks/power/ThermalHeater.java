package unity.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.world.blocks.*;

public class ThermalHeater extends GenericGraphBlock{
    public final TextureRegion[] regions = new TextureRegion[4];
    public TextureRegion heatRegion;
    public final Attribute attri = Attribute.heat;

    public ThermalHeater(String name){
        super(name);

        rotate = true;
    }

    @Override
    public void load(){
        super.load();

        for(int i = 0; i < 4; i++) regions[i] = Core.atlas.find(name + (i + 1));
        heatRegion = Core.atlas.find(name + "-heat");
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return tile.getLinkedTilesAs(this, tempTiles).sumf(other -> other.floor().attributes.get(attri)) > 0.01f;
    }

    public class ThermalHeaterBuild extends GenericGraphBuild{
        public float sum;

        @Override
        public void updateTile(){
            super.updateTile();
            heatNode().efficency = sum + attri.env(); /// <---------------
        }

        @Override
        public void draw(){
            Draw.rect(regions[rotation], x, y);
            UnityDrawf.drawHeat(heatRegion, x, y, rotdeg(), heatNode().getTemp()); /// <---------------

            drawTeamTop();
        }

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();

            sum = sumAttribute(attri, tileX(), tileY());
        }
    }
}
