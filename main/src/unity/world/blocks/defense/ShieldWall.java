package unity.world.blocks.defense;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.graphics.*;
import mindustry.ui.Bar;
import mindustry.world.meta.Stat;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.stroke;
import static mindustry.Vars.tilesize;

public class ShieldWall extends LevelLimitWall{
    private final int timerHeal = timers++;
    protected float shieldHealth;
    protected float repair = 50f;
    public TextureRegion topRegion;

    public Effect shieldGen = new Effect(20, e -> {
        color(e.color, e.fin());
        if (Core.settings.getBool("animatedshields")){
            Fill.rect(e.x, e.y, e.fin() * size * 8, e.fin() * size * 8);
        }else{
            stroke(1.5f);
            Draw.alpha(0.09f);
            Fill.rect(e.x - e.fin() * size * 4, e.y - e.fin() * size * 4, e.fin() * size * 8, e.fin() * size * 8);
            Draw.alpha(1f);
            Lines.rect(e.x, e.y, e.fin() * size * 8, e.fin() * size * 8);
        }
    }).layer(Layer.shields);

    public Effect shieldBreak = new Effect(40, e -> {
        color(e.color);
        stroke(3f * e.fout());
        Lines.rect(e.x, e.y, e.fin() * size * 8, e.fin() * size * 8);
    }).followParent(true);

    public Effect shieldShrink = new Effect(20, e -> {
        color(e.color, e.fout());
        if (Core.settings.getBool("animatedshields")){
            Fill.rect(e.x, e.y, e.fout() * size * 8, e.fout() * size * 8);
        }else{
            stroke(1.5f);
            Draw.alpha(0.2f);
            Fill.rect(e.x, e.y, e.fout() * size * 8, e.fout() * size * 8);
            Draw.alpha(1f);
            Lines.rect(e.x, e.y, e.fout() * size * 8, e.fout() * size * 8);
        }
    }).layer(Layer.shields);

    public ShieldWall(String name) {
        super(name);
        update = true;
        flashHit = false;
    }

    @Override
    public void load(){
        super.load();

        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.shieldHealth, shieldHealth);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("shield", (ShieldWallBuild e) -> new Bar("stat.shieldhealth", Pal.accent, () -> e.shieldBroke ? 0f : 1f - e.gotDamage / shieldHealth));
    }

    public class ShieldWallBuild extends LevelLimitWallBuild{
        public boolean shieldBroke = true;
        public float gotDamage, warmup, scl = 0;

        @Override
        public void created(){
            super.created();

            shieldGen.at(x, y);
        }

        @Override
        public void updateTile(){
            super.updateTile();

            warmup = Mathf.lerpDelta(warmup, 1f, 0.05f);
            scl = Mathf.lerpDelta(scl, shieldBroke ? 0f : 1f, 0.05f);

            if (timer(timerHeal, 60f) && shieldBroke && gotDamage > 0){
                gotDamage -= repair * delta();
            }

            if (gotDamage >= shieldHealth && !shieldBroke){
                shieldBroke = true;
                gotDamage = shieldHealth;
                shieldBreak.at(x, y);
            }

            if (shieldBroke && gotDamage <= 0){
                shieldBroke = false;
                gotDamage = 0;
            }

            if (gotDamage < 0) gotDamage = 0;

            if (this.hit > 0) this.hit -= 0.2f * Time.delta;
        }

        @Override
        public void draw(){
            super.draw();

            if(gotDamage > 0f){
                Draw.alpha(gotDamage / shieldHealth * 0.75f);
                Draw.blend(Blending.additive);
                Draw.rect(topRegion, x, y);
                Draw.blend();
                Draw.reset();
            }

            drawShield();
        }

        @Override
        public boolean collide(Bullet b){
            if (b.team != team && b.type.speed > 0.001f && b.type.absorbable){
                b.hit = true;
                b.type.despawnEffect.at(x, y, b.rotation(), b.type.hitColor);

                if (shieldBroke){
                    damage(b.damage);
                }else{
                    handleExp((int) (b.damage * damageExp));
                    setEFields(level());
                    gotDamage += b.damage;
                }
                hit = 1f;

                b.remove();
                return false;
            }
            return super.collide(b);
        }

        @Override
        public void onRemoved(){
            super.onRemoved();

            if (!shieldBroke) shieldShrink.at(x, y);
        }

        public void drawShield(){
            if (!shieldBroke){
                Draw.z(Layer.shields);
                color(team.color, Color.white, Mathf.clamp(hit));

                float radius = this.block.size * tilesize * warmup * scl;

                if(Core.settings.getBool("animatedshields")){
                    Fill.rect(x, y, radius, radius);
                }else{
                    stroke(1.5f);
                    Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
                    Fill.rect(x, y, radius, radius);
                    Draw.alpha(1f);
                    Lines.rect(x - radius / 2, y - radius - 2, radius, radius);
                    Draw.reset();
                }
            }

            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(shieldBroke);
            write.f(gotDamage);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            shieldBroke = read.bool();
            gotDamage = read.f();
            warmup = read.f();
        }
    }
}