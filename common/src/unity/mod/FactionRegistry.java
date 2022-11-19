package unity.mod;

import arc.struct.*;
import arc.util.*;

/**
 * Registry for mapping arbitrary contents with factions.
 * @author GlennFolker
 */
@SuppressWarnings("unchecked")
public final class FactionRegistry{
    private static final Seq<Object> contents = new Seq<>();

    private static final OrderedSet<Object>[] divisions = new OrderedSet[Faction.all.length];
    private static final OrderedMap<Object, Faction> registry = new OrderedMap<>();

    static{
        for(int i = 0; i < Faction.all.length; i++) divisions[i] = new OrderedSet<>();
    }

    private FactionRegistry(){
        throw new AssertionError();
    }

    public static <T> T register(Faction faction, T content){
        divisions[faction.ordinal()].add(content);

        Faction before = registry.put(content, faction);
        if(before != null && before != faction) divisions[before.ordinal()].remove(content);

        return content;
    }

    /** @return The faction associated with this content, or null if not any. */
    public static @Nullable Faction faction(Object content){
        return registry.get(content);
    }

    /**
     * @return An array containing all the faction's contents with the specified type. Note that the same array instance
     * is always returned; modification is not recommended.
     */
    public static <T> Seq<? extends T> contents(Faction faction, Class<T> type){
        contents.clear();
        for(Object content : divisions[faction.ordinal()]) if(type.isAssignableFrom(content.getClass())) contents.add(content);

        return contents.as();
    }
}
