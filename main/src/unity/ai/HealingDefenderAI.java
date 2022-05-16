package unity.ai;

import arc.math.*;
import arc.math.geom.*;
import mindustry.ai.types.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

/** @author EyeOfDarkness */
public class HealingDefenderAI extends DefenderAI{
    @Override
    public void updateTargeting(){
        if(unit.hasWeapons()){
            updateWeapons();
        }else{
            super.updateTargeting();
        }
    }

    @Override
    public void updateWeapons(){
        float rotation = unit.rotation - 90;
        boolean ret = retarget();
        if(ret){
            target = findTarget(unit.x, unit.y, unit.range(), true, true);
            if(invalid(target)) target = null;
        }
        unit.isShooting = false;
        for(WeaponMount mount : unit.mounts){
            Weapon weapon = mount.weapon;

            if(!weapon.controllable || weapon.bullet.healPercent <= 0f) continue;

            float mountX = unit.x + Angles.trnsx(rotation, weapon.x, weapon.y),
                mountY = unit.y + Angles.trnsy(rotation, weapon.x, weapon.y);

            if(unit.type.singleTarget){
                mount.target = target;
            }else{
                if(ret) mount.target = findTargetAlt(mountX, mountY, weapon.bullet.range, weapon.bullet.collidesAir, weapon.bullet.collidesGround);
            }

            if(checkTarget(mount.target, mountX, mountY, weapon.bullet.range)) mount.target = null;

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, weapon.bullet.range + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && shouldShoot();

                Vec2 to = Predict.intercept(unit, mount.target, weapon.bullet.speed);
                mount.aimX = to.x;
                mount.aimY = to.y;
            }

            unit.isShooting |= (mount.shoot = mount.rotate = shoot);

            if(shoot){
                unit.aimX = mount.aimX;
                unit.aimY = mount.aimY;
            }
        }
    }

    @Override
    public boolean checkTarget(Teamc target, float x, float y, float range){
        return target == null || target.team() != unit.team || (target instanceof Healthc h && (h.health() >= h.maxHealth() || h.dead())) || (range != Float.MAX_VALUE && !target.within(x, y, range + (target instanceof Sized hb ? hb.hitSize()/2f : 0f)));
    }

    @Override
    public boolean invalid(Teamc target){
        return target == null || target.team() != unit.team || (target instanceof Healthc h && h.dead());
    }

    Teamc findTargetAlt(float x, float y, float range, boolean air, boolean ground){
        Teamc trueResult;
        Building blockResult = ground ? Units.findDamagedTile(unit.team, unit.x, unit.y) : null;
        Unit unitResult = Units.closest(unit.team, x, y, Math.max(range, 400f), u -> !u.dead() && u.damaged() && u.checkTarget(air, ground) && u.type != unit.type, (u, tx, ty) -> -u.maxHealth + Mathf.dst2(u.x, u.y, tx, ty) / 6400f);
        if(unitResult == null || (blockResult != null && (unitResult.dst2(unit) / 6400f) + unitResult.health > (blockResult.dst2(unit) / 6400f) + blockResult.health)){
            trueResult = blockResult;
        }else{
            trueResult = unitResult;
        }
        return trueResult;
    }

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        Teamc trueResult;
        Building blockResult = Units.findDamagedTile(unit.team, unit.x, unit.y);
        Unit result = Units.closest(unit.team, x, y, Math.max(range, 400f), u -> !u.dead() && u.type != unit.type, (u, tx, ty) -> -u.maxHealth + Mathf.dst2(u.x, u.y, tx, ty) / 6400f);
        if(result == null || (blockResult != null && (result.dst2(unit) / 6400f) + result.health > (blockResult.dst2(unit) / 6400f) + blockResult.health)){
            trueResult = blockResult;
        }else{
            trueResult = result;
        }
        if(trueResult != null) return trueResult;

        return unit.closestCore();
    }
}
