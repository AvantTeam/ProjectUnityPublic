package unity.util;

import arc.*;
import arc.graphics.g2d.TextureAtlas.*;
import mindustry.ctype.*;

/** Intermediately-shared utility access for miscellaneous operations, such as convenient codes relating to contents. */
public final class MiscUtils{
    private MiscUtils(){}

    public static AtlasRegion reg(MappableContent content){
        return reg(content, content.getContentType().name());
    }

    /**
     * Similar to the lookup in {@link UnlockableContent#loadIcon()} for full icons, but adjusted so that modded content sprites
     * can be prefixed with its content type.
     * @author GlennFolker
     */
    public static AtlasRegion reg(MappableContent content, String type){
        // Strip away "unity-".
        String name = content.name, mod = "";
        if(content.minfo.mod != null) name = name.substring((mod = content.minfo.mod.name + "-").length());

        type += "-";
        return (AtlasRegion)
            Core.atlas.find(mod + type + name + "-full",
            Core.atlas.find(mod + type + name,
            Core.atlas.find(mod + name + "-full",
            Core.atlas.find(mod + name,
            Core.atlas.find(mod + name + "1")))));
    }

    public static AtlasRegion uiReg(MappableContent content){
        return uiReg(content, content.getContentType().name());
    }

    /**
     * {@link #reg(MappableContent, String)} prefixed with {@code "-ui"}, defaulting to the full icon itself if the UI icon is not
     * present.
     * @author GlennFolker
     */
    public static AtlasRegion uiReg(MappableContent content, String type){
        AtlasRegion full = reg(content, type);
        if(!full.found()) return full;

        // The found region might be an animated region; if that's the case, strip away the animation frame ID.
        String name = full.name;
        if(name.endsWith("1")) name = name.substring(0, name.length() - 1);

        return (AtlasRegion)Core.atlas.find(name + "-ui", full);
    }
}
