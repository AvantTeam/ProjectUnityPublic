package unity.parts.stat;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;
import unity.content.*;
import unity.parts.*;
import unity.util.*;

public class WeaponMountStat extends ModularPartStat{
    Weapon baseweapon;

    public WeaponMountStat(Weapon w){
        super("weapon");
        baseweapon = w.copy();
    }

    @Override
    public void merge(ModularPartStatMap id, ModularPart part){
        if(id.has("weapons")){
            var weaponsarr = id.stats.getList("weapons");
            ValueMap weapon = new ValueMap();
            weapon.put("part", part);
            Weapon copy = baseweapon.copy();
            copy.x = part.getCx()*ModularPartType.partSize;
            copy.y = part.getCy()*ModularPartType.partSize;
            weapon.put("weapon", copy);
            weaponsarr.add(weapon);
        }
    }

    @Override
    public void mergePost(ModularPartStatMap id, ModularPart part){

    }

    @Override
    public void display(Table e){
        e.row();
        e.table(t->weapons(baseweapon).display(t));
    }

    public static StatValue weapons( Weapon weapon){
        if(weapon.region==null){
            weapon.load(); // o-o
        }
        return table -> {
           TextureRegion region = !weapon.name.equals("") && weapon.outlineRegion.found() ? weapon.outlineRegion: weapon.region;

           table.image(region).size(60).scaling(Scaling.bounded).right().top();

           table.table(Tex.underline, w -> {
               w.left().defaults().padRight(3).left();

               addStats(weapon, w);
           }).padTop(-9).left();
           table.row();
        };
    }

    public static void addStats(Weapon u, Table t){
        if(u.inaccuracy > 0){
            t.row();
            t.add("[lightgray]" + Stat.inaccuracy.localized() + ": [white]" + (int)u.inaccuracy + " " + StatUnit.degrees.localized());
        }
        t.row();
        t.add("[lightgray]" + Stat.reload.localized() + ": " + (u.mirror ? "2x " : "") + "[white]" + Strings.autoFixed(60f / u.reload * u.shoot.shots, 2) + " " + StatUnit.perSecond.localized());

        StatValues.ammo(ObjectMap.of(UnityUnitTypes.modularUnitSmall, u.bullet)).display(t);
    }
}
