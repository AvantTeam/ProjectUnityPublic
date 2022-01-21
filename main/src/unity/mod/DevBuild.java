package unity.mod;

import mindustry.game.EventType.*;

/**
 * Developer build abstraction, defaults to doing nothing at all.
 * @author GlennFolker
 */
public interface DevBuild{
    /** Called when the mod initializes, sometime around {@link ClientLoadEvent}. */
    default void init(){}

    /** @return {@code true} if this implementation defines a developer build, {@code false} otherwise. */
    default boolean isDev(){
        return false;
    }
}
