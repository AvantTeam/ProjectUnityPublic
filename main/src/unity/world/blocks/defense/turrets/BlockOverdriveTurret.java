package unity.world.blocks.defense.turrets;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.world.blocks.exp.*;

import static mindustry.Vars.*;

public class BlockOverdriveTurret extends ReloadTurret {
    public Color overdrive = Color.valueOf("feb380");
    public BulletType bullet;

    public float buffRange = 50f;
    public float consumeReload = 360f;
    public float boostReload = 30f;
    public float strength, healSpeed, expSpeed;

    public TextureRegion baseRegion, laserRegion, laserEndRegion;

    public BlockOverdriveTurret(String name) {
        super(name);

        hasPower = update = solid = outlineIcon = true;
        flags = EnumSet.of(BlockFlag.turret);
        group = BlockGroup.projectors;
        canOverdrive = hasItems = false;
    }

    @Override
    public void load(){
        super.load();
        baseRegion = Core.atlas.find(name + "-base");
        laserRegion = Core.atlas.find("exp-laser");
        laserEndRegion = Core.atlas.find("exp-laser-end");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, buffRange / tilesize, StatUnit.blocks);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Draw.color(UnityPal.exp.a(Mathf.absin(6f, 1f)));
        Fill.poly(x, y, Lines.circleVertices(buffRange), range);
        Draw.reset();
    }

    public class BlockOverdriveTurretBuild extends ReloadTurretBuild{
        public Building target, lastTarget;
        public float consumeTime, targetTime, boostTime = 0f;
        public float warmup = 0f, intensity, intensityLimit;
        public boolean buffing, isExp = false;

        @Override
        public void drawSelect(){
            Drawf.circles(x, y, buffRange, Pal.accent);

            if (buffing) Drawf.selected(target, target instanceof ExpHolder ? UnityPal.exp.a(Mathf.absin(6f, 1f)) : Tmp.c1.set(Pal.heal).lerp(overdrive, Mathf.absin(9f, 1f)).a(Mathf.absin(6f, 1f)));
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Draw.z(Layer.turret);
            Drawf.shadow(region, x - (size/2f), y - (size/2f), rotation - 90);
            Draw.rect(region, x, y, rotation - 90);

            if (lastTarget != null){
                float angle = angleTo(lastTarget);
                float len = 5;
                Draw.color(lastTarget instanceof ExpHolder ? UnityPal.exp : Tmp.c2.set(overdrive).lerp(Pal.heal, Mathf.absin(10f, 1f)));
                Draw.alpha(1f);
                Draw.z(Layer.block + 1);
                Drawf.laser(team, laserRegion, laserEndRegion, x + Angles.trnsx(angle, len), y + Angles.trnsy(angle, len), lastTarget.x, lastTarget.y, strength / 4f);
                Draw.color();
            }
        }

        @Override
        public void updateTile(){
            warmup = Mathf.lerpDelta(warmup, efficiency() > 0 ? 1f : 0f, 0.1f);
            intensity = Mathf.lerpDelta(intensity, target != null && buffing ? intensityLimit : 0f, 0.06f * Time.delta);

            if (target != null){
                if (!targetValid(target)) target = null;
                lastTarget = target;
            } else if (intensity <= 0.01f) lastTarget = null;

            targetTime += warmup;
            if (targetTime >= reloadTime){
                target = Units.closestBuilding(team, x, y, realRad(), this::targetValid);
                targetTime = 0f;
                if (target != null){
                    isExp = target instanceof ExpHolder;
                    buffing = true;
                    intensityLimit = isExp ? 1f : 1.5f;
                }
            }

            if (efficiency() > 0){
                if (target != null){
                    boostTime += warmup;
                    rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.5f * Time.delta);

                    target.heal(healSpeed * warmup * Time.delta);
                    if (boostTime >= boostReload){
                        target.applyBoost(strength * intensity, boostReload + 1f);
                        boostTime = 0f;
                    }
                    if (isExp) ((ExpHolder)target).handleExp((int) (expSpeed * warmup * Time.delta));
                }

                consumeTime += edelta();
                if (consumeTime >= consumeReload) {
                    consume();
                    consumeTime = 0f;
                } else {
                    buffing = false;
                }
            }
        }

        @Override
        public boolean shouldConsume(){
            return target != null && enabled;
        }

        public boolean targetValid(Building b){
            return b.isValid() && b.block.canOverdrive && !proximity.contains(b) && !isBuffed(b) && b.enabled;
        }

        public boolean isBuffed(Building b){
            Seq<Bullet> bullets = Groups.bullet.intersect(b.x, b.y, b.block.size * 8, b.block.size * 8);

            if (bullets.size > 0) return bullets.get(0).owner != this;
            return false;
        }

        public float realRad(){
            return buffRange * warmup;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(rotation);
            write.f(consumeTime);
        }

        @Override
        public void read(Reads read){
            super.read(read);

            rotation = read.f();
            consumeTime = read.f();
        }
    }
}