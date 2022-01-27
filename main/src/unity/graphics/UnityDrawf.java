package unity.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.graphics.*;

public final class UnityDrawf{
    private final static float[] v = new float[6];
    private final static TextureRegion nRegion = new TextureRegion();
    private final static Vec2 vector = new Vec2();

    private UnityDrawf(){
        throw new AssertionError();
    }

    /** @author EyeOfDarkness */
    public static void shiningCircle(int seed, float time, float x, float y, float radius, int spikes, float spikeDuration, float spikeWidth, float spikeHeight){
        shiningCircle(seed, time, x, y, radius, spikes, spikeDuration, spikeWidth, spikeHeight, 0f);
    }

    /** @author EyeOfDarkness */
    public static void shiningCircle(int seed, float time, float x, float y, float radius, int spikes, float spikeDuration, float spikeWidth, float spikeHeight, float angleDrift){
        shiningCircle(seed, time, x, y, radius, spikes, spikeDuration, 0f, spikeWidth, spikeHeight, angleDrift);
    }

    /** @author EyeOfDarkness */
    public static void shiningCircle(int seed, float time, float x, float y, float radius, int spikes, float spikeDuration, float durationRange, float spikeWidth, float spikeHeight, float angleDrift){
        Fill.circle(x, y, radius);
        spikeWidth = Math.min(spikeWidth, 90f);
        int idx;

        for(int i = 0; i < spikes; i++){
            float d = spikeDuration * (durationRange > 0f ? Mathf.randomSeed((seed + i) * 41L, 1f - durationRange, 1f + durationRange) : 1f);
            float timeOffset = Mathf.randomSeed((seed + i) * 314L, 0f, d);
            int timeSeed = Mathf.floor((time + timeOffset) / d);

            float fin = ((time + timeOffset) % d) / d;
            float fslope = (0.5f - Math.abs(fin - 0.5f)) * 2f;
            float angle = Mathf.randomSeed(Math.max(timeSeed, 1) + ((i + seed) * 245L), 360f);

            if(fslope > 0.0001f){
                idx = 0;
                float drift = angleDrift > 0 ? Mathf.randomSeed(Math.max(timeSeed, 1) + ((i + seed) * 162L), -angleDrift, angleDrift) * fin : 0f;
                for(int j = 0; j < 3; j++){
                    float angB = (j * spikeWidth - (2f) * spikeWidth / 2f) + angle;
                    Tmp.v1.trns(angB + drift, radius + (j == 1 ? (spikeHeight * fslope) : 0f)).add(x, y);
                    v[idx++] = Tmp.v1.x;
                    v[idx++] = Tmp.v1.y;
                }

                Fill.tri(v[0], v[1], v[2], v[3], v[4], v[5]);
            }
        }
    }

    public static void drawHeat(TextureRegion reg, float x, float y, float rot, float temp){
        float a;
        if(temp > 273.15f){
            a = Math.max(0f, (temp - 498f) * 0.001f);
            if(a < 0.01f) return;
            if(a > 1f){
                Color fCol = Pal.turretHeat.cpy().add(0, 0, 0.01f * a);
                fCol.mul(a);
                Draw.color(fCol, a);
            }else{
                Draw.color(Pal.turretHeat, a);
            }
        }else{
            a = 1f - Mathf.clamp(temp / 273.15f);
            if(a < 0.01f) return;
            Draw.color(UnityPal.coldColor, a);
        }
        Draw.blend(Blending.additive);
        Draw.rect(reg, x, y, rot);
        Draw.blend();
        Draw.color();
    }

    public static void drawSlideRect(TextureRegion region, float x, float y, float w, float h, float tw, float th, float rot, int step, float offset){
        if(region == null) return;
        nRegion.set(region);

        float scaleX = w / tw;
        float texW = nRegion.u2 - nRegion.u;

        nRegion.u += Mathf.map(offset % 1, 0f, 1f, 0f, texW * step / tw);
        nRegion.u2 = nRegion.u + scaleX * texW;
        Draw.rect(nRegion, x, y, w, h, w * 0.5f, h * 0.5f, rot);
    }

    public static void drawRotRect(TextureRegion region, float x, float y, float w, float h, float th, float rot, float ang1, float ang2){
        if(region == null || !Core.settings.getBool("effects")) return;
        float amod1 = Mathf.mod(ang1, 360f);
        float amod2 = Mathf.mod(ang2, 360f);
        if(amod1 >= 180f && amod2 >= 180f) return;

        nRegion.set(region);
        float uy1 = nRegion.v;
        float uy2 = nRegion.v2;
        float uCenter = (uy1 + uy2) / 2f;
        float uSize = (uy2 - uy1) * h / th * 0.5f;
        uy1 = uCenter - uSize;
        uy2 = uCenter + uSize;
        nRegion.v = uy1;
        nRegion.v2 = uy2;

        float s1 = -Mathf.cos(ang1 * Mathf.degreesToRadians);
        float s2 = -Mathf.cos(ang2 * Mathf.degreesToRadians);
        if(amod1 > 180f){
            nRegion.v2 = Mathf.map(0f, amod1 - 360f, amod2, uy2, uy1);
            s1 = -1f;
        }else if(amod2 > 180f){
            nRegion.v = Mathf.map(180f, amod1, amod2, uy2, uy1);
            s2 = 1f;
        }
        s1 = Mathf.map(s1, -1f, 1f, y - h / 2f, y + h / 2f);
        s2 = Mathf.map(s2, -1f, 1f, y - h / 2f, y + h / 2f);
        Draw.rect(nRegion, x, (s1 + s2) * 0.5f, w, s2 - s1, w * 0.5f, y - s1, rot);
    }
}
