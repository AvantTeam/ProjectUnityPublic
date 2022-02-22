package unity.world.blocks.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import unity.annotations.Annotations.*;
import unity.gen.*;
import unity.graphics.*;
import unity.world.blocks.exp.*;

import static arc.Core.atlas;
import static mindustry.Vars.*;

@Dupe(base = ExpTurret.class, parent = LimitWall.class, name = "ExpLimitWall")
public class LevelLimitWall extends ExpLimitWall {
    public TextureRegion[] levelRegions; //level top regions
    public TextureRegion edgeRegion, shieldRegion, edgeMaxRegion;
    public float damageExp = 1 / 20f;
    public float shieldZ = Layer.buildBeam;

    public Effect updateEffect = Fx.none;
    public float updateChance = 0.01f;

    public LevelLimitWall(String name){
        super(name);
        maxLevel = 6;
        passive = true;
        updateExpFields = false;
        upgradeEffect = Fx.none;
    }

    @Override
    public void init(){
        damageReduction = new EField.EExpoZero(f -> {}, 0.1f, Mathf.pow(8f, 1f / maxLevel), true, null, v -> Strings.autoFixed(Mathf.roundPositive(v * 10000) / 100f, 2) + "%");
        super.init();
    }

    @Override
    public void load(){
        super.load();
        edgeRegion = atlas.find(name + "-under");
        edgeMaxRegion = atlas.find(name + "-under-max", name + "-under");
        shieldRegion = atlas.find(name + "-shield");
        int n = 1;
        while(n <= 100){ //worst-case scenario
            TextureRegion t = atlas.find(name + n);
            if(!t.found()) break;
            n++;
        }
        if(n > 1){
            //name+n-1 was the last sprite that was found
            levelRegions = new TextureRegion[n];
            levelRegions[0] = region;
            for(int i = 1; i < n; i++){
                levelRegions[i] = atlas.find(name + i);
            }
        }
    }

    public class LevelLimitWallBuild extends ExpLimitWallBuild{
        public TextureRegion levelRegion(){
            if(levelRegions == null) return region;
            return levelRegions[Math.min((int)(levelf() * levelRegions.length), levelRegions.length - 1)];
        }

        @Override
        public void draw(){
            TextureRegion top = levelRegion();
            Draw.z(Layer.block);
            Draw.rect(top, x, y);
            //Draw.z(Layer.blockOver);
            if(top != region){
                //Draw.rect(top, x, y);
                Draw.z(Layer.blockUnder - 0.01f);
                if(edgeRegion.found()) Draw.rect(top == levelRegions[levelRegions.length - 1] ? edgeMaxRegion : edgeRegion, x, y);
                if(!state.isPaused() && updateEffect != Fx.none && top == levelRegions[levelRegions.length - 1] && Mathf.chanceDelta(updateChance)) updateEffect.at(x + Mathf.range(size * 4f), y + Mathf.range(size * 4f), UnityPal.exp);
            }

            if(flashHit && hit > 0.0001f){
                Draw.z(Layer.block);
                Draw.color(flashColor);
                Draw.alpha(hit * 0.5f);
                Draw.blend(Blending.additive);
                Fill.rect(x, y, tilesize * size, tilesize * size);
                if(top != region){
                    Draw.z(Layer.blockUnder - 0.01f);
                    Draw.mixcol(Color.white, 1f);
                    Draw.rect(edgeRegion, x, y);
                    Draw.mixcol();
                }
                Draw.blend();
                Draw.reset();

                if(!state.isPaused()){
                    hit = Mathf.clamp(hit - Time.delta / 10f);
                }
            }


            /*if(shieldZ > 0 && shieldRegion.found() && level() == maxLevel){
                Draw.z(shieldZ);
                if(Core.settings.getBool("animatedshields")){
                    Draw.rect(shieldRegion, x, y);
                }
                else{
                    Draw.blend(Blending.additive);
                    Draw.rect(shieldRegion, x, y);
                    Draw.blend();
                }
            }*/
        }

        @Override
        public float handleDamage(float amount){
            float a = amount * damageExp;

            if(a >= 1f) handleExp((int)a);
            else if(a > 0f && Mathf.chance(a)) handleExp(1);
            setEFields(level());
            return super.handleDamage(amount);
        }

        @Override
        public void levelup(){
            upgradeSound.at(this);
            upgradeEffect.at(this);
            if(upgradeBlockEffect != Fx.none) upgradeBlockEffect.at(x, y, 0, Color.white, levelRegion());
        }
    }
}
