package unity.world.blocks.exp;

import arc.Core;
import arc.math.Mathf;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.ui.Bar;
import mindustry.world.blocks.power.ImpactReactor;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import unity.entities.ExpOrbs;
import unity.graphics.UnityPal;

public class KoruhReactor extends ImpactReactor{
    public int expUse = 2;
    public int expCapacity = 24;
    public boolean ignoreExp = true;

    public float reactDamage = 3.5f;
    public Effect damageEffect = Fx.explosion;

    public KoruhReactor(String name) {
        super(name);
        sync = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemCapacity, "@", Core.bundle.format("exp.expAmount", expCapacity));
        if(expUse > 0){
            stats.add(Stat.input, "@ [lightgray]@[]", Core.bundle.format("explib.expAmount", (expUse / itemDuration) * 60), StatUnit.perSecond.localized());
        }
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("exp", (KoruhReactor.KoruhReactorBuild entity) -> new Bar(() -> Core.bundle.get("bar.exp"), () -> UnityPal.exp, entity::expf));
    }

    public class KoruhReactorBuild extends ImpactReactorBuild implements ExpHolder{
        public int exp;

        @Override
        public int getExp(){
            return exp;
        }

        /* from KoruhCrafter */
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

        @Override
        public int unloadExp(int amount){
            int e = Math.min(amount, exp);
            exp -= e;
            return e;
        }

        @Override
        public boolean canConsume(){
            return super.canConsume() && (ignoreExp || exp >= expUse);
        }

        @Override
        public boolean acceptOrb() { return true; }

        @Override
        public boolean handleOrb(int orbExp){
            return handleExp(orbExp) > 0;
        }

        @Override
        public void consume(){
            super.consume();

            int consumeExp = Math.min(expUse, exp);
            exp -= consumeExp;

            if (consumeExp < expUse){
                takeDamage(expUse - consumeExp);
                damageEffect.at(x, y);
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            drawPlaceText(exp + "/" + expCapacity, tile.x, tile.y, exp >= expUse);
        }

        @Override
        public void onDestroyed(){
            ExpOrbs.spreadExp(x, y, exp * 0.3f, 3 * size);
            super.onDestroyed();
        }

        public void takeDamage(int lack){
            Core.app.post(() -> {
                damage(reactDamage * lack * Mathf.random(0.5f, 1f));
            });
        }

        public float expf(){
            return exp / (float)expCapacity;
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
