package unity.world.blocks.exp;

import mindustry.graphics.*;
import unity.annotations.Annotations.*;
import unity.gen.*;
import unity.world.draw.*;

/**
 * handles correct exp tower & orb input, as the KoruhCrafter and Expturret clash
 */
@Dupe(base = ExpTurret.class, parent = KoruhCrafter.class)
public class LevelKoruhCrafter extends ExpKoruhCrafter {
    public int expGain = 2;

    public LevelKoruhCrafter(String name){
        super(name);
        passive = true;
        maxLevel = 3;
        draw = new DrawOver(Layer.blockOver - 1);
    }

    public class LevelKoruhCrafterBuild extends ExpKoruhCrafterBuild {
        //all exp transactions use expc
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

        @Override
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
        public void craft(){
            super.craft();
            incExp(expGain, false);
        }
    }
}
