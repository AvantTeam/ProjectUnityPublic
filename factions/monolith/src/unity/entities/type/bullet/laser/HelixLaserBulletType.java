package unity.entities.type.bullet.laser;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.util.*;

import static unity.graphics.MonolithPal.*;

/** @author GlennFolker */
public class HelixLaserBulletType extends LaserBulletType{
    public int swirlAmount = 3;
    public Color
    swirlColor = monolithLighter, swirlColorDark = monolithMid,
    dashColor = monolithLight, dashColorDark = monolithMid;

    public float
    swirlScale = 12f, swirlMagnitude = 6f, swirlThickness = 1f,
    swirlIn = 0.1f, swirlStay = 0.2f, swirlOut = 0.5f, swirlFrom = 0.1f, swirlTo = 0.96f,

    laserExtTime = 0.2f, laserShrinkTime = 0.3f, laserTo = 1f,

    dashWidth = 8f,
    dashFrom = 0.2f, dashTo = 0.92f, dashThickness = 1.5f;

    public Interp
    laserGrowInterp = Interp.pow2Out, laserShrinkInterp = Interp.pow2Out, laserThickInterp = Interp.pow4Out,
    swirlInInterp = Interp.pow2In, swirlOutInterp = Interp.pow3In, swirlFadeInterp = Interp.pow10Out,
    dashInterp1 = Interp.pow2In, dashInterp2 = Interp.pow2Out, dashColorInterp = Interp.pow5In;

    public HelixLaserBulletType(float damage){
        super(damage);
        lifetime = 32f;
    }

    @Override
    public void draw(Bullet b){
        float
        z = Draw.z(),
        realLength = b.fdata, scl = realLength / length,
        fin = b.fin(), rot = b.rotation(),

        lfin = Mathf.curve(fin, 0f, laserTo), lfout = 1f - lfin,
        laserLenf = Mathf.curve(lfin, 0f, laserExtTime * scl), laserLen = laserGrowInterp.apply(laserLenf) * realLength,
        laserShrinkf = Mathf.curve(lfin, 1f - laserShrinkTime * scl, 1f), laserShrink = laserShrinkInterp.apply(laserShrinkf) * realLength,
        cwidth = width,
        compound = 1f,

        sfin = Mathf.curve(fin, swirlFrom, swirlTo),
        slife = swirlIn + swirlStay + swirlOut, soffset = 1f - slife,

        dfin = Mathf.curve(fin, dashFrom * scl, dashTo);

        if(lfin <= laserTo){
            for(Color color : colors){
                Tmp.v1.trns(rot, laserShrink);

                Draw.color(color);
                Lines.stroke((cwidth *= lengthFalloff) * laserThickInterp.apply(lfout));
                Lines.lineAngle(b.x + Tmp.v1.x, b.y + Tmp.v1.y, rot, laserLen - laserShrink, false);

                Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Lines.getStroke(), Lines.getStroke() / 2f, rot + 180f);
                Tmp.v1.trns(rot, laserLen);
                Drawf.tri(b.x + Tmp.v1.x, b.y + Tmp.v1.y, Lines.getStroke(), cwidth * 2f + width / 2f, rot);

                Fill.circle(b.x, b.y, 1f * cwidth * lfout);
                for(int i : Mathf.signs){
                    Drawf.tri(b.x, b.y, sideWidth * lfout * cwidth, sideLength * compound, rot + sideAngle * i);
                }

                compound *= lengthFalloff;
            }
        }

        if(dfin >= dashFrom && dfin <= dashTo){
            Lines.stroke(dashThickness, Tmp.c1.set(dashColor).lerp(dashColorDark, dashColorInterp.apply(dfin)));
            for(int sign : Mathf.signs){
                float x = dashWidth * sign * 0.5f, cy = Lines.getStroke() * 2.5f;
                Tmp.v1.trns(rot - 90f, x, dashInterp1.apply(dfin) * realLength + cy).add(b);
                Tmp.v2.trns(rot - 90f, x, dashInterp2.apply(dfin) * realLength + cy).add(b);

                Lines.line(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, false);
                Drawf.tri(Tmp.v1.x, Tmp.v1.y, Lines.getStroke(), cy, rot + 180f);
                Drawf.tri(Tmp.v2.x, Tmp.v2.y, Lines.getStroke(), cy, rot);
            }
        }

        if(sfin >= swirlFrom && sfin <= swirlTo){
            Lines.stroke(swirlThickness);

            int iterations = Math.max(Mathf.round(realLength), 2);
            float
            seg = realLength / iterations,
            rand = Mathf.randomSeed(b.id, Mathf.PI2 * swirlScale) + (Mathf.randomSeed(b.id + 1, 0, 1) * 2f - 1f);
            for(int i = 0; i < swirlAmount; i++){
                DrawUtils.beginLine();

                float angleOffset = rand + (Mathf.PI2 * swirlScale) * ((float)i / swirlAmount);
                for(int it = 0; it < iterations; it++){
                    float
                    in = it / (iterations - 1f),
                    off = soffset * in,
                    prog = (
                    swirlInInterp.apply(Mathf.curve(sfin, off, swirlIn + off)) -
                    swirlOutInterp.apply(Mathf.curve(sfin, swirlStay + off, swirlOut + off))
                    ) * swirlFadeInterp.apply(1f - in),

                    rad = it * seg + angleOffset,
                    x = Mathf.cos(rad, swirlScale, swirlMagnitude),
                    tz = Mathf.sin(rad, swirlScale, 1f) >= 0f ? z : (z - 0.01f);

                    Tmp.v1.trns(rot - 90f, x, it * seg).add(b);
                    DrawUtils.linePoint(Tmp.v1.x, Tmp.v1.y, Tmp.c1.set(swirlColor).lerp(swirlColorDark, 1f - prog).a(prog).toFloatBits(), tz);
                }

                DrawUtils.endLine();
            }
        }

        Draw.z(z);
        Draw.reset();
    }
}
