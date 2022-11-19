package unity.content.type;

import arc.graphics.*;
import mindustry.type.*;
import unity.util.*;

/**
 * Currently just used to "correct" the region loading.
 * @author GlennFolker
 */
public class PUStatusEffect extends StatusEffect{
    public PUStatusEffect(String name, Color color){
        super(name);
        this.color = color;
    }

    @Override
    public void loadIcon(){
        fullIcon = MiscUtils.reg(this);
        uiIcon = MiscUtils.uiReg(this);
    }
}
