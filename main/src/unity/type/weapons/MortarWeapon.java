package unity.type.weapons;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

/** @author EyeOfDarkness */
public class MortarWeapon extends Weapon{
    public float inclineOffset = 5f, maxIncline = 85f;
    public float barrelSpeed = 5f, barrelOffset = 0f;
    public TextureRegion barrelRegion, barrelEndRegion;

    public MortarWeapon(String name){
        super(name);
        mountType = MortarMount::new;
    }

    @Override
    public void load(){
        super.load();
        barrelRegion = Core.atlas.find(name + "-barrel");
        barrelEndRegion = Core.atlas.find(name + "-barrel-end");
    }

    @Override
    public void draw(Unit unit, WeaponMount mount){
        super.draw(unit, mount);
        MortarMount mMount = (MortarMount)mount;
        float incline = -Mathf.sinDeg(Mathf.lerp(inclineOffset, maxIncline, mMount.incline)) * barrelRegion.width * Draw.scl;
        float endIncline = -Mathf.cosDeg(Mathf.lerp(inclineOffset, maxIncline, mMount.incline)) * barrelEndRegion.height;
        float rotation = unit.rotation - 90f,
        weaponRotation  = rotation + (rotate ? mount.rotation : 0),
        recoil = -((mount.reload) / reload * this.recoil),
        wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, recoil),
        wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, recoil);


        Tmp.v1.trns(weaponRotation - 90f, incline + barrelOffset).add(wx, wy);
        Tmp.v2.trns(weaponRotation - 90f, barrelOffset).add(wx, wy);

        Lines.stroke(barrelRegion.width * Draw.scl * 0.5f);
        Lines.line(barrelRegion, Tmp.v2.x, Tmp.v2.y, Tmp.v1.x, Tmp.v1.y, false);
        Draw.rect(barrelEndRegion, Tmp.v1, barrelEndRegion.width * Draw.scl, endIncline * Draw.scl, weaponRotation);
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        MortarMount mMount = (MortarMount)mount;
        float r = bullet.range;
        mMount.incline = Mathf.approachDelta(mMount.incline, Mathf.clamp(unit.dst(mount.aimX, mount.aimY) / r), barrelSpeed / r);
        super.update(unit, mount);
    }

    @Override
    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
        MortarMount mMount = (MortarMount)mount;
        float incline = Mathf.sinDeg(Mathf.lerp(inclineOffset, maxIncline, mMount.incline)) * this.shootY;
        float weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : 0),
        mX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
        mY = unit.y + Angles.trnsy(unit.rotation - 90, x, y);
        Tmp.v1.trns(weaponRotation - 90f, -incline + barrelOffset);
        shootX = mX + Tmp.v1.x;
        shootY = mY + Tmp.v1.y;
        super.shoot(unit, mount, shootX, shootY, rotation);
    }

    /*@Override
    protected Bullet bullet(Unit unit, float shootX, float shootY, float angle, float lifescl){
        float xr = Mathf.range(xRand);

        Bullet b = bullet.create(unit, unit.team,
        shootX + Angles.trnsx(angle, 0, xr),
        shootY + Angles.trnsy(angle, 0, xr),
        angle, (lifescl - velocityRnd) + Mathf.random(velocityRnd), 1f);
        b.fdata = 1f - lifescl;

        return b;
    }*/

    static class MortarMount extends WeaponMount{
        float incline = 0f;

        MortarMount(Weapon weapon){
            super(weapon);
        }
    }
}
