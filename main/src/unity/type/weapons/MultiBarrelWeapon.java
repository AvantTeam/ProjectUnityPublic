package unity.type.weapons;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

/** @author EyeOfDarkness */
public class MultiBarrelWeapon extends Weapon{
    public int barrels = 2;
    public float barrelSpacing = 6f;
    public float barrelOffset = 0f;
    public float barrelRecoil = 0f;
    public boolean mirrorBarrels = false;
    public TextureRegion barrelRegion, barrelOutlineRegion;
    private final static Vec2 tv = new Vec2();

    public MultiBarrelWeapon(String name){
        super(name);
        mountType = MultiBarrelMount::new;
    }

    @Override
    public void load(){
        super.load();
        barrelRegion = Core.atlas.find(name + "-barrel");
        barrelOutlineRegion = Core.atlas.find(name + "-barrel-outline");
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        super.update(unit, mount);
        MultiBarrelMount mMount = ((MultiBarrelMount)mount);
        for(int i = 0; i < mMount.recoils.length; i++){
            mMount.recoils[i] = Math.max(0f, mMount.recoils[i] - (((barrelRecoil / reload) / barrels) * Time.delta));
        }
    }

    @Override
    public void drawOutline(Unit unit, WeaponMount mount){
        if(barrelOutlineRegion.found()){
            MultiBarrelMount mMount = ((MultiBarrelMount)mount);
            float
            rotation = unit.rotation - 90,
            weaponRotation = rotation + (rotate ? mount.rotation : 0),
            recoil = -((mount.reload) / reload * this.recoil),
            wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, recoil),
            wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, recoil);

            int barrels = mMount.recoils.length;

            Intc drawBarrel = i -> {
                float offset = i * barrelSpacing - (barrels - 1) * barrelSpacing / 2f;
                int s = Mathf.sign((!mirrorBarrels || offset < 0) != flipSprite);
                tv.trns(weaponRotation - 90f, barrelOffset + -mMount.recoils[i], offset).add(wx, wy);
                Draw.rect(barrelOutlineRegion,
                tv.x, tv.y,
                barrelOutlineRegion.width * Draw.scl * -Mathf.sign(flipSprite) * s,
                barrelOutlineRegion.height * Draw.scl,
                weaponRotation);
            };

            if(!flipSprite){
                for(int i = 0; i < barrels; i++){
                    drawBarrel.get(i);
                }
            }else{
                for(int i = barrels - 1; i >= 0; i--){
                    drawBarrel.get(i);
                }
            }
        }

        super.drawOutline(unit, mount);
    }

    @Override
    public void draw(Unit unit, WeaponMount mount){
        MultiBarrelMount mMount = ((MultiBarrelMount)mount);

        float
        rotation = unit.rotation - 90,
        weaponRotation  = rotation + (rotate ? mount.rotation : 0),
        recoil = -((mount.reload) / reload * this.recoil),
        wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, recoil),
        wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, recoil);

        int barrels = mMount.recoils.length;

        if(shadow > 0){
            Drawf.shadow(wx, wy, shadow);
        }

        if(outlineRegion.found() && top){
            Draw.rect(outlineRegion,
            wx, wy,
            outlineRegion.width * Draw.scl * -Mathf.sign(flipSprite),
            outlineRegion.height * Draw.scl,
            weaponRotation);
        }

        Intc drawBarrel = i -> {
            float offset = i * barrelSpacing - (barrels - 1) * barrelSpacing / 2f;
            int s = Mathf.sign((!mirrorBarrels || offset < 0) != flipSprite);
            tv.trns(weaponRotation + 90f, barrelOffset + -mMount.recoils[i], offset).add(wx, wy);
            if(top && barrelOutlineRegion.found()){
                Draw.rect(barrelOutlineRegion,
                tv.x, tv.y,
                barrelOutlineRegion.width * Draw.scl * -Mathf.sign(flipSprite) * s,
                barrelOutlineRegion.height * Draw.scl,
                weaponRotation);
            }
            Draw.rect(barrelRegion,
            tv.x, tv.y,
            barrelRegion.width * Draw.scl * -Mathf.sign(flipSprite) * s,
            barrelRegion.height * Draw.scl,
            weaponRotation);
        };

        if(!flipSprite){
            for(int i = 0; i < barrels; i++){
                drawBarrel.get(i);
            }
        }else{
            for(int i = barrels - 1; i >= 0; i--){
                drawBarrel.get(i);
            }
        }

        Draw.rect(region,
        wx, wy,
        region.width * Draw.scl * -Mathf.sign(flipSprite),
        region.height * Draw.scl,
        weaponRotation);
    }

    @Override
    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
        MultiBarrelMount mMount = ((MultiBarrelMount)mount);
        float offset = Mathf.mod(mMount.inUse, mMount.recoils.length) * barrelSpacing - (mMount.recoils.length - 1) * barrelSpacing / 2f;
        tv.trns(rotation, 0f, offset);
        shootX += tv.x;
        shootY += tv.y;

        super.shoot(unit, mount, shootX, shootY, rotation);

        mMount.recoils[Mathf.mod(mMount.inUse, mMount.recoils.length)] = barrelRecoil;
        mMount.inUse += Mathf.sign(flipSprite);
        mMount.inUse %= mMount.recoils.length;
    }

    public static class MultiBarrelMount extends WeaponMount{
        int inUse = 0;
        float[] recoils;

        public MultiBarrelMount(Weapon weapon){
            super(weapon);
            recoils = new float[((MultiBarrelWeapon)weapon).barrels];
            if(weapon.flipSprite) inUse = recoils.length - 1;
        }
    }
}
