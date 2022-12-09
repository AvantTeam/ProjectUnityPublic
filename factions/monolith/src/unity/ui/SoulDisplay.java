package unity.ui;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ui.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.world.meta.*;

import static mindustry.Vars.*;

public class SoulDisplay extends Table{
    public final float amount;
    public final boolean perSecond;

    public SoulDisplay(float amount, boolean perSecond){
        this.amount = amount;
        this.perSecond = perSecond;

        add(new Stack(){{
            add(new Image(PUIcon.soul).setScaling(Scaling.fit));

            if(amount != 0f){
                Table t = new Table().left().bottom();
                t.add(Strings.autoFixed(amount, 2)).style(Styles.outlineLabel);
                add(t);
            }
        }}).size(iconMed).padRight(3f + (amount != 0f && Strings.autoFixed(amount, 2).length() > 2 ? 8f : 0f));

        if(perSecond){
            add(StatUnit.perSecond.localized()).padLeft(2).padRight(5).color(Color.lightGray).style(Styles.outlineLabel);
        }

        add(PUStatCat.soul.localized());
    }
}
