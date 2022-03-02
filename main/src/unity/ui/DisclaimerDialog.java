package unity.ui;

import arc.scene.actions.*;
import arc.scene.ui.*;
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
        delayButton("@ok", this::hide, 8, 5);
        if(shouldSkip()) return;
        delayButton("@mod.disclaimer.skip", () -> {
            hide();
            settings.put("mod.disclaimer.skip", true);
        }, 17, 5);
    }

    void delayButton(String text, Runnable listener, float delay, float fadeTime){
        TextButton b = buttons.button(text, listener).get();
        if(shouldSkip()) return;
        //Add a delay to when the ok button can be pressed. Read the damn disclaimer and don't ignore it like you do with every terms and conditions list you see.
        b.setDisabled(() -> b.color.a < 1);
        b.actions(
            Actions.alpha(0),
            Actions.delay(delay),
            Actions.fadeIn(fadeTime)
        );
        b.getStyle().disabledFontColor = b.getStyle().fontColor;
        b.getStyle().disabled = b.getStyle().up;
    }

    boolean shouldSkip(){
        return settings.getBool("mod.disclaimer.skip", false) || Unity.dev.isDev();
    }
}
