package unity.mod;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import mindustry.game.*;
import mindustry.graphics.*;
import unity.annotations.Annotations.*;
import unity.graphics.*;

import static mindustry.Vars.*;

@FactionBase
public enum Faction{
    vanilla("vanilla", Team.sharded.color),
    youngcha("youngcha", Color.valueOf("95c184")),
    koruh("koruh", Color.valueOf("61caff")),
    light("light", Color.valueOf("fffde8")),
    monolith("monolith", UnityPal.monolith),
    dark("dark", Color.valueOf("fc6203")),
    scar("scar", Pal.remove),
    advance("advance", Color.sky),
    imber("imber", Pal.surge),
    plague("plague", Color.valueOf("a3f080")),
    end("end", Color.gray);

    public static final Faction[] all = values();

    public final String name;
    public String localizedName;

    public final Color color;
    public TextureRegion icon;

    public static void init(){
        if(headless) return;
        for(Faction faction : all){
            faction.localizedName = Core.bundle.format("faction." + faction.name, faction.color);
        }
    }

    public void load(){
        icon = Core.atlas.find("unity-faction-"+name+"-icon");
    }

    Faction(String name, Color color){
        this.name = name;
        this.color = color.cpy();
    }

    @Override
    public String toString(){
        return localizedName;
    }
}
