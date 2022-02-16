package unity.world.blocks.production;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.*;
import unity.world.blocks.*;
import unity.world.blocks.production.CruciblePump.*;
import unity.world.blocks.production.GenericCaster.*;
import unity.world.meta.*;

import static mindustry.Vars.content;

public class CrucibleSource extends GenericGraphBlock{
    public CrucibleSource(String name){
        super(name);
        configurable = true;
        config(Item.class, (CrucibleSourceBuild tile, Item item) -> tile.config = item);
        configClear((CrucibleSourceBuild tile) -> tile.config = null);
    }

    public class CrucibleSourceBuild extends GenericGraphBuild{
        Item config;

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
            config = content.item(read.s());
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(CrucibleSource.this, table, content.items().select(CrucibleRecipes.items::containsKey), () -> config, this::configure);
        }

        @Override
        public Item config(){
            return config;
        }

        @Override
        public boolean onConfigureTileTapped(Building other){
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
