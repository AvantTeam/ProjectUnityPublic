package unity.graphics;

import arc.graphics.*;
import mindustry.graphics.*;

import static arc.graphics.Color.*;

public final class UnityPal{
    public static Color

    monolithLight = valueOf("c0ecff"),
    monolith = valueOf("87ceeb"),
    monolithDark = valueOf("6586b0"),
    monolithAtmosphere = valueOf("001e6360"),

    outline = Pal.darkerMetal,
    darkOutline = valueOf("38383d"),
    darkerOutline = valueOf("2e3142");

    private UnityPal(){
        throw new AssertionError();
    }
}
