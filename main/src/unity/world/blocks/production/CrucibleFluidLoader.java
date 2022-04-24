package unity.world.blocks.production;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.type.*;
import unity.world.blocks.*;
import unity.world.meta.*;

public class CrucibleFluidLoader extends GenericGraphBlock{
    TextureRegion top[];
    TextureRegion bottom;
    public CrucibleFluidLoader(String name){
        super(name);
        hasLiquids = true;
    }

    @Override
    public void load(){
        super.load();
        top = new TextureRegion[4];
        top[0] = loadTex("top1");
        top[1] = loadTex("top2");
        top[2] = loadTex("top3");
        top[3] = loadTex("top4");
        bottom = loadTex("bottom");
    }

    public class CrucibleFluidLoaderBuild extends GenericGraphBuild{

        @Override
        public void updateTile(){
            super.updateTile();

            var cr = CrucibleRecipes.liquids.get(liquids.current());
            if(cr!=null && liquids.get(liquids.current())>=0){
                float wentin = crucibleNode().addLiquidIngredient(cr,liquids.get(liquids.current()));
                liquids.remove(liquids.current(),wentin);
            }
            ;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return hasLiquids && (liquids.current() == liquid|| liquids.currentAmount() < 0.2f) && liquids.get(liquid)<liquidCapacity && CrucibleRecipes.liquids.get(liquid)!=null;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            super.handleLiquid(source, liquid, amount);
        }

        @Override
        public void draw(){
            Draw.rect(bottom,x,y);
            Draw.color(crucibleNode().getColor());
            Fill.rect(x,y,size*8,size*8);
            Draw.color();
            Draw.rect(top[rotation],x,y);

            drawTeamTop();
        }
    }
}
