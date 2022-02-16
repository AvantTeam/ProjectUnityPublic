package unity.world.blocks.defense;

import arc.util.*;
import mindustry.entities.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.meta.*;
import unity.content.effects.*;

import static arc.Core.*;

public class LimitWall extends Wall{
    protected Effect maxDamageFx = UnityFx.maxDamageFx, withstandFx = UnityFx.withstandFx, blinkFx = UnityFx.blinkFx;
    protected float maxDamage = 30f, over9000 = 90000000, blinkFrame = -1f;

    public LimitWall(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();
        //if(maxDamage > 0f && blinkFrame > 0f) stats.add(Stat.abilities, "@\n@", bundle.format("stat.unity.maxdamage", maxDamage), bundle.format("stat.unity.blinkframe", blinkFrame));
        if(maxDamage > 0f) stats.add(Stat.abilities, "@", bundle.format("stat.unity.maxdamage", maxDamage));
        if(blinkFrame > 0f) stats.add(Stat.abilities, "@", bundle.format("stat.unity.blinkframe", blinkFrame));
    }

    public class LimitWallBuild extends WallBuild{
        protected float blink;

        @Override
        public float handleDamage(float amount){
            if(blinkFrame > 0f){
                if(Time.time - blink >= blinkFrame){
                    blink = Time.time;
                    blinkFx.at(x, y, size);
                }else return 0;
            }
            if(maxDamage > 0f && amount > maxDamage && amount < over9000){
                withstandFx.at(x, y, size);
                return super.handleDamage(Math.min(amount, maxDamage));
            }
            return super.handleDamage(amount);
        }
    }
}
