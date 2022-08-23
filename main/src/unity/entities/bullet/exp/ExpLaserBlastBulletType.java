package unity.entities.bullet.exp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.content.*;
import unity.content.effects.*;
import unity.graphics.*;
import unity.world.blocks.exp.*;

public class ExpLaserBlastBulletType extends LaserBulletType{
    /** Color of laser. Shifts to second color as the turret levels up. */
    public Color[] fromColors = {Pal.lancerLaser.cpy().mul(1f, 1f, 1f, 0.4f), Pal.lancerLaser, Color.white},
        toColors = {UnityPal.exp.cpy().mul(1f, 1f, 1f, 0.4f), UnityPal.exp, Color.white};
    /** Damage increase per owner level, if the owner can level up. */
    public float damageInc;
    /** Size increase per owner level, if the owner can level up. */
    public float lengthInc, widthInc;
    /** You can guess by now. */
    public float lightningSpacingInc, lightningDamageInc;
    /** Color of lightning. Shifts to second color as the turret levels up. */
    public Color lightningFromColor = Pal.lancerLaser, lightningToColor = UnityPal.exp;
    /** Exp gained on hit */
    public int hitUnitExpGain, hitBuildingExpGain;

    public ExpLaserBlastBulletType(float length, float damage){
        super(damage);
        this.length = length;
        ammoMultiplier = 1;
        drawSize = length * 2f;
        hitEffect = Fx.hitLiquid;
        shootEffect = Fx.hitLiquid;
        lifetime = 18f;
        despawnEffect = Fx.none;
        keepVelocity = false;
        collides = false;
        pierce = true;
        hittable = false;
        absorbable = false;
    }

    public ExpLaserBlastBulletType(){
        this(120f, 1f);
    }

    public void handleExp(Bullet b, float x, float y, int amount){
        if(b.owner instanceof ExpTurret.ExpTurretBuild exp){
            if(exp.level() < exp.maxLevel() && Core.settings.getBool("hitexpeffect")){
                for(int i = 0; i < Math.ceil(amount); i++){
                    UnityFx.expGain.at(x, y, 0f, b.owner);
                }
            }
            exp.handleExp(amount);
        }
    }

    public int getLevel(Bullet b){
        if(b.owner instanceof ExpTurret.ExpTurretBuild exp){
            return exp.level();
        }else{
            return 0;
        }
    }

    public float getLevelf(Bullet b){
        if(b.owner instanceof ExpTurret.ExpTurretBuild exp){
            return exp.levelf();
        }else{
            return 0f;
        }
    }

    public void setDamage(Bullet b){
        b.damage += damageInc * getLevel(b) * b.damageMultiplier();
    }

    public void setColors(Bullet b){
        float f = getLevelf(b);
        Color[] data = {Tmp.c1.set(fromColors[0]).lerp(toColors[0], f).cpy(), Tmp.c2.set(fromColors[1]).lerp(toColors[1], f).cpy(), Tmp.c3.set(fromColors[2]).lerp(toColors[2], f).cpy()};
        b.data = data;
    }

    public Color getLightningColor(Bullet b){
        return Tmp.c1.set(lightningFromColor).lerp(lightningToColor, getLevelf(b)).cpy();
    }

    public float getLength(Bullet b){
        return length + lengthInc * getLevel(b);
    }

    public float getWidth(Bullet b){
        return width + widthInc * getLevel(b);
    }

    public float getLightningSpacing(Bullet b){
        return lightningSpacing + lightningSpacingInc * getLevel(b);
    }

    public float getLightningDamage(Bullet b){
        return lightningDamage + lightningDamageInc * getLevel(b);
    }

    @Override
    public float calculateRange(){
        return Math.max(length, maxRange);
    }

    @Override
    public void init(Bullet b){
        setDamage(b);
        float resultLength = Damage.collideLaser(b, getLength(b), largeHit, laserAbsorb, pierceCap), rot = b.rotation();

        laserEffect.at(b.x, b.y, rot, resultLength * 0.75f);

        if(getLightningSpacing(b) > 0){
            int idx = 0;
            for(float i = 0; i <= resultLength; i += getLightningSpacing(b)){
                float cx = b.x + Angles.trnsx(rot,  i),
                    cy = b.y + Angles.trnsy(rot, i);

                int f = idx++;

                for(int s : Mathf.signs){
                    Time.run(f * lightningDelay, () -> {
                        if(b.isAdded() && b.type == this){
                            Lightning.create(b, getLightningColor(b),
                                getLightningDamage(b) < 0 ? damage : getLightningDamage(b),
                                cx, cy, rot + 90*s + Mathf.range(lightningAngleRand),
                                lightningLength + Mathf.random(lightningLengthRand));
                        }
                    });
                }
            }
        }
        setColors(b);
    }

    @Override
    public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
        handleExp(b, build.x, build.y, hitBuildingExpGain);
        super.hitTile(b, build, x, y, initialHealth, direct);
    }

    @Override
    public void hitEntity(Bullet b, Hitboxc other, float initialHealth){
        handleExp(b, other.x(), other.y(), hitUnitExpGain);
        super.hitEntity(b, other, initialHealth);
    }

    @Override
    public void draw(Bullet b){
        float realLength = b.fdata;

        float f = Mathf.curve(b.fin(), 0f, 0.2f);
        float baseLen = realLength * f;
        float cwidth = getWidth(b);
        float compound = 1f;

        Lines.lineAngle(b.x, b.y, b.rotation(), baseLen);
        for(Color color : (Color[])b.data){
            Draw.color(color);
            Lines.stroke((cwidth *= lengthFalloff) * b.fout());
            Lines.lineAngle(b.x, b.y, b.rotation(), baseLen, false);
            Tmp.v1.trns(b.rotation(), baseLen);
            Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Lines.getStroke() * 1.22f, cwidth * 2f + getWidth(b) / 2f, b.rotation());

            Fill.circle(b.x, b.y, 1f * cwidth * b.fout());
            for(int i : Mathf.signs){
                Drawf.tri(b.x, b.y, sideWidth * b.fout() * cwidth, sideLength * compound, b.rotation() + sideAngle * i);
            }

            compound *= lengthFalloff;
        }
        Draw.reset();

        Tmp.v1.trns(b.rotation(), baseLen * 1.1f);
        Drawf.light(b.x, b.y, b.x + Tmp.v1.x, b.y + Tmp.v1.y, cwidth * 1.4f * b.fout(), ((Color[])b.data)[0], 0.6f);
    }

    @Override
    public void drawLight(Bullet b){
        //no light drawn here
    }
}