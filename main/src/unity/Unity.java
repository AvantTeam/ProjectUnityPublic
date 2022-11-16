package unity;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

import mindustry.ui.dialogs.*;
import mindustry.ui.dialogs.JoinDialog.*;

import mindustry.ui.fragments.*;

import mindustry.world.blocks.environment.*;
import unity.annotations.Annotations.*;
import unity.content.*;
import unity.content.blocks.*;
import unity.gen.*;
import unity.graphics.*;

import unity.graphics.menu.*;

import unity.mod.*;
import unity.net.*;
import unity.parts.*;
import unity.ui.*;
import unity.util.*;
import unity.world.graph.*;
import unity.world.systems.*;

import static mindustry.Vars.*;

/**
 * The mod's main mod class. Contains static references to other modules.
 * @author Avant Team
 */
@LoadRegs("error") // Need this temporarily, so the class gets generated.
@SuppressWarnings("unchecked")
public class Unity extends Mod{
    /** Whether the mod is in an asset-processing context. */
    public static boolean tools = false;

    /** Abstract developer build specification; dev builds allow users to have various developer accessibility. */
    public static DevBuild dev;
    /** Abstract music handler; will be overridden in a separate music mod. */
    public static MusicHandler music;

    /** Lists all the mod's classes by their canonical names. Generated at compile-time. */
    @ListClasses
    public static Seq<String> classes = Seq.with();
    /** Lists all the mod's packages by their canonical names. Generated at compile-time. */
    @ListPackages
    public static Seq<String> packages = Seq.with();

    /**UI**/
    public static UnityUI ui= new UnityUI();

    /**manages the liquids**/
    public static GroundFluidControl groundFluidControl;

    /** Default constructor for Mindustry mod loader to instantiate. */
    public Unity(){
        this(false);
    }

    /**
     * Constructs the mod, and binds several functionality to the game under certain circumstances.
     * @param tools Whether the mod is in an asset-processing context.
     */
    public Unity(boolean tools){
        Unity.tools = tools;

        if(!headless){
            // Load assets once they're added into `Vars.tree`.
            Events.on(FileTreeInitEvent.class, e -> Core.app.post(UnitySounds::load));

            // Disclaimer, because apparently we're stupid enough to need this
            Events.on(ClientLoadEvent.class, e -> {
                // Might break on mobile
                try{
                    Reflect.set(MenuFragment.class, Vars.ui.menufrag, "renderer", new UnityMenuRenderer());
                }catch(Exception ex){
                    Log.err("Failed to replace renderer", ex);
                }

                UnitySettings.init();
                Vars.ui.showOkText("@mod.disclaimer.title", "@mod.disclaimer.text", () -> {});

                //bc they are not a contentType
                ModularPartType.loadStatic();
                for(var en: ModularPartType.partMap){
                    en.value.load();
                }
                for(Faction faction : Faction.all){
                    faction.load();
                }
                Graphs.load();
                UnityParts.loadDoodads();

                if(dev.isDev()){
                    Seq<Server> servers = ReflectUtils.getFieldValue(Vars.ui.join, JoinDialog.class,"servers");
                    boolean found = false;
                    for(Server s:servers){
                        if(s.ip.equals("mindustry.xeloboyo.art") || s.ip.equals("172.105.174.77")){
                            found = true;
                        }
                    }
                    if(!found){
                        Server xeloserver = new Server();
                        xeloserver.ip = "mindustry.xeloboyo.art";
                        servers.add(xeloserver);
                        ReflectUtils.invokeMethod(Vars.ui.join,"setupRemote");
                        ReflectUtils.invokeMethod(Vars.ui.join,"refreshRemote");

                    }

                }
            });

            Events.on(FileTreeInitEvent.class, e -> Core.app.post(UnityShaders::load));

            Events.on(DisposeEvent.class, e -> {
                UnityShaders.dispose();
            });
        }else{
            Events.run(Trigger.update , ()->{
                if(Utils.isCrash){
                    throw new RuntimeException("DEATH");
                }
            });
        }

        Events.on(ContentInitEvent.class, e -> {
            if(!headless){
                Regions.load();
            }
        });

        Utils.init();

        try{
            Class<? extends DevBuild> impl = (Class<? extends DevBuild>)Class.forName("unity.mod.DevBuildImpl", true, mods.mainLoader());
            dev = impl.getDeclaredConstructor().newInstance();

            Log.info("Dev build class implementation found and instantiated.");
        }catch(Throwable e){
            dev = new DevBuild(){};
            Log.info("Dev build class implementation not found; defaulting to regular user implementation.");
        }

        music = new MusicHandler(){};

        Core.app.post(() -> {
            JSBridge.init();
            JSBridge.importDefaults(JSBridge.unityScope);
        });


    }

    @Override
    public void init(){
        dev.init();
        music.init();
        ui.init();
        UnityCalls.registerPackets();;
        groundFluidControl = new GroundFluidControl();
    }

    @Override
    public void loadContent(){


        Faction.init();
        UnityItems.load();
        UnityStatusEffects.load();
        UnityLiquids.load();
        UnityBullets.load();
        UnityUnitTypes.load();
        KoruhBlocks.load();
        YoungchaBlocks.load();
        UnityParts.load();

        //below has to be done after all things with faction tags are loaded.
        FactionMeta.init();
        UnityEntityMapping.init();

        GroundFluidControl.initialiseContent();
        //logContent();
    }

    public void logContent(){
        for(Faction faction : Faction.all){
            Seq<Object> array = FactionMeta.getByFaction(faction, Object.class);
            Log.debug("Faction @ has @ contents", faction, array.size);
        }

        Seq<Class<?>> ignoreDesc = Seq.with(Floor.class, Prop.class);
        for(Seq<Content> content : content.getContentMap()){
            for(Content c : content){
                if(c.minfo.mod == null || c.minfo.mod.main != this || !(c instanceof UnlockableContent cont)) continue;

                if(Core.bundle.getOrNull(cont.getContentType() + "." + cont.name + ".name") == null){
                    Log.debug("@ has no bundle entry for name.", cont);
                }

                if(
                    !ignoreDesc.contains(t -> t.isAssignableFrom(cont.getClass())) &&
                    Core.bundle.getOrNull(cont.getContentType() + "." + cont.name + ".description") == null
                ){
                    Log.debug("@ has no bundle entry for description.", cont);
                }
            }
        }
    }
}
