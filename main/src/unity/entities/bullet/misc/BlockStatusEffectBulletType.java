package unity.entities.bullet.misc;

import arc.math.Mathf;
import arc.util.Time;
import unity.entities.ExpOrbs;
import unity.world.blocks.defense.turrets.BlockOverdriveTurret;
import unity.world.blocks.defense.turrets.BlockOverdriveTurret.*;
import unity.world.blocks.exp.ExpHolder;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.*;

public class BlockStatusEffectBulletType extends BasicBulletType {
    public float strength = 2f;
    public int amount = 3;
    public boolean upgrade = false;
    Building target = null;
    boolean buffing = false;
    float phaseHeat = 0;
    float phaseBoost = 0;
    float phaseExpBoost = 0;
    float efficiency = 0;

    public BlockStatusEffectBulletType(float speed, float damage) { super(speed, damage); }

    @Override
    public void draw(Bullet b) {
        //no
    }

    @Override
    public void update(Bullet b) {
        if (b.owner instanceof BlockOverdriveTurretBuild bb) {
            target = bb.target;
            buffing = bb.buffing;
            phaseHeat = bb.phaseHeat;
            phaseBoost = ((BlockOverdriveTurret) bb.block).phaseBoost;
            phaseExpBoost = ((BlockOverdriveTurret) bb.block).phaseExpBoost;
            efficiency = bb.efficiency();
        }

        if (buffing) {
            if (b.x == target.x && b.y == target.y) {
                strength = Mathf.lerpDelta(strength, 3f + phaseHeat * phaseBoost, 0.02f);
                if (b.timer(0, 179f)) {
                    if (upgrade) addExp(target, (5 + phaseBoost * phaseExpBoost) * Time.delta * efficiency);
                    else buff(target, (strength + phaseHeat * phaseBoost) * Time.delta * efficiency);
                }
            }
        }else{
            strength = 1f;
        }
    }

    public void buff(Building b, float intensity) {
        if (b.health < b.maxHealth) {
            b.applyBoost(intensity, 180f);
            b.heal(intensity);
        } else {
            b.applyBoost(intensity * 2, 180f);
        }
    }

    public void addExp(Building b, float intensity) {
        if (b instanceof ExpHolder exp) {
            exp.handleExp(Mathf.round(exp.getExp() * 0.1f) / 10 * Mathf.round(intensity));
            ExpOrbs.spreadExp(b.x, b.y, amount);
        }
    }
}