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
            weapon.put("name", baseweapon.name);
            weapon.put("reload", baseweapon.reload);
            weapon.put("shots", baseweapon.shots);
            weapon.put("shotDelay", baseweapon.shotDelay);
            weapon.put("shootX", baseweapon.shootX);
            weapon.put("shootY", baseweapon.shootY);
            weapon.put("rotate", baseweapon.rotate);
            weapon.put("x", part.getAx()*ModularPartType.partSize);
            weapon.put("y", part.getAy()*ModularPartType.partSize);
            weaponsarr.put(weapon);
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){

    }
}
