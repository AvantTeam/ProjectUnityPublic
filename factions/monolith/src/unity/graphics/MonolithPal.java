package unity.graphics;

import arc.graphics.*;
import unity.mod.*;

/**
 * {@linkplain Faction#monolith Monolith}-specific palettes.
 * @author GlennFolker
 */
public final class MonolithPal{
    public static final Color
    monolithLighter = new Color(0x9cd4f8ff),
    monolithLight = new Color(0x72a2d7ff),
    monolithMid = Palettes.monolith,
    monolithDark = new Color(0x354d97ff),
    monolithDarker = new Color(0x253080ff),

    monolithOutline = Palettes.darkOutline;

    private MonolithPal(){
        throw new AssertionError();
    }
}
