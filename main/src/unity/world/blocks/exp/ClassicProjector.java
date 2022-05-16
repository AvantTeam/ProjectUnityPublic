package unity.world.blocks.exp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.content.effects.*;
import unity.gen.*;
import unity.graphics.*;

import static arc.Core.atlas;
import static mindustry.Vars.tilesize;

@Dupe(base = ExpTurret.class, parent = ForceProjector.class, name = "ExpForceProjector")
public class ClassicProjector extends ExpForceProjector {
    public float deflectChance = 0f;
    public Effect deflectEffect = UnityFx.deflect;
    public Effect absorbEffect = UnityFx.absorb;
    public Effect shieldBreakEffect = UnityFx.shieldBreak;

    public float expChance = 1f;
    public int expGain = 1;
    public TextureRegion altRegion;

    public @Nullable Consume itemConsumer, coolantConsumer;

    public ClassicProjector(String name){
        super(name);
    }

    @Override
    public float getRange(){
        return radius;
    }

    @Override
    public void init(){
        coolantConsumer = findConsumer(c -> c instanceof ConsumeLiquidBase);

        super.init();
        if(effectColors == null) effectColors = new Color[]{fromColor};
    }

    @Override
    public void load(){
        super.load();
        altRegion = atlas.find(name + "-1");
    }

    @Override
    public void setStats(){
        super.setStats();
        if(deflectChance > 0f) stats.add(Stat.baseDeflectChance, deflectChance, StatUnit.none);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        drawPotentialLinks(x, y);

        if(rangeStart != rangeEnd) Drawf.circles(x * tilesize + offset, y * tilesize + offset, rangeEnd, UnityPal.exp);
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, rangeStart, fromColor);

        if(!valid && pregrade != null) drawPlaceText(Core.bundle.format("exp.pregrade", pregradeLevel, pregrade.localizedName), x, y, false);
    }

    public class ClassicProjectorBuild extends ExpForceProjectorBuild{
        public void hitBullet(Bullet b, float r){
            if(!b.type.absorbable || b.team == team || dst2(b) > r * r) return;
            if(b.type.reflectable && deflectChance > 0f && Mathf.chance(deflectChance / b.damage())){
                //deflecc
                float a = b.angleTo(this);
                float rb = b.rotation();
                if(Angles.near(a, rb, 90f)){
                    //deflect angle
                    b.trns(-b.vel.x, -b.vel.y);
                    b.rotation(rb + 2 * (a - rb) + 180f);
                }
                b.owner = this;
                b.team = team;
                b.time += 1f;
                deflectEffect.at(b.x, b.y, angleTo(b), effectColor());
            }
            else{
                //absorbb
                b.absorb();
                absorbEffect.at(b.x, b.y, 0f, effectColor());
            }
            hit = 1f;
            buildup += b.damage();

            if(Mathf.chance(expChance)) handleExp(expGain);
        }

        @Override
        public float realRadius(){
            return ((rangeField == null ? radius : rangeField.fromLevel(level())) + phaseHeat * phaseRadiusBoost) * radscl;
        }

        @Override
        public void onRemoved(){
            float radius = realRadius();
            if(!broken && radius > 1f) UnityFx.forceShrink.at(x, y, radius, effectColor());
        }

        @Override
        public void updateTile(){
            boolean phaseValid = hasItems && itemConsumer != null && itemConsumer.efficiency(this) > 0;

            phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(phaseValid), 0.1f);

            if(phaseValid && !broken && timer(timerUse, phaseUseTime) && efficiency() > 0){
                consume();
            }

            radscl = Mathf.lerpDelta(radscl, broken ? 0f : warmup, 0.05f);

            if(Mathf.chanceDelta(buildup / shieldHealth * 0.1f)){
                Fx.reactorsmoke.at(x + Mathf.range(tilesize / 2f), y + Mathf.range(tilesize / 2f));
            }

            warmup = Mathf.lerpDelta(warmup, efficiency(), 0.1f);

            if(buildup > 0){
                float scale = !broken ? cooldownNormal : cooldownBrokenBase;

                if(hasLiquids && coolantConsumer != null){
                    if(coolantConsumer.efficiency(this) > 0){
                        coolantConsumer.update(this);
                        scale *= (cooldownLiquid * (1f + (liquids.current().heatCapacity - 0.4f) * 0.9f));
                    }
                }

                buildup -= delta() * scale;
            }

            if(broken && buildup <= 0){
                broken = false;
            }

            if(buildup >= shieldHealth + phaseShieldBoost * phaseHeat && !broken){
                broken = true;
                buildup = shieldHealth;
                shieldBreakEffect.at(x, y, realRadius(), effectColor());
            }

            if(hit > 0f){
                hit -= 1f / 5f * Time.delta;
            }

            float realRadius = realRadius();

            if(realRadius > 0 && !broken){
                Groups.bullet.intersect(x - realRadius, y - realRadius, realRadius * 2f, realRadius * 2f, b -> hitBullet(b, realRadius));
            }
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            if(altRegion.found()){
                Draw.alpha(levelf());
                Draw.rect(altRegion, x, y);
                Draw.color();
            }

            if(buildup > 0f){
                Draw.alpha(buildup / shieldHealth * 0.75f);
                Draw.mixcol(shootColor(Tmp.c1), 1f);
                Draw.blend(Blending.additive);
                Draw.rect(topRegion, x, y);
                Draw.blend();
                Draw.reset();
            }

            drawShield();
        }

        @Override
        public void drawShield(){
            if(!broken){
                float radius = realRadius();

                Draw.z(Layer.shields);

                Draw.color(shootColor(Tmp.c2), Color.white, Mathf.clamp(hit));

                if(Core.settings.getBool("animatedshields")){
                    Fill.poly(x, y, Lines.circleVertices(radius), radius);
                }else{
                    Lines.stroke(1.5f);
                    Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
                    Fill.circle(x, y, radius);
                    Draw.alpha(1f);
                    Lines.circle(x, y, radius);
                    Draw.reset();
                }
            }

            Draw.reset();
        }
    }
}
