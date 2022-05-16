package unity.world.blocks.exp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.entities.*;
import unity.graphics.*;

import static arc.Core.atlas;

public class ExpTank extends Block {
    public int expCapacity = 600;
    public TextureRegion topRegion, expRegion;

    public ExpTank(String name){
        super(name);
        update = solid = sync = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemCapacity, "@", Core.bundle.format("exp.expAmount", expCapacity));
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("exp", (ExpTankBuild entity) -> new Bar(() -> Core.bundle.get("bar.exp"), () -> UnityPal.exp, entity::expf));
    }

    @Override
    public void load(){
        super.load();
        topRegion = atlas.find(name + "-top");
        expRegion = atlas.find(name + "-exp");
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    public class ExpTankBuild extends Building implements ExpHolder{
        public int exp = 0;

        @Override
        public int getExp(){
            return exp;
        }

        @Override
        public int handleExp(int amount){
            if(amount > 0){
                int e = Math.min(expCapacity - exp, amount);
                exp += e;
                return e;
            }
            else{
                int e = Math.min(-amount, exp);
                exp -= e;
                return -e;
            }
        }

        public float expf(){
            return exp / (float)expCapacity;
        }

        @Override
        public int unloadExp(int amount){
            int e = Math.min(amount, exp);
            exp -= e;
            return e;
        }

        @Override
        public boolean acceptOrb(){
            return true;
        }

        @Override
        public boolean handleOrb(int orbExp){
            return handleExp(orbExp) > 0;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.color(UnityPal.exp, Color.white, Mathf.absin(20, 0.6f));
            Draw.alpha(expf());
            Draw.rect(expRegion, x, y);
            Draw.color();
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            drawPlaceText(exp + "/" + expCapacity, tile.x, tile.y, exp > 0);
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, 25f + 25f * expf(), UnityPal.exp, 0.5f * expf());
        }

        @Override
        public void onDestroyed(){
            ExpOrbs.spreadExp(x, y, exp * 0.8f, 3 * size);
            super.onDestroyed();
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case itemCapacity -> expCapacity;
                case totalItems -> exp;
                default -> super.sense(sensor);
            };
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(exp);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            exp = read.i();
        }
    }
}
