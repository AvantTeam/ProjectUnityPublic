package unity.util;

import arc.math.*;
import arc.math.Interp.*;
import arc.math.geom.*;

public final class Utils{
    public static final PowIn pow6In = new PowIn(6);

    public static final float sqrtHalf = Mathf.sqrt(0.5f);

    public static final Quat
        q1 = new Quat(),
        q2 = new Quat(),
        q3 = new Quat();

    private Utils(){
        throw new AssertionError();
    }

    public static void init(){
        //...
    }
}
