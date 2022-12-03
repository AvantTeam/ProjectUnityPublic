package unity;

import arc.struct.*;
import mindustry.mod.*;
import unity.mod.*;
import unity.world.*;

@SuppressWarnings("unchecked")
public abstract class ProjectUnityI extends Mod{
    public static DevBuild dev;
    public static final Seq<String> classes = new Seq<>(), packages = new Seq<>();

    public static WorldStateI worldState;

    public static <T extends WorldStateI> T worldState(){
        return (T)worldState;
    }
}
