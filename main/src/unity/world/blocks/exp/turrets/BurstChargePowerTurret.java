package unity.world.blocks.exp.turrets;

import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;

public class BurstChargePowerTurret extends ExpPowerTurret {
    public BurstChargePowerTurret(String name){
        super(name);
    }

    public class BurstChargeTurretBuild extends ExpPowerTurretBuild {

        //TODO broken, needs complete rewrite -Anuke
        /*
        protected void shootCharge(BulletType type, float rotation){
            float rx = Mathf.range(xRand);
            tr.trns(rotation, shootLength, rx);
            chargeBeginEffect.at(x + tr.x, y + tr.y, rotation);
            chargeSound.at(x + tr.x, y + tr.y, 1);

            for(int i = 0; i < chargeEffects; i++){
                Time.run(Mathf.random(chargeMaxDelay), () -> {
                    if(dead) return;
                    tr.trns(rotation, shootLength, rx);
                    chargeEffect.at(x + tr.x, y + tr.y, rotation);
                });
            }

            Time.run(chargeTime, () -> {
                if(dead) return;
                tr.trns(rotation, shootLength, rx);
                heat = 1f;
                effects();
                useAmmo();
                recoil = recoilAmount;
                bullet(type, rotation + Mathf.range(inaccuracy + type.inaccuracy));
            });
        }

        @Override
        protected void shoot(BulletType type){
            if(chargeTime <= 0){
                super.shoot(type);
                return;
            }

            if(burstSpacing > 0.0001f){
                charging = true;
                for(int i = 0; i < shots; i++){
                    int ii = i;
                    Time.run(burstSpacing * i, () -> {
                        if(dead || !hasAmmo()) return;
                        tr.trns(rotation, shootLength, Mathf.range(xRand));
                        shootCharge(peekAmmo(), rotation + Mathf.range(inaccuracy + peekAmmo().inaccuracy) + (ii - (int)(shots / 2f)) * spread);
                    });
                }

                Time.run(burstSpacing * shots + chargeTime, () -> {
                    charging = false;
                });

            }else{
                //otherwise, use the normal shot pattern(s)

                charging = true;
                if(alternate){
                    float i = (shotCounter % shots) - (shots-1)/2f;

                    tr.trns(rotation - 90, spread * i + Mathf.range(xRand), shootLength);
                    shootCharge(type, rotation + Mathf.range(inaccuracy + type.inaccuracy));
                }else{
                    tr.trns(rotation, shootLength, Mathf.range(xRand));

                    for(int i = 0; i < shots; i++){
                        shootCharge(type, rotation + Mathf.range(inaccuracy + type.inaccuracy) + (i - (int)(shots / 2f)) * spread);
                    }
                }

                Time.run(chargeTime, () -> {
                    charging = false;
                });
                shotCounter++;
            }
        }
        */
    }
}
