package unity.graphics;

import arc.*;
import arc.scene.style.*;

public final class PUIcon{
    public static final TextureRegionDrawable
    soul = new TextureRegionDrawable();

    private PUIcon(){
        throw new AssertionError();
    }

    public static void load(){
        load(soul, "soul");
    }

    private static void load(TextureRegionDrawable drawable, String name){
        drawable.setRegion(Core.atlas.find("unity-" + name + "-icon"));
    }
}
