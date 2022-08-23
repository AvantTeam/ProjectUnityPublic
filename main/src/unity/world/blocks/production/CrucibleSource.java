package unity.world.blocks.production;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.*;
import unity.world.blocks.*;
import unity.world.blocks.production.CruciblePump.*;
import unity.world.blocks.production.GenericCaster.*;
import unity.world.meta.*;
import unity.world.meta.CrucibleRecipes.*;

import static mindustry.Vars.content;

public class CrucibleSource extends GenericGraphBlock{
    public CrucibleSource(String name){
        super(name);
        configurable = true;
        config(Item.class, (CrucibleSourceBuild tile, Item item) -> tile.config = CrucibleRecipes.items.get(item));
        config(Liquid.class, (CrucibleSourceBuild tile, Liquid item) -> tile.config = CrucibleRecipes.liquids.get(item));
        config(Integer.class, (CrucibleSourceBuild tile, Integer item) -> tile.config = CrucibleRecipes.ingredients.get(item));
        configClear((CrucibleSourceBuild tile) -> tile.config = null);
    }

    public class CrucibleSourceBuild extends GenericGraphBuild{
        CrucibleIngredient config;

        @Override
        public void updateTile(){
            super.updateTile();
            if(config != null){

                crucibleNode().getFluid(config).solid = 0;
                crucibleNode().getFluid(config).melted = crucibleNode().capacity;
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(config == null ? -1 : config.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            config = CrucibleRecipes.ingredients.get(read.s());
        }

        @Override
        public void buildConfiguration(Table table){
            Seq<UnlockableContent> contents = new Seq<>();
            contents.addAll(content.items().select(CrucibleRecipes.items::containsKey));
            contents.addAll(content.liquids().select(CrucibleRecipes.liquids::containsKey));
            ItemSelection.buildTable(CrucibleSource.this, table, contents, () -> {
                if(config instanceof CrucibleItem i){
                    return i.item;
                }
                if(config instanceof CrucibleLiquid i){
                    return i.liquid;
                }
                return null;
            }, this::configure);
        }

        @Override
        public Integer config(){
            return config.id;
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other){
                deselect();
                configure(null);
                return false;
            }

            return true;
        }

        @Override
        public void draw(){
            Draw.color(config==null? Pal.gray:config.color);
            Fill.rect(x,y,8,8);
            Draw.color();
            Draw.rect(region,x,y);
            drawTeamTop();
        }
    }
}
