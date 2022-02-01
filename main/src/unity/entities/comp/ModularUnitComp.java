package unity.entities.comp;

import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import org.json.*;
import unity.annotations.Annotations.*;
import unity.parts.*;

import java.util.*;

//Ok we need to essentially replace nearly every usage of UnitType bc the stats dont come from there anymore

@SuppressWarnings("unused")
@EntityComponent
abstract class ModularUnitComp implements Unitc, ElevationMovec{
    @Import UnitType type;
    @Import boolean dead;
    @Import float health, maxHealth, rotation;
    @Import int id;
    @Import UnitController controller;
    @Import WeaponMount[] mounts;
    transient ModularConstruct construct;
    transient boolean constructLoaded = false;
    public byte[] constructdata;

    @Override
    public void add(){
        if(ModularConstruct.cache.containsKey(this)){
            construct = new ModularConstruct(ModularConstruct.cache.get(this));
        }else{
            if(constructdata!=null){
                construct = new ModularConstruct(constructdata);
            }else{
                construct = ModularConstruct.test;
            }
        }
        constructdata = Arrays.copyOf(construct.data,construct.data.length);

        var statmap = new ModularUnitStatMap();
        ModularConstructBuilder.getStats(construct.parts,statmap);
        applyStatMap(statmap);
        if(construct!=ModularConstruct.test){
            constructLoaded = true;
        }
    }

    public void applyStatMap(ModularUnitStatMap statmap){
        float hratio = Mathf.clamp(this.health/this.maxHealth);
        this.maxHealth =  statmap.getOrCreate("health").getFloat("value");
        this.health = hratio*this.maxHealth;
        var weapons = statmap.stats.getJSONArray("weapons");
        mounts = new WeaponMount[weapons.length()];
        for(int i = 0;i<weapons.length();i++){
            var weapon = getWeaponFromStat(weapons.getJSONObject(i));
            weapon.load();
            mounts[i] = weapon.mountType.get(weapon);
        }
    }

    public void initWeapon(Weapon w){
        if(w.recoilTime < 0) w.recoilTime = w.reload;
    }

    public Weapon getWeaponFromStat(JSONObject weaponstat){
        Weapon weapon = new Weapon(weaponstat.getString("name"));
        weapon.reload = weaponstat.getFloat("reload");
        weapon.shots = weaponstat.getInt("shots");
        weapon.shotDelay = weaponstat.getFloat("shotDelay");
        weapon.shootX= weaponstat.getFloat("shootX");
        weapon.shootY= weaponstat.getFloat("shootY");
        weapon.rotate= weaponstat.getBoolean("rotate");
        weapon.x= weaponstat.getFloat("x");
        weapon.y= weaponstat.getFloat("y");
        weapon.bullet = Bullets.standardCopper;
        initWeapon(weapon);
        return weapon;
    }

    @Override
    public void setType(UnitType type){
        /*
        this.drag = type.drag;
        this.armor = type.armor;
        this.hitSize = type.hitSize;
        this.hovering = type.hovering;


        if(mounts().length != type.weapons.size) setupWeapons(type);
        if(abilities.size != type.abilities.size){
            abilities = type.abilities.map(Ability::copy);
        }*/
    }

    @Override
    public void update(){
        if(construct!=null && constructdata==null){
            Log.info("uh constructdata died");
            constructdata= Arrays.copyOf(construct.data,construct.data.length);
        }
    }


}
