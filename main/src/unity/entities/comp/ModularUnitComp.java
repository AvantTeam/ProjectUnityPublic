package unity.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ai.formations.*;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import unity.annotations.Annotations.*;
import unity.content.*;
import unity.content.effects.*;
import unity.gen.*;
import unity.parts.*;
import unity.parts.PanelDoodadType.*;
import unity.parts.types.*;
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
    float health, maxHealth, rotation, armor;
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
    public transient Formation formation;

    transient ModularConstruct construct;
    transient boolean constructLoaded = false;
    public transient Seq<PanelDoodad> doodadlist = new Seq<>();
    public byte[] constructdata;

    //visuals
    public transient float driveDist = 0;
    public transient float clipsize = 0;
    //stat
    public transient float enginepower = 0;
    public transient float speed = 0;
    public transient float rotateSpeed = 0;
    public transient float massStat = 0;
    public transient float weaponrange = 0;

    @Override
    public void add(){
        if(ModularConstruct.cache.containsKey(this)){
            construct = new ModularConstruct(ModularConstruct.cache.get(this));
        }else{
            if(constructdata != null){
                construct = new ModularConstruct(constructdata);
            }else{
                construct = ModularConstruct.test;
            }
        }
        constructdata = Arrays.copyOf(construct.data, construct.data.length);

        var statmap = new ModularUnitStatMap();
        ModularConstructBuilder.getStats(construct.parts, statmap);
        applyStatMap(statmap);
        if(construct != ModularConstruct.test){
            constructLoaded = true;
            if(!headless){
                initDoodads();
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

    public void initDoodads(){
        /// :I welp i tried
        if(construct != null){
            if(construct.parts.length == 0){
                return;
            }
            boolean[][] filled = new boolean[construct.parts.length][construct.parts[0].length];


            int w = construct.parts.length;
            int h = construct.parts[0].length;
            int miny = 999, maxy = 0;
            for(int i = 0; i < w; i++){
                for(int j = 0; j < h; j++){
                    filled[i][j] = construct.parts[i][j] != null && !construct.parts[i][j].type.open;
                    if(filled[i][j]){
                        miny = Math.min(j, miny);
                        maxy = Math.max(j, maxy);
                    }
                }
            }

            float[][] lightness = new float[w][h];
            int tiles = 0;
            for(int i = 0; i < w; i++){
                for(int j = 0; j < h; j++){
                    lightness[i][j] = Mathf.clamp((0.5f - (1f - Mathf.map(j, miny, maxy, 0, 1))) * 2 + 1, 0, 1);
                    tiles += filled[i][j] ? 1 : 0;
                }
            }
            if(tiles == 0){
                return;
            }
            Seq<Point2> seeds = new Seq();
            int[][] seedspace = new int[w][h];
            int[][] seedspacebuf = new int[w][h];
            for(int i = 0; i < Math.max(Mathf.floor(Mathf.sqrt(tiles) / 2), 1); i++){
                int cnx = Mathf.random(0, Math.round(w / 2f) - 1);
                int cny = Mathf.random(0, h - 1);
                while(!filled[cnx][cny]){
                    cnx = Mathf.random(0, Math.round(w / 2f) - 1);
                    cny = Mathf.random(0, h - 1);
                }
                seeds.add(new Point2(cnx, cny));
                seedspace[cnx][cny] = seeds.size;
                if(filled[w - cnx - 1][cny]){
                    seedspace[w - cnx - 1][cny] = seeds.size;
                }
            }
            boolean hasEmpty = true;
            while(hasEmpty){
                hasEmpty = false;
                for(int i = 0; i < w; i++){
                    for(int j = 0; j < h; j++){
                        if(seedspace[i][j] != 0){
                            int seed = seedspace[i][j];
                            seedspacebuf[i][j] = seed;
                            if(i > 0 && seedspace[i - 1][j] == 0 && seedspacebuf[i - 1][j] < seed){
                                seedspacebuf[i - 1][j] = seed;
                            }
                            if(i < w - 1 && seedspace[i + 1][j] == 0 && seedspacebuf[i + 1][j] < seed){
                                seedspacebuf[i + 1][j] = seed;
                            }
                            if(j > 0 && seedspace[i][j - 1] == 0 && seedspacebuf[i][j - 1] < seed){
                                seedspacebuf[i][j - 1] = seed;
                            }
                            if(j < h - 1 && seedspace[i][j + 1] == 0 && seedspacebuf[i][j + 1] < seed){
                                seedspacebuf[i][j + 1] = seed;
                            }
                        }else{
                            hasEmpty = true;
                        }
                    }
                }
                for(int i = 0; i < w; i++){
                    for(int j = 0; j < h; j++){
                        seedspace[i][j] = seedspacebuf[i][j];
                    }
                }
            }
            for(int i = 0; i < Math.round(w / 2f) - 1; i++){
                for(int j = 0; j < h; j++){
                    float val = Mathf.map((i * 34.343f + j * 844.638f) % 1f, -0.1f, 0.1f);
                    lightness[i][j] += val;
                    lightness[w - i - 1][j] += val;
                }
            }
            for(int i = 0; i < w; i++){
                for(int j = 0; j < h; j++){

                    if(j > 0 && seedspace[i][j] != seedspace[i][j - 1]){
                        lightness[i][j] -= 0.5;
                    }
                    if(i == Math.round(w / 2f) - 1){
                        continue;
                    }
                    lightness[i][j] = Mathf.clamp(lightness[i][j], 0, 1);
                }
            }

            ///finally apply doodads
            boolean[][] placed = new boolean[construct.parts.length][construct.parts[0].length];
            float ox = -w * 0.5f;
            float oy = -h * 0.5f;
            int middlex = Math.round(w / 2f) - 1;
            Seq<PanelDoodadType> draw = new Seq<>();
            PanelDoodadType mirrored = null;
            for(int i = 0; i < Math.round(w / 2f); i++){
                for(int j = 0; j < h; j++){
                    mirrored = null;
                    if(filled[i][j] && !placed[i][j]){
                        draw.clear();
                        draw.add(getType(UnityParts.unitdoodads1x1, lightness[i][j]));
                        var x2 = getType(UnityParts.unitdoodads2x2, lightness[i][j]);
                        if(x2.canFit(construct.parts, i, j) && i + 1 < middlex){
                            draw.add(x2);
                        }

                        PanelDoodadType doodad = draw.random();
                        mirrored = doodad;

                        addDoodad(placed, get(doodad, i + ox, j + oy), i, j);
                    }
                    if(filled[w - i - 1][j] && !placed[w - i - 1][j]){
                        if(mirrored != null){
                            addDoodad(placed, get(mirrored, w - i - mirrored.w + ox, j + oy), w - i - mirrored.w, j);
                            continue;
                        }
                        draw.clear();
                        draw.add(getType(UnityParts.unitdoodads1x1, lightness[w - i - 1][j]));
                        PanelDoodadType doodad = draw.random();
                        addDoodad(placed, get(doodad, w - i - doodad.w + ox, j + oy), w - i - doodad.w, j);
                    }
                }
            }
        }
    }

    public void addDoodad(boolean[][] placed, PanelDoodad p, int x, int y){
        doodadlist.add(p);
        for(int i = 0; i < p.type.w; i++){
            for(int j = 0; j < p.type.h; j++){
                placed[x + i][y + j] = true;
            }
        }
    }

    public PanelDoodad get(PanelDoodadType type, float x, float y){
        return type.create((type.w * 0.5f + x) * ModularPartType.partSize, (type.h * 0.5f + y) * ModularPartType.partSize, x > 0);
    }

    public PanelDoodadType getType(Seq<PanelDoodadType> palette, float lightness){
        lightness = 1 - lightness;
        var t = palette.get(Mathf.clamp((int)(lightness * palette.size), 0, palette.size - 1));
        return t;
    }

    public void applyStatMap(ModularUnitStatMap statmap){
        if(construct.parts.length == 0){
            return;
        }
        float power = statmap.getOrCreate("power").getFloat("value");
        float poweruse = statmap.getOrCreate("powerusage").getFloat("value");
        float eff = Mathf.clamp(power / poweruse, 0, 1);

        float hratio = Mathf.clamp(this.health / this.maxHealth);
        this.maxHealth = statmap.getOrCreate("health").getFloat("value");
        if(savedHp<=0){
            this.health = hratio * this.maxHealth;
        }else{
            this.health = savedHp;
            savedHp = -1;
        }
        var weapons = statmap.stats.getList("weapons");
        mounts = new WeaponMount[weapons.length()];
        weaponrange = 0;

        int weaponslots = Math.round(statmap.getValue("weaponslots"));
        int weaponslotsused = Math.round(statmap.getValue("weaponslotuse"));
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
            weaponrange = Math.max(weaponrange, weapon.bullet.range() + Mathf.dst(mpart.cx, mpart.cy) * ModularPartType.partSize);
        }
        this.massStat = statmap.getOrCreate("mass").getFloat("value");


        float wheelspd = getFloat(statmap.getOrCreate("wheel"), "nominal speed", 0);
        float wheelcap = getFloat(statmap.getOrCreate("wheel"), "weight capacity", 0);
        speed = eff * Mathf.clamp(wheelcap / this.massStat, 0, 1) * wheelspd;
        rotateSpeed = Mathf.clamp(10f * speed / (float)Math.max(construct.parts.length, construct.parts[0].length), 0, 5);

        armor = statmap.getValue("armour", "realValue");

    }

    @Replace
    public void setType(UnitType type) {
        this.type = type;
        if (controller == null) controller(type.createController()); //for now
        if(type!=UnityUnitTypes.modularUnit){
            this.maxHealth = type.health;
            drag(type.drag);
            this.armor = type.armor;
            hitSize(type.hitSize);
            hovering(type.hovering);
            if(controller == null) controller(type.createController());
            if(mounts().length != type.weapons.size) setupWeapons(type);
            if(abilities().size != type.abilities.size){
                abilities(type.abilities.map(Ability::copy));
            }
        }
    }

    @Replace
    public void setupWeapons(UnitType def) {
        if(def!=UnityUnitTypes.modularUnit){
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
            constructdata = Arrays.copyOf(construct.data, construct.data.length);
        }
        float vellen = this.vel().len();
        driveDist += vellen;
        if(construct != null && vellen > 0.01f){
            DrawTransform dt = new DrawTransform(new Vec2(this.x(), this.y()), rotation);
            float dustvel = 0;
            if(moving){
                dustvel = vellen - speed;
            }
            Vec2 nv = vel().cpy().nor().scl(dustvel * 40);//
            Vec2 nvt = new Vec2();
            final Vec2 pos = new Vec2();
            construct.partlist.each(part -> {
                if(!(Mathf.random() > 0.1 || !(part.type instanceof ModularWheelType))){
                    pos.set(part.cx * ModularPartType.partSize, part.ay * ModularPartType.partSize);
                    dt.transform(pos);
                    nvt.set(nv.x + Mathf.range(3), nv.y + Mathf.range(3));
                    Tile t = Vars.world.tileWorld(pos.x, pos.y);
                    if(t != null){
                        OtherFx.dust.at(pos.x, pos.y, 0, t.floor().mapColor, nvt);
                    }

                }
            });

        }
        moving = false;
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
            return clipsize + type.miningRange;
        }
        return clipsize;
    }

    private static transient final Seq<FormationMember> members2 = new Seq<>();

    @Replace
    public void command(Formation formation, Seq<Unit> units){
        clearCommand();
        units.shuffle();
        float spacing = hitSize() * 0.9F;
        minFormationSpeed = speed;
        controlling.addAll(units);
        for(Unit unit : units){
            FormationAI ai;
            unit.controller(ai = new FormationAI(self(), formation));
            spacing = Math.max(spacing, ai.formationSize());
            minFormationSpeed = Math.min(minFormationSpeed, unit instanceof ModularUnitUnit ? ((ModularUnitUnit)unit).speed : unit.type.speed);
        }
        this.formation = formation;
        formation.pattern.spacing = spacing;
        members2.clear();
        for(Unitc u : units){
            members2.add((FormationAI)u.controller());
        }
        formation.addMembers(members2);
    }

    @Replace
    public float speed(){
        float strafePenalty = isGrounded() || !isPlayer() ? 1.0F : Mathf.lerp(1.0F, type.strafePenalty, Angles.angleDist(vel().angle(), rotation) / 180.0F);
        float boost = Mathf.lerp(1.0F, type.canBoost ? type.boostMultiplier : 1.0F, elevation);
        return (isCommanding() ? minFormationSpeed * 0.98F : speed) * strafePenalty * boost * floorSpeedMultiplier();
    }

    @Replace
    public float mass(){
        return massStat == 0 ? type.hitSize * type.hitSize : massStat;
    }

    transient boolean moving = false;

    @Replace
    public void rotateMove(Vec2 vec){
        moveAt(Tmp.v2.trns(rotation, vec.len()));
        moving = vec.len2() > 0.1;
        if(!vec.isZero()){
            rotation = Angles.moveToward(rotation, vec.angle(), rotateSpeed * Math.max(Time.delta, 1));
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
}
