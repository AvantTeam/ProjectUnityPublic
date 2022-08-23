package unity.entities.bullet.exp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.content.effects.*;
import unity.graphics.*;
import unity.util.*;
import unity.world.blocks.exp.*;

public class ExpBulletType extends BulletType{
    /** Color of bullet. Shifts to second color as the turret levels up. */
    public Color fromColor = Pal.lancerLaser, toColor = UnityPal.expLaser;
    /** Damage increase per owner level, if the owner can level up. */
    public float damageInc;
    /** Exp gained on hit */
    public int expGain = 1;
    public boolean expOnHit = false;
    public float expChance = 1f;

    public boolean overrideTrail = true;
    public boolean overrideLight = true;

    public ExpBulletType(float speed, float damage){
        super(speed, damage);
    }

    @Override
    public void hit(Bullet b, float x, float y){
        if(expOnHit) handleExp(b, x, y, expGain);

        fragExp(b, x, y);
        BulletType f = fragBullet;
        fragBullet = null; //avoid fragging twice
        super.hit(b, x, y);
        fragBullet = f;
    }

    @Override
    public void drawTrail(Bullet b){
        if(trailLength > 0 && b.trail != null){
            float z = Draw.z();
            Draw.z(z - 0.0001f);
            b.trail.draw(overrideTrail ? getColor(b).mul(trailColor) : trailColor, trailWidth);
            Draw.z(z);
        }
    }

    @Override
    public void drawLight(Bullet b){
        if(lightOpacity <= 0f || lightRadius <= 0f) return;
        Drawf.light(b, lightRadius, overrideLight ? getColor(b).mul(lightColor) : lightColor, lightOpacity);
    }

    public void handleExp(Bullet b, float x, float y, int amount){
        if(!Mathf.chance(expChance)) return;
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

    /** @return Tmp.c2 set to the color of the bullet
     */
    public Color getColor(Bullet b){
        return Tmp.c2.set(fromColor).lerp(toColor, getLevelf(b));
    }

    /** Handles fragging of exp bullets. */
    public void fragExp(Bullet b, float x, float y){
        if(fragBullet != null){
            for(int i = 0; i < fragBullets; i++){
                float len = Mathf.random(1f, 7f);
                float a = b.rotation() + Mathf.range(fragRandomSpread/2) + fragAngle;
                fragBullet.create(b.owner, b.team, x + Angles.trnsx(a, len), y + Angles.trnsy(a, len), a, Mathf.random(fragVelocityMin, fragVelocityMax), Mathf.random(fragLifeMin, fragLifeMax));
            }
        }
    }
}
