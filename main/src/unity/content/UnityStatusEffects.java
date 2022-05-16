package unity.content;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import unity.content.effects.*;
import unity.graphics.*;

public final class UnityStatusEffects{
    public static StatusEffect disabled, weaken, plasmaed, radiation, reloadFatigue, speedFatigue, sagittariusFatigue, molten, tpCoolDown, teamConverted, boosted, distort;

    private UnityStatusEffects(){
        throw new AssertionError();
    }

    public static void load(){
        disabled = new StatusEffect("disabled"){{
            reloadMultiplier = 0f;
            speedMultiplier = 0f;
            disarm = true;
        }};

        weaken = new StatusEffect("weaken"){{
            damageMultiplier = 0.75f;
            healthMultiplier = 0.75f;
            speedMultiplier = 0.5f;
        }};

        plasmaed = new StatusEffect("plasmaed"){{
            effectChance = 0.15f;
            damage = 0.5f;
            reloadMultiplier = 0.8f;
            healthMultiplier = 0.9f;
            damageMultiplier = 0.8f;
            effect = UnityFx.plasmaedEffect;
        }};

        radiation = new StatusEffect("radiation"){
            {
                damage = 1.6f;
            }

            @Override
            public void update(Unit unit, float time){
                super.update(unit, time);
                if(Mathf.chanceDelta(0.008f * Mathf.clamp(time / 120f))) unit.damage(unit.maxHealth * 0.125f);
                for(int i = 0; i < unit.mounts.length; i++){
                    float strength = Mathf.clamp(time / 120f);
                    WeaponMount temp = unit.mounts[i];
                    if(temp == null) continue;
                    if(Mathf.chanceDelta(0.12f)) temp.reload = Math.min(temp.reload + Time.delta * 1.5f * strength, temp.weapon.reload);
                    temp.rotation += Mathf.range(12f * strength);
                }
            }
        };

        reloadFatigue = new StatusEffect("reload-fatigue"){{
            reloadMultiplier = 0.75f;
        }};

        speedFatigue = new StatusEffect("speed-fatigue"){{
            speedMultiplier = 0.6f;
        }};

        sagittariusFatigue = new StatusEffect("sagittarius-fatigue"){{
            speedMultiplier = 0.1f;
            healthMultiplier = 0.6f;
            Color.valueOf(color, "62ae7f");
        }};

        molten = new StatusEffect("molten"){{
            color = UnityPal.lava;
            speedMultiplier = 0.6f;
            healthMultiplier = 0.5f;
            damage = 1f;
            effect = UnityFx.ahhimaLiquidNow;
        }};

        tpCoolDown = new StatusEffect("tpcooldonw"){{
            color = UnityPal.diriumLight;
            effect = Fx.none;
        }};

        teamConverted = new StatusEffect("team-converted"){{
            healthMultiplier = 0.35f;
            damageMultiplier = 0.4f;
            permanent = true;
            effect = Fx.none;
            color = Color.valueOf("a3e3ff");
        }};

        boosted = new StatusEffect("boosted"){{
            color = Pal.lancerLaser;
            effect = Fx.none;
            speedMultiplier = 2f;
        }};

        distort = new StatusEffect("distort"){
            {
                speedMultiplier = 0.35f;
                color = Pal.lancerLaser;
                effect = UnityFx.distortFx;
            }

            public void update(Unit unit, float time){
                if(damage > 0){
                    unit.damageContinuousPierce(damage);
                }else if(damage < 0){ //heal unit
                    unit.heal(-1f * damage * Time.delta);
                }

                if(effect != Fx.none && Mathf.chanceDelta(effectChance)){
                    Tmp.v1.rnd(unit.type.hitSize /2f);
                    effect.at(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, 0, 45f);
                }
            }
        };
    }
}
