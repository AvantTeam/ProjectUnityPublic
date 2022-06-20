package unity.ui;

import arc.math.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ui.dialogs.*;
import unity.*;

import static arc.Core.*;

public class DisclaimerDialog extends BaseDialog{
    public DisclaimerDialog(){
        super("@mod.disclaimer.title");
        cont.add("@mod.disclaimer.text").width(500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
        buttons.defaults().size(200f, 54f).pad(2f);
        setFillParent(false);

        TextButton b = buttons.button("@ok", this::hide).get();

        if(shouldSkip()) return;

        b.setDisabled(() -> b.color.a < 1);
        float moveDst = Scl.scl(101f); //Half button width + Half pad
        b.actions(
            Actions.alpha(0), Actions.moveBy(moveDst, 0f),
            Actions.delay(8f),
            Actions.fadeIn(5f),
            Actions.delay(4f),
            Actions.moveBy(-moveDst, 0f, 5f, Interp.smoother)
        );
        b.getStyle().disabledFontColor = b.getStyle().fontColor;
        b.getStyle().disabled = b.getStyle().up;

        TextButton s = buttons.button("@mod.disclaimer.skip", () -> {
            hide();
            settings.put("mod.disclaimer.skip", true);
        }).get();
        s.setDisabled(() -> s.color.a < 1);
        s.actions(
            Actions.alpha(0),
            Actions.delay(17f),
            Actions.fadeIn(5f)
        );
        s.getStyle().disabledFontColor = s.getStyle().fontColor;
        s.getStyle().disabled = s.getStyle().up;
    }

    boolean shouldSkip(){
        return settings.getBool("mod.disclaimer.skip", false) || Unity.dev.isDev();
    }
}
