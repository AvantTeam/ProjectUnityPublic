package unity.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import unity.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.blocks.production.CrucibleSource.*;
import unity.world.graph.*;
import unity.world.meta.*;
import unity.world.meta.CrucibleRecipes.*;
import unity.world.systems.*;

import static mindustry.Vars.*;

public class CruciblePump extends GenericGraphBlock{
    TextureRegion floor, base, arrow;

    public CruciblePump(String name){
        super(name);
        configurable = true;
        config(Item.class, (CruciblePumpBuild tile, Item item) -> tile.config = CrucibleRecipes.items.get(item));
        config(Liquid.class, (CruciblePumpBuild tile, Liquid item) -> tile.config = CrucibleRecipes.liquids.get(item));
        config(Integer.class, (CruciblePumpBuild tile, Integer item) -> tile.config = CrucibleRecipes.ingredients.get(item));
        configClear((CruciblePumpBuild tile) -> tile.config = null);
    }

    @Override
    public void load(){
        super.load();
        floor = loadTex("floor");
        base = loadTex("base");
        arrow = loadTex("arrow");
    }

    public class CruciblePumpBuild extends GenericGraphBuild{
        CrucibleIngredient config;

        @Override
        public void buildConfiguration(Table table){
            Seq<UnlockableContent> contents = new Seq<>();
            contents.addAll(content.items().select(CrucibleRecipes.items::containsKey));
            contents.addAll(content.liquids().select(CrucibleRecipes.liquids::containsKey));
            ItemSelection.buildTable(CruciblePump.this, table, contents, () -> {
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
        public boolean onConfigureBuildTapped(Building other){
            if(this == other){
                deselect();
                configure(null);
                return false;
            }

            return true;
        }

        @Override
        public void updateTile(){
            var crucibleNode = crucibleNode();
            var torque = getGraph(TorqueGraph.class);

            crucibleNode.accessConnector = crucibleNode.connector.first();

            super.updateTile();

            float eff = Mathf.curve(torque.lastVelocity,0,50)*0.2f;
            if(front()==null){
                if(config!=null){
                    var f = crucibleNode.getFluid(config);
                    float remove = Mathf.clamp(eff>0?(eff * f.melted + 0.001f):0,0,f.melted);
                    f.melted-=remove;
                    Liquid liquid = Liquids.slag;
                    if(f.getIngredient() instanceof CrucibleLiquid cl){
                        liquid = cl.liquid;
                    }

                    if( GroundFluidControl.supportsLiquid(liquid)){
                        Unity.groundFluidControl.addFluid(liquid,frontTile(),remove*GroundFluidControl.UnitPerLiquid);
                        //todo: make this delta time agnostic. A minilag spike causes a huge wave.
                    }else{
                        Puddles.deposit(frontTile(), this.tile, liquid, remove * 8);
                    }
                }
            }else if(front() instanceof GraphBuild gb){
                if(crucibleNode.connector.get(1).isConnected(gb)){
                    var other  = gb.crucibleNode();
                    if(config!=null){
                        var f = crucibleNode.getFluid(config);
                        var of  = other.getFluid(config);
                        float remove = Mathf.clamp(eff * f.melted + 0.001f,0,Math.min(f.melted,other.capacity-of.total()));
                        f.melted-=remove;
                        of.melted+=remove;
                    }
                }
            }
        }
        public Tile frontTile() {
            int trns = this.block.size / 2 + 1;
            return Vars.world.tile(this.tile.x + Geometry.d4(this.rotation).x * trns, this.tile.y + Geometry.d4(this.rotation).y * trns);
        }

        @Override
        public Integer config(){
            return config==null?-1:config.id;
        }

        @Override
        public void draw(){
            var torque = getGraph(TorqueGraph.class);
            Draw.rect(floor, x, y);
            Draw.color(crucibleNode().getColor());
            Fill.rect(x, y, tilesize, tilesize);
            Draw.color();
            Draw.rect(base, x, y, -get2SpriteRotation());
            if(config != null && torque!=null){
                Draw.color(config.color);
                UnityDrawf.drawRectOffsetHorz(arrow, x, y,arrow.width*Draw.scl,arrow.height*Draw.scl, rotdeg(),(torque.rotation*0.5f/360f)%1f);
                Draw.color();
            }
            drawTeamTop();
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
    }
}
