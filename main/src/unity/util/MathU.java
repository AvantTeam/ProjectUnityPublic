package unity.util;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;

public final class MathU{
    private final static Vec2 vec = new Vec2();
    private final static Rand seedr = new Rand();

    private MathU(){
        throw new AssertionError();
    }

    public static void randLenVectors(long seed, int amount, float in, float inRandMin, float inRandMax, float lengthRand, FloatFloatf length, UParticleConsumer cons){
        seedr.setSeed(seed);
        for(int i = 0; i < amount; i++){
            float r = seedr.random(inRandMin, inRandMax);
            float offset = r > 0 ? seedr.nextFloat() * r : 0f;

            float fin = Mathf.curve(in, offset, (1f - r) + offset);
            float f = length.get(fin) * (lengthRand <= 0f ? 1f : seedr.random(1f - lengthRand, 1f));
            vec.trns(seedr.random(360f), f);
            cons.get(vec.x, vec.y, fin);
        }
    }

    public static Vec2 addLength(Vec2 vec, float add){
        float len = vec.len();
        vec.x += add * (vec.x / len);
        vec.y += add * (vec.y / len);
        return vec;
    }

    public static float hyperbolicLimit(float t){
        return 1f-1f/(t+1);
    }

    public interface UParticleConsumer{
        void get(float x, float y, float fin);
    }
}
