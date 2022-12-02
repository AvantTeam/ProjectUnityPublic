package unity;

import arc.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.mod.Mods.*;
import mindustry.mod.*;

import static mindustry.Vars.*;

/**
 * A development server plugin for various testing supports.
 * @author GlennFolker
 */
public class ProjectUnityPlugin extends Plugin{
    public static boolean linked;
    public static LoadedMod mod;

    public ProjectUnityPlugin(){
        Core.app.post(() -> {
            try{
                linked = (mod = mods.getMod(ProjectUnity.class)) != null;
            }catch(NoClassDefFoundError e){
                linked = false;
                Log.warn("This plugin should be used with the ProjectUnity mod.");
            }
        });
    }

    public static boolean depLoaded(){
        return linked && mod != null && mod.state == ModState.enabled;
    }

    @Override
    public void registerServerCommands(CommandHandler handler){}

    @Override
    public void registerClientCommands(CommandHandler handler){
        if(!depLoaded()) return;
        handler.<Player>register("js", "<script...>", "Executes arbitrary JS codes in the server.", (args, player) -> {
            String script = args[0].replace("##self", "(Groups.player.getByID(" + player.id + "))");
            Call.sendMessage(player.name + " ran: " + script);
            Call.sendMessage("Result: " + mods.getScripts().runConsole(script));
        });
    }
}
