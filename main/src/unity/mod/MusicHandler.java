package unity.mod;

import arc.audio.*;
import arc.func.*;
import arc.util.*;
import unity.*;

/**
 * A default interface for Project Unity's music handler. Further implementations are done
 * in another mod, for the sake of mod size, performance, and preference.
 * @author GlennFolker
 */
@SuppressWarnings("unused")
public interface MusicHandler{
    /** Called in {@link Unity#init()}. */
    default void init(){}

    default void registerLoop(String name, Music loop){
        registerLoop(name, loop, loop);
    }

    /** Override. Should register loops by name. */
    default void registerLoop(String name, Music intro, Music loop){}

    default void play(String name){
        play(name, null);
    }

    /**
     * Override. Should add another player to data, of which the music is
     * played by finding the data with the maximum players.
     */
    default void play(String name, Boolp predicate){}
}
