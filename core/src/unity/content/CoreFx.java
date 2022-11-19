package unity.content;

import mindustry.entities.*;
import mindustry.graphics.*;

import static mindustry.Vars.state;

/**
 * Defines all shared faction-less effect types. Specific effects should be defined in the corresponding submodules, optionally
 * copying/referencing effects from this class to avoid recompilation as much as possible.
 * @author GlennFolker
 */
public final class CoreFx{
    public static final Effect
    trailFadeLow = new Effect(400f, e -> {
        if(!(e.data instanceof Trail trail)) return;
        e.lifetime = trail.length * 1.4f;

        if(!state.isPaused()) trail.shorten();
        trail.drawCap(e.color, e.rotation);
        trail.draw(e.color, e.rotation);
    }).layer(Layer.flyingUnitLow - 0.001f);

    private CoreFx(){
        throw new AssertionError();
    }
}
