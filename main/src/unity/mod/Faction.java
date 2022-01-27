package unity.mod;

import arc.*;
import arc.graphics.*;
import unity.annotations.Annotations.*;
import unity.graphics.*;

import static mindustry.Vars.*;

@FactionBase
public enum Faction{
    koruh("koruh", Color.valueOf("61caff")),
    youngcha("youngcha", Color.valueOf("a69f95")),
    monolith("monolith", UnityPal.monolith);

    public static final Faction[] all = values();

    public final String name;
    public String localizedName;

    public final Color color;

    public static void init(){
        if(headless) return;
        for(Faction faction : all){
            faction.localizedName = Core.bundle.format("faction." + faction.name, faction.color);
        }
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
