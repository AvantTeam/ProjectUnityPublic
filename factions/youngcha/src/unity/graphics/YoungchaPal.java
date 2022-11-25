package unity.graphics;

import arc.graphics.*;
import mindustry.graphics.*;
import unity.mod.*;

/**
 * {@linkplain Faction#youngcha Youngcha}-specific palettes.
 * @author Xelo
 */
public final class YoungchaPal{
    public static final Color
    coldColor = new Color(0x6bc7ffff),
    heatColor =  Pal.turretHeat.cpy();

    private YoungchaPal(){
        throw new AssertionError();
    }
}
