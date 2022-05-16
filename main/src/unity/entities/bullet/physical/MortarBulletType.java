package unity.entities.bullet.physical;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;

/** @author EyeOfDarkness */
public class MortarBulletType extends BasicBulletType{
    public float heightScl = 1.5f;

    public MortarBulletType(float speed, float damage){
        super(speed, damage, "shell");
        collides = false;
        collidesTiles = false;
        scaleLife = true;
        shrinkX = shrinkY = 0f;
        trailLength = 15;
        trailInterp = a -> Mathf.sin(a * Mathf.PI);
    }

    @Override
    public void draw(Bullet b){
        drawTrail(b);
        float f = Mathf.lerp(Math.max(b.fdata, 0f), 1f, 0.125f);
        float scl = 1f + (heightScl * Interp.sineOut.apply(b.fslope()) * f);
        float height = this.height * ((1f - shrinkY) + shrinkY * b.fout()) * scl;
        float width = this.width * ((1f - shrinkX) + shrinkX * b.fout()) * scl;
        float offset = -90 + (spin != 0 ? Mathf.randomSeed(b.id, 360f) + b.time * spin : 0f);

        Color mix = Tmp.c1.set(mixColorFrom).lerp(mixColorTo, b.fin());

        Draw.mixcol(mix, mix.a);

        Draw.color(backColor);
        Draw.rect(backRegion, b.x, b.y, width, height, b.rotation() + offset);
        Draw.color(frontColor);
        Draw.rect(frontRegion, b.x, b.y, width, height, b.rotation() + offset);

        Draw.reset();
    }
}
