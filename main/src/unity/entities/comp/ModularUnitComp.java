package unity.entities.comp;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.entities.abilities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import unity.annotations.Annotations.*;
import unity.content.*;
import unity.content.effects.*;
import unity.parts.*;
import unity.parts.PanelDoodadType.*;
import unity.parts.types.*;
import unity.type.*;
import unity.util.*;

import java.util.*;

import static mindustry.Vars.*;
import static unity.util.Utils.getFloat;


//Ok we need to essentially replace nearly every usage of UnitType bc the stats dont come from there anymore

@SuppressWarnings("unused")
@EntityComponent
abstract class ModularUnitComp implements Unitc, ElevationMovec{
    @Import
    UnitType type;
    @Import
    boolean dead;
    @Import
    float health, maxHealth, rotation, armor,drownTime;
    @Import
    int id;
    @Import
    UnitController controller;
    @Import
    WeaponMount[] mounts;
    @Import
    float minFormationSpeed;
    @Import
    float elevation;
    @Import
    public transient Seq<Unit> controlling;
    @Import
    public transient Floor lastDrownFloor;

    transient ModularConstruct construct;
    transient boolean constructLoaded = false;
    public transient Seq<PanelDoodad> doodadlist = new Seq<>();
    public byte[] constructdata;
    //todo: PREPARE FOR COSMETICS!
    //visuals
    public transient float driveDist = 0;
    public transient float clipsize = 0;
    public transient float[] partTransientProp;
    //stat
    public transient float enginepower = 0;
    public transient float speed = 0;
    public transient float rotateSpeed = 0;
    public transient float massStat = 0;
    public transient float weaponrange = 0;
    public transient int itemcap = 0;
    public transient boolean differentialSteer = true;

    public transient float statHp = 0;


    @Override
    public void add(){
        if(ModularConstruct.cache.containsKey(this)){
            construct = ModularConstruct.cache.get(this);
            partTransientProp = new float[construct.partlist.size];
        }else{
            if(constructdata != null){
                construct = ModularConstruct.get(constructdata);
                partTransientProp = new float[construct.partlist.size];
            }else{
                String templatestr = ((UnityUnitType)type).templates.random();
                constructdata = Base64.getDecoder().decode(templatestr.trim().replaceAll("[\\t\\n\\r]+", ""));
                construct = ModularConstruct.get(constructdata);
                partTransientProp = new float[construct.partlist.size];
            }
        }
        var compdata = construct.getCompressedData();
        constructdata = Arrays.copyOf(compdata, compdata.length);

        var statmap = construct.getStatMap(ModularUnitStatMap.class);
        applyStatMap(statmap);
        if(construct != ModularConstruct.placeholder){
            constructLoaded = true;
            if(!headless){
                UnitDoodadGenerator.initDoodads(construct.parts.length, doodadlist, construct);
            }
            int w = construct.parts.length;
            int h = construct.parts[0].length;
            int maxx = 0, minx = 256;
            int maxy = 0, miny = 256;

            for(int j = 0; j < h; j++){
                for(int i = 0; i < w; i++){
                    if(construct.parts[i][j] != null){
                        maxx = Math.max(i, maxx);
                        minx = Math.min(i, minx);
                        maxy = Math.max(j, maxy);
                        miny = Math.min(j, miny);
                    }
                }
            }
            clipsize = Mathf.dst((maxy - miny), (maxx - minx)) * ModularPartType.partSize * 6;
            hitSize(((maxy - miny) + (maxx - minx)) * 0.5f * ModularPartType.partSize);
        }
    }

    public void applyStatMap(ModularUnitStatMap statmap){
        Log.info("Applying stats to unit...");
        if(construct.parts.length == 0){
            Log.info("construct has no parts!");
            return;
        }
        Log.info("Construct size: "+construct.parts.length+" by "+construct.parts[0].length);
        Log.info(statmap);
        float power = statmap.power;
        float poweruse = statmap.powerUsage;
        float eff = Mathf.clamp(power / poweruse, 0, 1);

        float hratio = Mathf.clamp(this.health / this.maxHealth);
        this.maxHealth = statmap.health;
        statHp = maxHealth;
        if(savedHp<=0){
            this.health = hratio * this.maxHealth;
        }else{
            this.health = savedHp;
            savedHp = -1;
        }
        var weapons = statmap.stats.getList("weapons");
        mounts = new WeaponMount[weapons.length()];
        weaponrange = 0;

        int weaponslots = statmap.weaponSlots;
        int weaponslotsused = statmap.weaponslotuse;

        for(int i = 0; i < weapons.length(); i++){
            var weapon = getWeaponFromStat(weapons.getMap(i));
            weapon.reload *= 1f / eff;
            if(weaponslotsused>weaponslots){
                weapon.reload *= 4f*(weaponslotsused-weaponslots);
            }
            if(!headless){
                weapon.load();
            }
            mounts[i] = weapon.mountType.get(weapon);
            ModularPart mpart = weapons.getMap(i).get("part");
            weaponrange = Math.max(weaponrange, weapon.bullet.range + Mathf.dst(mpart.cx, mpart.cy) * ModularPartType.partSize);
        }

        int abilityslots = statmap.abilityslots;
        int abilityslotsused = statmap.abilityslotuse;

        if(abilityslotsused<=abilityslots){
            var abilitiesStats = statmap.stats.getList("abilities");
            Ability[] resultAbilities = new Ability[abilitiesStats.length()];
            for(int i = 0; i < abilitiesStats.length(); i++){
                var abilityStat = abilitiesStats.getMap(i);
                Ability ability = abilityStat.get("ability");

                resultAbilities[i] = ability.copy();
            }
            abilities(resultAbilities);
        }

        this.massStat = statmap.mass*8f;

        speed = eff * Mathf.pow(Mathf.clamp(statmap.weightCapacity * 8f / this.massStat, 0, 1),3) * statmap.speed;
        rotateSpeed = Mathf.clamp(statmap.turningspeed / (float)Math.max(construct.parts.length, construct.parts[0].length), 0, 5);

        armor = statmap.armour;
        itemcap = (int)statmap.itemcapacity;

        differentialSteer = statmap.differentialSteering;
    }

    @Replace
    public void setType(UnitType type) {
        this.type = type;
        if (controller == null) controller(type.createController(self())); //for now
        if(type!=UnityUnitTypes.modularUnitSmall){
            this.maxHealth = type.health;
            drag(type.drag);
            this.armor = type.armor;
            hitSize(type.hitSize);
            hovering(type.hovering);
            if(controller == null) controller(type.createController(self()));
            if(mounts().length != type.weapons.size) setupWeapons(type);
            if(abilities().length != type.abilities.size){
                abilities(type.abilities.map(Ability::copy).toArray(Ability.class));
            }
        }
    }

    @Replace
    public void setupWeapons(UnitType def) {
        if(def!=UnityUnitTypes.modularUnitSmall){
            mounts = new WeaponMount[def.weapons.size];
            for(int i = 0; i < mounts.length; i++){
                mounts[i] = def.weapons.get(i).mountType.get(def.weapons.get(i));
            }
        }
    }

    public void initWeapon(Weapon w){
        if(w.recoilTime < 0) w.recoilTime = w.reload;
    }

    public Weapon getWeaponFromStat(ValueMap weaponstat){
        Weapon weapon = weaponstat.get("weapon");
        initWeapon(weapon);
        return weapon;
    }


    ///replaces ==================================================================================================================================


    @Override
    public void update(){
        if(construct != null && constructdata == null){
            Log.info("uh constructdata died");
            var compdata = construct.getCompressedData();
            constructdata = Arrays.copyOf(compdata, compdata.length);
        }
        if(partTransientProp == null){
            Log.info("partTransientProp was null fsr");
            partTransientProp = new float[construct.partlist.size];
        }
        if(construct != null){
            construct.hasCustomUpdate.each(part -> {
                part.type.update(this, part);
            });
        }
        if(construct != null && elevation<0.01){
            steerAngle *= 0.98;
        }
        moving = false;
        if(maxHealth!=statHp){
            maxHealth=statHp;
        }
    }

    @Replace
    public int itemCapacity(){
        return itemcap;
    }

    @Replace
    public boolean hasWeapons(){
        return mounts.length > 0;
    }

    @Replace
    public float range(){
        return weaponrange;
    }

    @Replace(value = 5)
    public float clipSize(){
        if(isBuilding()){
            return state.rules.infiniteResources ? Float.MAX_VALUE : Math.max(clipsize, type.region.width) + buildingRange + tilesize * 4.0F;
        }
        if(mining()){
            return clipsize + type.mineRange;
        }
        return clipsize;
    }

    @Replace
    public float speed(){
        float strafePenalty = isGrounded() || !isPlayer() ? 1.0F : Mathf.lerp(1.0F, type.strafePenalty, Angles.angleDist(vel().angle(), rotation) / 180.0F);
        float boost = Mathf.lerp(1.0F, type.canBoost ? type.boostMultiplier : 1.0F, elevation);
        return speed * strafePenalty * boost * floorSpeedMultiplier();
    }

    @Replace
    public float mass(){
        return massStat == 0 ? type.hitSize * type.hitSize : massStat;
    }

    transient boolean moving = false;
    public transient float rotateVel = 0;
    public transient float steerAngle = 0;

    private float steerAngle(float from, float to){
        from = Mathf.mod(from, 360.0F);
        to = Mathf.mod(to, 360.0F);
        float b = Angles.backwardDistance(from, to);
        float f = Angles.forwardDistance(from, to);
        if (from > to == b > f) {
            return - b;
        }else{
            return f;
        }
    }

    @Replace
    public void rotateMove(Vec2 vec){
        float vecangle = vec.angle();
        boolean isZero = vec.isZero();

        if(differentialSteer && Math.abs(Angles.angleDist(rotation,vecangle)) > rotateSpeed * 16 && !isZero){
            moveAt(Tmp.v2.trns(rotation, 0));
            moving = vec.len2() > 0.1;
            rotateVel = Angles.moveToward(rotation, vecangle, 16 * speed * Math.max(Time.delta, 1) / hitSize()) - rotation;
            rotation += rotateVel;
        }else{
            moveAt(Tmp.v2.trns(rotation, vec.len()));
            moving = vec.len2() > 0.1;

            if(!vec.isZero()){
                rotateVel = Angles.moveToward(rotation, vecangle, 4 * rotateSpeed * Math.max(Time.delta, 1) * (vec.len() + 0.1f)) - rotation;
                rotation += rotateVel;
            }
        }
        if(isZero){
            rotateVel*=0.8;
            steerAngle*=0.8;
        }else{
            steerAngle =  steerAngle(rotation,vecangle);
        }
    }

    @Replace
    public void lookAt(float angle){
        rotation = Angles.moveToward(rotation, angle, rotateSpeed * Time.delta * speedMultiplier());
    }
    transient float savedHp = -1 ;
    @Override
    public void read(Reads read){
        savedHp = health;
    }

    @Replace
    public void updateDrowning() {
       Floor floor = drownFloor();
       if (floor != null && floor.isLiquid && floor.drownTime > 0) {
           lastDrownFloor = floor;
           drownTime += Time.delta / floor.drownTime / type.drownTimeMultiplier / (hitSize() / 8f);
           if (Mathf.chanceDelta(0.05F)) {
               floor.drownUpdateEffect.at(x(), y(), hitSize(), floor.mapColor);
           }
           if (drownTime >= 0.999F && !net.client()) {
               kill();
               Events.fire(new UnitDrownEvent(self()));
           }
       } else {
           drownTime -= Time.delta / 50.0F;
       }
       drownTime = Mathf.clamp(drownTime);
    }

}
