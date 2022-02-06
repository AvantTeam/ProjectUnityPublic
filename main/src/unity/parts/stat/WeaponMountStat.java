package unity.parts.stat;

import arc.audio.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import org.json.*;
import unity.parts.*;

public class WeaponMountStat extends ModularPartStat{
    Weapon baseweapon;

    public WeaponMountStat(Weapon w){
        super("weapon");
        baseweapon = w.copy();
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(id.has("weapons")){
            var weaponsarr = id.stats.getJSONArray("weapons");
            JSONObject weapon = new JSONObject();
            weapon.put("part", part);
            Weapon copy = baseweapon.copy();
            copy.x = part.getCx()*ModularPartType.partSize;
            copy.y = part.getCy()*ModularPartType.partSize;
            weapon.put("weapon", copy);
            weaponsarr.put(weapon);
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){

    }
}
