package unity.type.weapons;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.audio.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import unity.util.*;

import static mindustry.Vars.*;

//TODO this seems like something that should just be added to the base game -Anuke
/** @author EyeOfDarkness */
public class LimitedAngleWeapon extends Weapon{
    public float angleCone = 45f;
    public float angleOffset = 0f;
    public float defaultAngle = 0f;

    public LimitedAngleWeapon(String name){
        super(name);
        mountType = weapon -> {
            WeaponMount mount = new WeaponMount(weapon);
            mount.rotation = defaultAngle * Mathf.sign(flipSprite);
            return mount;
        };
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        boolean can = unit.canShoot();
        mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);
        mount.recoil = Mathf.approachDelta(mount.recoil, 0f, (Math.abs(recoil) * unit.reloadMultiplier) / recoilTime);

        float
        weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : 0),
        mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
        mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
        bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY),
        bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY),
        shootAngle = rotate ? weaponRotation + 90 : Angles.angle(bulletX, bulletY, mount.aimX, mount.aimY) + (unit.rotation - unit.angleTo(mount.aimX, mount.aimY));

        // Find a new target.
        if(!controllable && autoTarget){
            if((mount.retarget -= Time.delta) <= 0f){
                mount.target = findTarget(unit, mountX, mountY, bullet.range, bullet.collidesAir, bullet.collidesGround);
                mount.retarget = mount.target == null ? targetInterval : targetSwitchInterval;
            }

            if(mount.target != null && checkTarget(unit, mount.target, mountX, mountY, bullet.range)){
                mount.target = null;
            }

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, bullet.range + Math.abs(shootY) + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && can;

                if(predictTarget){
                    Vec2 to = Predict.intercept(unit, mount.target, bullet.speed);
                    mount.aimX = to.x;
                    mount.aimY = to.y;
                }else{
                    mount.aimX = mount.target.x();
                    mount.aimY = mount.target.y();
                }
            }

            mount.shoot = mount.rotate = shoot;

            // Note that shooting state is not affected, as these cannot be controlled.
            // Logic will return shooting as false even if these return true, which is fine.
        }

        // Update continuous state.
        if(continuous && mount.bullet != null){
            if(!mount.bullet.isAdded() || mount.bullet.time >= mount.bullet.lifetime || mount.bullet.type != bullet){
                mount.bullet = null;
            }else{
                mount.bullet.rotation(weaponRotation + 90);
                mount.bullet.set(bulletX, bulletY);
                mount.reload = reload;
                unit.vel.add(Tmp.v1.trns(unit.rotation + 180f, mount.bullet.type.recoil));
                if(shootSound != Sounds.none && !headless){
                    if(mount.sound == null) mount.sound = new SoundLoop(shootSound, 1f);
                    mount.sound.update(bulletX, bulletY, true);
                }
            }
        }else{
            // Heat decreases when not firing.
            mount.heat = Math.max(mount.heat - Time.delta * unit.reloadMultiplier / mount.weapon.cooldownTime, 0);

            if(mount.sound != null){
                mount.sound.update(bulletX, bulletY, false);
            }
        }

        // Flip weapon shoot side for alternating weapons at half reload.
        if(otherSide != -1 && alternate && mount.side == flipSprite &&
        mount.reload + Time.delta * unit.reloadMultiplier > reload/2f && mount.reload <= reload/2f){
            unit.mounts[otherSide].side = !unit.mounts[otherSide].side;
            mount.side = !mount.side;
        }

        // Rotate if applicable.
        if(rotate && (mount.rotate || mount.shoot) && can){
            float axisX = unit.x + Angles.trnsx(unit.rotation - 90,  x, y),
            axisY = unit.y + Angles.trnsy(unit.rotation - 90,  x, y);

            mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - unit.rotation;
            mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, rotateSpeed * Time.delta);
            mount.rotation = Utils.clampedAngle(mount.rotation, angleOffset * Mathf.sign(flipSprite), angleCone);
        }else if(!rotate){
            mount.rotation = 0;
            mount.targetRotation = unit.angleTo(mount.aimX, mount.aimY);
        }

        if(mount.shoot &&
        can &&
        (!useAmmo || unit.ammo > 0 || !state.rules.unitAmmo || unit.team.rules().infiniteAmmo) && //check ammo
        (!alternate || mount.side == flipSprite) &&
        (unit.vel.len() >= mount.weapon.minShootVelocity || (net.active() && !unit.isLocal())) && //check velocity requirements
        mount.reload <= 0.0001f &&
        Angles.within(rotate ? mount.rotation : unit.rotation, mount.targetRotation, mount.weapon.shootCone) //has to be within the cone
        ){
            shoot(unit, mount, bulletX, bulletY, shootAngle);

            mount.reload = reload;

            if(useAmmo){
                unit.ammo--;
                if(unit.ammo < 0) unit.ammo = 0;
            }
        }
    }

    @Override
    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        Boolf<Posc> angBool = e -> Utils.angleDist(unit.rotation + (angleOffset * Mathf.sign(flipSprite)), unit.angleTo(e)) <= angleCone;
        return Units.closestTarget(unit.team, x, y, range + Math.abs(shootY), u -> u.checkTarget(air, ground) && angBool.get(u), t -> ground && angBool.get(t));
    }

    @Override
    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return super.checkTarget(unit, target, x, y, range) || Utils.angleDist(unit.rotation + (angleOffset * Mathf.sign(flipSprite)), unit.angleTo(target)) > angleCone;
    }
}
