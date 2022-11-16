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
    bgCol = valueOf("323232"),
    bgColMid = valueOf("525252"),

    //koruh
    expLaser = valueOf("F9DBB1"),
    exp = valueOf("84ff00"),
    expMax = valueOf("90ff00"),
    expBack = valueOf("4d8f07"),
    lava = valueOf("ff2a00"),
    lava2 = valueOf("ffcc00"),
    dense = valueOf("ffbeb8"),
    dirium = valueOf("96f7c3"),
    diriumLight = valueOf("ccffe4"),
    coldColor = valueOf("6bc7ff"),
    deepRed = Color.valueOf("f25555"),
    deepBlue = Color.valueOf("554deb"),
    passive = valueOf("61caff"),
    armor = valueOf("e09e75"),

    lancerSap1 = Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.167f),
    lancerSap2 = Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.333f),
    lancerSap3 = Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.5f),
    lancerSap4 = Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.667f),
    lancerSap5 = Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.833f),

    lancerDir1 = Pal.lancerLaser.cpy().lerp(diriumLight, 0.25f),
    lancerDir2 = Pal.lancerLaser.cpy().lerp(diriumLight, 0.5f),
    lancerDir3 = Pal.lancerLaser.cpy().lerp(diriumLight, 0.75f);

    private UnityPal(){
        throw new AssertionError();
    }
}
