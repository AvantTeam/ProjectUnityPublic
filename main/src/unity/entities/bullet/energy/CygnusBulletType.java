package unity.entities.bullet.energy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import unity.graphics.*;

/** @author EyeOfDarkness */
public class CygnusBulletType extends EmpBulletType{
    public float size = 18f;
    public float allyStatusDuration = 60f * 2f;
    public StatusEffect allyStatus = StatusEffects.overclock;

    @Override
    public void hit(Bullet b, float x, float y){
        super.hit(b, x, y);

        if(hitUnits){
            Units.nearby(b.team, x, y, radius, other -> {
                if(other.team == b.team && other != b.owner){
                    other.heal(healPercent / 100f * other.maxHealth);
                    other.apply(allyStatus, allyStatusDuration);
                }
            });
        }
    }

    @Override
    public void drawLight(Bullet b){
        Drawf.light(b.team, b.x, b.y, size * 3f, backColor, 0.3f);
    }

    @Override
    public void draw(Bullet b){
        drawTrail(b);
        Draw.color(backColor);
        for(int s : Mathf.signs){
            float r = b.rotation() + (90f * s);
            Drawf.tri(b.x + Angles.trnsx(r, size - 2f), b.y + Angles.trnsy(r, size - 2f), size * 0.6f, (size * 2f) + Mathf.sin(Time.time, 15f, size / 2f), r);
        }
        UnityDrawf.shiningCircle(b.id, Time.time, b.x, b.y, size, 7, 30f, 17f, 12f, 180f);
        Draw.color(Color.white);
        UnityDrawf.shiningCircle(b.id, Time.time, b.x, b.y, size * 0.65f, 7, 30f, 23f, 11f, 180f);
    }
}
