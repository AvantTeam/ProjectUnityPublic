package unity.content.type;

import arc.graphics.*;
import mindustry.type.*;
import unity.util.*;

/**
 * Currently just used to "correct" the region loading.
 * @author GlennFolker
 */
public class PUFluid extends Liquid{
    public PUFluid(String name, Color color){
        super(name, color);
    }

    @Override
    public void loadIcon(){
        String type = gas ? "gas" : "liquid";
        fullIcon = MiscUtils.reg(this, type);
        uiIcon = MiscUtils.uiReg(this, type);
    }
}
