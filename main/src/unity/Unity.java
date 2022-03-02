package unity;

import arc.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.*;
import mindustry.world.blocks.environment.*;
import unity.annotations.Annotations.*;
import unity.content.*;
import unity.content.blocks.*;
import unity.gen.*;
import unity.graphics.*;
import unity.mod.*;
import unity.parts.*;
import unity.ui.*;
import unity.util.*;

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
                UnitySettings.init();
                //Vars.ui.showOkText("@mod.disclaimer.title", "@mod.disclaimer.text", () -> {});
                BaseDialog dialog = new BaseDialog("@mod.disclaimer.title");
                dialog.cont.add("@mod.disclaimer.text").width(500f).wrap().pad(4f).get().setAlignment(Align.center, Align.center);
                dialog.buttons.defaults().size(200f, 54f).pad(2f);
                dialog.setFillParent(false);
                TextButton b = dialog.buttons.button("@ok", dialog::hide).get();
                //Add a delay to when the ok button can be pressed. Read the damn disclaimer and don't ignore it like you do with every terms and conditions list you see.
                b.setDisabled(() -> b.color.a < 1);
                b.actions(
                    Actions.alpha(0),
                    Actions.delay(10),
                    Actions.fadeIn(2)
                );
                b.getStyle().disabledFontColor = b.getStyle().fontColor;
                b.getStyle().disabled = b.getStyle().up;
                dialog.show();

                //bc they are not a contentType
                ModularPartType.loadStatic();
                for(var en: ModularPartType.partMap){
                    en.value.load();
                }
                for(Faction faction : Faction.all){
                    faction.load();
                }
                UnityParts.loadDoodads();
            });

            Events.on(FileTreeInitEvent.class, e -> Core.app.post(UnityShaders::load));

            Events.on(DisposeEvent.class, e -> {
                UnityShaders.dispose();
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


        logContent();
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
