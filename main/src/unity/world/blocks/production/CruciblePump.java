package unity.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;

import static mindustry.Vars.*;

public class CruciblePump extends GenericGraphBlock{
    TextureRegion floor, base, arrow;

    public CruciblePump(String name){
        super(name);
        configurable = true;
        config(Item.class, (CruciblePumpBuild tile, Item item) -> tile.config = item);
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
        Item config;

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(CruciblePump.this, table, content.items(), () -> config, this::configure);
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
        public void updateTile(){
            var crucibleNode = crucibleNode();
            var torque = getGraph(TorqueGraph.class);

            crucibleNode.accessConnector = crucibleNode.connector.first();

            super.updateTile();

            float eff = Mathf.curve(torque.lastVelocity,0,50)*0.2f;
            if(front()==null){
                if(config!=null){
                    var f = crucibleNode.getFluid(config);
                    float remove = Mathf.clamp(eff * f.melted + 0.001f,0,f.melted);
                    f.melted-=remove;
                    Puddles.deposit(frontTile(), this.tile, Liquids.slag, remove*8);
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
        public Item config(){
            return config;
        }

        @Override
        public void draw(){
            var torque = getGraph(TorqueGraph.class);
            Draw.rect(floor, x, y);
            Draw.color(crucibleNode().getColor());
            Fill.rect(x, y, tilesize, tilesize);
            Draw.color();
            Draw.rect(base, x, y, -get2SpriteRotation());
            if(config != null){
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
            config = content.item(read.s());
        }
    }
}