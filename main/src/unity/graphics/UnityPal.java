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
    //graph heat
    coldcolor = valueOf("6bc7ff"),
    heatcolor =  Pal.turretHeat,

    outline = Pal.darkerMetal,
    darkOutline = valueOf("38383d"),
    darkerOutline = valueOf("2e3142"),

    //modular parts ui
    blueprintCol = valueOf("649fb7"),
    blueprintColAccent = valueOf("a6cad9"),
    bgCol = valueOf("323232");



    private UnityPal(){
        throw new AssertionError();
    }
}
