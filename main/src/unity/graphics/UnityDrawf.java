package unity.graphics;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;

public final class UnityDrawf{
    private final static float[] v = new float[6];

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
}
