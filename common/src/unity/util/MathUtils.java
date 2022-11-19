package unity.util;

import arc.func.*;
import arc.math.*;
import arc.math.Interp.*;
import arc.math.geom.*;
import arc.util.*;

import static java.lang.Math.exp;

/** Shared utility access for mathematical operations. */
@SuppressWarnings({"SuspiciousNameCombination", "UnusedReturnValue"})
public final class MathUtils{
    public static final Interp pow25In = new PowIn(25f);

    public static final Quat q1 = new Quat(), q2 = new Quat();
    public static final Mat3D m31 = new Mat3D(), m32 = new Mat3D();
    public static final Ray ray1 = new Ray();
    public static final Rand seedr = new Rand(), seedr2 = new Rand();

    private static final Vec2 v1 = new Vec2(), v2 = new Vec2(), v3 = new Vec2();

    private MathUtils(){
        throw new AssertionError();
    }

    /**
     * Code taken from <a href="https://github.com/earlygrey/shapedrawer/blob/features/variable-line-width/drawer/src/space/earlygrey/shapedrawer/Joiner.java">
     * the shapes renderer library</a>; calculates the left and right point of a joint path vertex.
     * @param a Path vertex {@code n - 1}.
     * @param b Path vertex {@code n}.
     * @param c Path vertex {@code n + 1}.
     * @param d Left point output vector.
     * @param e Right point output vector.
     * @return The mid-point angle, in radians.
     * @author earlygrey
     */
    public static float pathJoin(Vec2 a, Vec2 b, Vec2 c, Vec2 d, Vec2 e, float halfWidth){
        v1.set(b).sub(a);
        v2.set(c).sub(b);

        float angle = Mathf.atan2(v1.x * v2.x + v1.y * v2.y, v2.x * v1.y - v2.y * v1.x);
        if(Mathf.zero(angle) || Mathf.equal(angle, Mathf.PI2)){
            v1.setLength(halfWidth);
            d.set(-v1.y, v1.x).add(b);
            e.set(v1.y, -v1.x).add(b);

            return angle;
        }

        float len = halfWidth / Mathf.sin(angle);
        boolean bendsLeft = angle < 0f;

        v1.setLength(len);
        v2.setLength(len);

        (bendsLeft ? d : e).set(b).sub(v1).add(v2);
        (bendsLeft ? e : d).set(b).add(v1).sub(v2);
        return angle;
    }

    /**
     * Code taken from <a href="https://github.com/earlygrey/shapedrawer/blob/features/variable-line-width/drawer/src/space/earlygrey/shapedrawer/Joiner.java">
     * the shapes renderer library</a>; calculates the left and right point of an endpoint path vertex.
     * @param sx Start X.
     * @param sy Start Y.
     * @param ex End X.
     * @param ey End Y.
     * @param d Left point output vector.
     * @param e Right point output vector.
     * @author earlygrey
     */
    public static void pathEnd(float sx, float sy, float ex, float ey, Vec2 d, Vec2 e, float halfWidth){
        v3.set(ex, ey).sub(sx, sy).setLength(halfWidth);
        d.set(v3.y, -v3.x).add(ex, ey);
        e.set(-v3.y, v3.x).add(ex, ey);
    }

    /**
     * @return [0-1] value depending on how close the normal is to {@code 225f}.
     * @author GlennFolker
     */
    public static float shade(float normalAngle){
        return shade(normalAngle, 225f);
    }

    /**
     * @return [0-1] value depending on how close the normal is to the light angle.
     * @author GlennFolker
     */
    public static float shade(float normalAngle, float lightAngle){
        return Angles.angleDist(normalAngle, lightAngle - 180f) / 180f;
    }

    /** @author GlennFolker */
    public static int randomSeedSign(long seed){
        return Mathf.randomSeed(seed, 0, 1) * 2 - 1;
    }

    /**
     * {@link Structs#findMin(Object[], Floatf)} for integer array.
     * @author GlennFolker
     */
    public static int min(IntExtractor ext, int... values){
        if(values == null || values.length <= 0) return 0;
        int result = 0;

        float min = Float.MAX_VALUE;
        for(int value : values){
            float val = ext.get(value);
            if(val < min){
                result = value;
                min = val;
            }
        }

        return result;
    }

    /**
     * {@link #min(IntExtractor, int...)} with additional filter.
     * @author GlennFolker
     */
    public static int min(IntPredicate pred, IntExtractor ext, int... values){
        if(values == null || values.length <= 0) return 0;
        int result = 0;

        float min = Float.MAX_VALUE;
        for(int value : values){
            if(!pred.get(value)) continue;

            float val = ext.get(value);
            if(val < min){
                result = value;
                min = val;
            }
        }

        return result;
    }

    /**
     * {@link Structs#contains(Object[], Object)} for integer array.
     * @author GlennFolker
     */
    public static boolean contains(int target, int... values){
        for(int value : values) if(value == target) return true;
        return false;
    }

    /**
     * Inverse linear interpolation; gets the progression (0-1) from the given interpolated value.
     * @author GlennFolker
     */
    public static float invLerp(float from, float to, float value){
        return (value - from) / (to - from);
    }

    /**
     * {@link #invLerp(float, float, float)} for 2D vectors. Parameters should be collinear.
     * @author GlennFolker
     */
    public static Vec2 invLerp(Vec2 from, Vec2 to, Vec2 value, Vec2 out){
        return out.set(
        invLerp(from.x, to.x, value.x),
        invLerp(from.y, to.y, value.y)
        );
    }

    /**
     * {@link #invLerp(float, float, float)} for 3D vectors. Parameters should be collinear.
     * @author GlennFolker
     */
    public static Vec3 invLerp(Vec3 from, Vec3 to, Vec3 value, Vec3 out){
        return out.set(
        invLerp(from.x, to.x, value.x),
        invLerp(from.y, to.y, value.y),
        invLerp(from.z, to.z, value.z)
        );
    }

    static float curveFunc(float x){
        return 1f / (Mathf.pow(Math.abs(x * 1.3591409f), Mathf.PI) + 1f);
    }

    /**
     * Bad implementation of a curved interpolation.
     * @author EyeOfDarkness
     */
    public static Vec2 curve(float[] pos, int size, float progress, Vec2 out){
        float offset = progress * ((size - 2) / 2f);

        float l1 = 1f - offset;
        float l2 = offset - (size - 4) / 2f;

        float scl = 0f;
        float nx = 0f, ny = 0f;
        for(int i = 0; i < size; i += 2){
            float z = curveFunc((i / 2f) - offset);
            scl += z;
            nx += pos[i] * z;
            ny += pos[i + 1] * z;
        }
        nx /= scl;
        ny /= scl;

        if(l1 > 0){
            nx = Mathf.lerp(nx, pos[0], l1 * l1);
            ny = Mathf.lerp(ny, pos[1], l1 * l1);
        }
        if(l2 > 0){
            nx = Mathf.lerp(nx, pos[size - 2], l2 * l2);
            ny = Mathf.lerp(ny, pos[size - 1], l2 * l2);
        }
        return out.set(nx, ny);
    }

    /** @author EyeOfDarkness */
    public static float slope(float fin, float bias){
        return (fin < bias ? (fin / bias) : 1f - (fin - bias) / (1f - bias));
    }

    public static float angleDistSigned(float a, float b){
        a = Mathf.mod(a, 360f);
        b = Mathf.mod(b, 360f);

        float d = Math.abs(a - b) % 360f;
        int sign = (a - b >= 0f && a - b <= 180f) || (a - b <= -180f && a - b >= -360f) ? 1 : -1;
        return (d > 180f ? 360f - d : d) * sign;
    }

    public static float interp(float x, float x2, float t){
        return (float)(1 - (1 / (1 + exp((t * 2 - 1) / 0.2)))) * (x2 - x) + x;
    }

    public static float sqinterp(float x, float x2, float t){
        return t * t * (x2 - x) + x;
    }

    public interface IntExtractor{
        float get(int value);
    }

    public interface IntPredicate{
        boolean get(int value);
    }
}
