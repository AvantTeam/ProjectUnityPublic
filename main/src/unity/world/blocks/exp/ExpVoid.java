package unity.world.blocks.exp;

import arc.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.world.blocks.exp.*;

public class ExpVoid extends Block {
    public int produceTimer = timers++;

    public float reload = 30f;

    public ExpVoid(String name){
        super(name);
        update = solid = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemCapacity, "@", Core.bundle.format("exp.expAmount", "Infinity"));
    }

    public class ExpVoidBuild extends Building implements ExpHolder{
        @Override
        public void updateTile(){
            if(enabled && timer.get(produceTimer, reload)){
                for(Building b : proximity){
                    if(b instanceof ExpHolder exp) exp.unloadExp(99999999);
                }
            }
        }

        @Override
        public int getExp(){
            return 0;
        }

        @Override
        public int handleExp(int amount){
            return amount;
        }

        @Override
        public boolean acceptOrb(){
            return true;
        }

        @Override
        public boolean handleOrb(int orbExp){
            return true;
        }
    }
}
