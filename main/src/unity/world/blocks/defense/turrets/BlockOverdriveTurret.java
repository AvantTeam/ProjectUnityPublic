package unity.world.blocks.defense.turrets;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.Tmp;
import mindustry.gen.*;
import mindustry.entities.Units;
import mindustry.graphics.*;
import mindustry.world.blocks.defense.turrets.ReloadTurret;
import mindustry.world.meta.*;
import unity.content.*;
import unity.entities.bullet.misc.BlockStatusEffectBulletType;
import unity.graphics.UnityPal;
import unity.world.blocks.exp.ExpHolder;

import static mindustry.Vars.tilesize;

public class BlockOverdriveTurret extends ReloadTurret {
    public final int timerBullet = timers++;

    public float buffRange = 50f;
    public float buffReload = 180f;
    public float phaseBoost = 3f;
    public float phaseRangeBoost = 1.5f;
    public float phaseExpBoost = 2f;

    public TextureRegion baseRegion, laserRegion, laserEndRegion;
    public BlockStatusEffectBulletType bullet = (BlockStatusEffectBulletType) UnityBullets.statusEffect;

    public BlockOverdriveTurret(String name) {
        super(name);

        hasPower = hasItems = update = solid = outlineIcon = true;
        flags = EnumSet.of(BlockFlag.turret);
        group = BlockGroup.projectors;
        canOverdrive = false;
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
        Drawf.dashCircle(x * tilesize, y * tilesize, buffRange, Pal.accent);
        Draw.reset();
    }

    public class BlockOverdriveTurretBuild extends ReloadTurretBuild{
        public Building target;
        public float buffingTime, phaseHeat, targetTime;
        public boolean buffing, isExp;

        @Override
        public void drawSelect(){
            Drawf.circles(x, y, buffRange, Pal.accent);

            if (buffing) Drawf.selected(target, isExp ? UnityPal.exp.a(Mathf.absin(6f, 1f)) : Tmp.c1.set(Pal.heal).lerp(Color.valueOf("feb380"), Mathf.absin(9f, 1f)).a(Mathf.absin(6f, 1f)));
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Draw.z(Layer.turret);
            Drawf.shadow(region, x - (size/2f), y - (size/2f), rotation - 90);
            Draw.rect(region, x, y, rotation - 90);

            if (buffing){
                float angle = angleTo(target);
                float len = 5;
                Draw.color(isExp ? UnityPal.exp : Tmp.c2.set(Color.valueOf("feb380")).lerp(Pal.heal, Mathf.absin(10f, 1f)));
                Draw.alpha(1f);
                Draw.z(Layer.block + 1);
                Drawf.laser(team, laserRegion, laserEndRegion, x + Angles.trnsx(angle, len), y + Angles.trnsy(angle, len), target.x, target.y, bullet.strength / 4f);
                Draw.color();
            }
        }

        @Override
        public void updateTile(){
            phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(hasItems && !items.empty()), 0.1f);
            float radius = buffRange + phaseHeat * phaseRangeBoost;
            buffing = false;

            if (target != null){
                isExp = target instanceof ExpHolder;
                if (!targetValid(target)){
                    target = null;
                }else if (consValid() && enabled){
                    if (timer(timerBullet, buffReload)){
                        bullet.create(this, target.x, target.y, 0f);
                        timer.reset(timerBullet, 0);
                    }
                    rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.5f);
                    buffing = true;
                }
                targetTime = 0f;
            }

            if (cons.optionalValid() && efficiency() > 0){
                buffingTime += edelta();
                if (buffingTime >= buffReload) {
                    consume();
                    buffingTime = 0f;
                }
            }

            if (consValid()){
                targetTime += edelta();
                if (targetTime >= buffReload){
                    target = Units.closestBuilding(team, x, y, radius, this::targetValid);
                    targetTime = 0f;
                }
            }
        }

        @Override
        public boolean shouldConsume(){
            return target != null && enabled;
        }

        public boolean targetValid(Building b){
            return b.isValid() && b.block.canOverdrive && b != this && !proximity.contains(b) && !isBeingBuffed(b) && b.enabled;
        }

        public boolean isBeingBuffed(Building b){
            Seq<Bullet> bullets = Groups.bullet.intersect(b.x, b.y, b.block.size * 8, b.block.size * 8);

            if (bullets.size > 0) return bullets.get(0).owner != this;
            return false;
        }
    }
}