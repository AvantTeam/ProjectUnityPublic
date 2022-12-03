package unity;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.game.EventType.*;
import mindustry.io.*;
import mindustry.io.SaveFileReader.*;
import unity.assets.list.*;
import unity.content.*;
import unity.gen.entities.*;
import unity.gen.graph.*;
import unity.graphics.*;
import unity.io.*;
import unity.mod.*;
import unity.util.*;
import unity.world.*;

import java.io.*;

import static mindustry.Vars.*;

public class ProjectUnity extends ProjectUnityI{
    public ProjectUnity(){
        try{
            Class<DevBuildImpl> type = ReflectUtils.findc("unity.DevBuildImpl");
            if(type != null){
                dev = ReflectUtils.inst(type);
                Log.info("Successfully instantiated developer build.");
            }
        }catch(Throwable t){
            Throwable cause = t.getCause();
            if(cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError){
                Log.info("Defaulting to user build.");
            }else{
                Log.err("Error while trying to instantiate developer build", t);
            }
        }finally{
            if(dev == null) dev = new DevBuild(){};
        }

        Events.on(FileTreeInitEvent.class, e -> Core.app.post(() -> {
            PUSounds.load();
            PUShaders.load();
            PUCacheLayer.load();

            try(Reader file = tree.get("meta/classes.out").reader();
                BufferedReader reader = new BufferedReader(file)
            ){
                Seq<String> current = packages;

                String line;
                while((line = reader.readLine()) != null){
                    switch(line){
                        case "Packages:" -> current = packages;
                        case "Classes:" -> current = classes;
                        default -> current.add(line);
                    }
                }

                classes.removeAll(str -> ReflectUtils.findc(str) == null);
            }catch(IOException ex){
                throw new RuntimeException(ex);
            }
        }));

        Events.on(ContentInitEvent.class, e -> Core.app.post(() -> {
            Faction.load();
            Graphs.load();
        }));

        Core.app.post(() -> {
            dev.setup();
            JSBridge.setup();

            PUPackets.register();
        });

        worldState = new WorldState();
        SaveVersion.addCustomChunk("unity-io-world-state", new CustomChunk(){
            final Writes write = new Writes(null);
            final Reads read = new Reads(null);

            @Override
            public void write(DataOutput stream) throws IOException{
                write.output = stream;
                worldState.write(write);
            }

            @Override
            public void read(DataInput stream) throws IOException{
                read.input = stream;
                worldState.read(read);
            }
        });
    }

    @Override
    public void init(){
        JSBridge.importDefaults(JSBridge.defaultScope);
        dev.init();
    }

    @Override
    public void loadContent(){
        EntityRegistry.register();
        Faction.init();

        PUStatusEffects.load();

        MonolithItems.load();
        MonolithStatusEffects.load();
        MonolithFluids.load();
        MonolithAttributes.load();
        MonolithUnitTypes.load();
        MonolithBlocks.load();
        MonolithPlanets.load();

        YoungchaBlocks.load();
    }
}
