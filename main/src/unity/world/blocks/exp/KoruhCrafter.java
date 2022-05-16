package unity.world.blocks.exp;

import arc.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.ui.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;
import unity.entities.*;
import unity.graphics.*;

public class KoruhCrafter extends GenericCrafter {
    public int expUse = 2;
    public int expCapacity = 24;
    public boolean ignoreExp = true; //if true, works without exp but takes damage instead

    public float craftDamage = 3.5f; //damage taken per exp when crafting without enough exp
    public Effect craftDamageEffect = Fx.explosion;

    public KoruhCrafter(String name){
        super(name);
        sync = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemCapacity, "@", Core.bundle.format("exp.expAmount", expCapacity));
        if(expUse > 0){
            stats.add(Stat.input, "@ [lightgray]@[]", Core.bundle.format("explib.expAmount", (expUse / craftTime) * 60), StatUnit.perSecond.localized());
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("exp", (KoruhCrafterBuild entity) -> new Bar(() -> Core.bundle.get("bar.exp"), () -> UnityPal.exp, entity::expf));
    }

    public class KoruhCrafterBuild extends GenericCrafterBuild implements ExpHolder{
        public int expc;

        public void lackingExp(int missing){
            //this block is run last so that in the event of a block destruction, no code relies on the block type
            Core.app.post(() -> {
                damage(craftDamage * missing * Mathf.random(0.5f, 1f));
            });
        }

        @Override
        public boolean canConsume(){
            return super.canConsume() && (ignoreExp || expc >= expUse);
        }

        @Override
        public void consume(){
            super.consume();
            int a = Math.min(expUse, expc);
            expc -= a;
            if(a < expUse){
                lackingExp(expUse - a);
                craftDamageEffect.at(this);
            }
        }

        @Override
        public int getExp(){
            return expc;
        }

        @Override
        public int handleExp(int amount){
            if(amount > 0){
                int e = Math.min(expCapacity - expc, amount);
                expc += e;
                return e;
            }
            else{
                int e = Math.min(-amount, expc);
                expc -= e;
                return -e;
            }
        }

        public float expf(){
            return expc / (float)expCapacity;
        }

        @Override
        public int unloadExp(int amount){
            int e = Math.min(amount, expc);
            expc -= e;
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
        public void drawSelect(){
            super.drawSelect();
            drawPlaceText(expc + "/" + expCapacity, tile.x, tile.y, expc >= expUse);
        }

        @Override
        public void onDestroyed(){
            ExpOrbs.spreadExp(x, y, expc * 0.3f, 3 * size);
            super.onDestroyed();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(expc);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            expc = read.i();
        }
    }
}
