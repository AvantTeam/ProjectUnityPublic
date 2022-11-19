package unity.io;

import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.io.*;
import unity.annotations.Annotations.*;
import unity.util.*;

import static mindustry.Vars.world;

/** Custom IO for certain types that aren't handled in {@link TypeIO}. */
@TypeIOHandler
public final class PUTypeIO{
    private PUTypeIO(){
        throw new AssertionError();
    }

    public static void writeHealthc(Writes write, Healthc e){
        if(e instanceof Unit u){
            write.b(0);
            write.i(u.id);
        }else if(e instanceof Building b){
            write.b(1);
            write.i(b.pos());
        }else if(e instanceof Syncc s){
            write.b(2);
            write.i(s.id());
        }else{
            write.b(3);
            if(e != null) Log.debug("Unhandled Healthc overload: @", ReflectUtils.known(e.getClass()));
        }
    }

    public static Healthc readHealthc(Reads read){
        return switch(read.b()){
            case 0 -> Groups.unit.getByID(read.i());
            case 1 -> world.build(read.i());
            case 2 -> (Healthc)Groups.sync.getByID(read.i());
            default -> null;
        };
    }
}
