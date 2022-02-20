package unity.world.blocks.exp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.entities.*;
import unity.gen.*;
import unity.world.blocks.exp.*;

import static arc.Core.atlas;

public class ExpSource extends Block {
    public int produceTimer = timers++;

    public float reload = 60f;
    public int amount = 100;
    public TextureRegion topRegion;

    public ExpSource(String name){
        super(name);
        update = true;
        solid = rotate = false;
        configurable = true;

        config(Boolean.class, (ExpSourceBuild entity, Boolean b) -> {
            if(b) entity.clicked();
        });
    }

    @Override
    public void load(){
        super.load();
        topRegion = atlas.find(name + "-top");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemCapacity, "@", Core.bundle.format("exp.expAmount", "-Infinity"));
    }

    public class ExpSourceBuild extends Building {
        @Override
        public void updateTile(){
            if(enabled && timer.get(produceTimer, reload)){
                //ExpOrbs.spreadExp(x, y, amount, 6f);
                for(Building b : proximity){
                    if(b instanceof ExpHolder exp) exp.handleExp(99999999);
                }
            }
        }

        @Override
        public void draw(){
            super.draw();
            Draw.blend(Blending.additive);
            Draw.color(Color.white);
            Draw.alpha(Mathf.absin(Time.time, 20, 0.4f));
            Draw.rect(topRegion, x, y);
            Draw.blend();
            Draw.reset();
        }

        @Override
        public void onDestroyed(){
            ExpOrbs.spreadExp(x, y, amount * 5, 8f);
            super.onDestroyed();
        }

        @Override
        public boolean configTapped(){
            configure(true);
            UnitySounds.expChime.at(this);
            return false;
        }

        public void clicked(){
            ExpOrbs.spreadExp(x, y, amount, 6f);
        }
    }
}
