package unity.world;

import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import unity.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class WorldState implements WorldStateI{
    protected final OrderedMap<Class<? extends WorldModule>, WorldModule> map = new OrderedMap<>();

    public WorldState(WorldModule... mods){
        for(var mod : mods) add(mod);
    }

    @Override
    public void add(WorldModule mod){
        map.put(mod.getClass(), mod);
    }

    @Override
    public <T extends WorldModule> T remove(Class<T> type){
        return (T)map.remove(type);
    }

    @Override
    public <T extends WorldModule> T get(Class<T> type){
        return (T)map.get(type);
    }

    @Override
    public void write(Writes write){
        write.i(map.size);
        for(var e : map){
            write.str(e.key.getName());

            write.b(e.value.revision());
            e.value.write(write);
        }
    }

    @Override
    public void read(Reads read){
        for(int i = 0, size = read.i(); i < size; i++){
            var name = read.str();
            Class type = ReflectUtils.findc(name);
            if(type == null){
                Log.warn("Ignoring '@' world module as the class does not exist.", name);
                continue;
            }else if(!WorldModule.class.isAssignableFrom(type)){
                Log.warn("Ignoring '@' world module as it is not a world module class.", name);
                continue;
            }

            var mod = get((Class<? extends WorldModule>)type);
            if(mod == null){
                Log.warn("Ignoring '@' world module as it's not present.", name);
                continue;
            }

            mod.read(read, read.b());
        }
    }
}
