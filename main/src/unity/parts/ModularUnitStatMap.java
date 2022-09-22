package unity.parts;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ui.*;
import unity.ui.*;
import unity.ui.PartsEditorElement.*;
import unity.util.*;

public class ModularUnitStatMap extends ModularPartStatMap{
    public float health,armour,armourPoints;
    public float power,powerUsage,powerRpsMul,rps;
    public float mass,speed,turningspeed,weightCapacity, speedPower, turningPower, tractionTotal;
    public int itemcapacity,abilityslotuse,abilityslots, weaponSlots,weaponslotuse;
    public boolean differentialSteering;

    public ModularUnitStatMap(){
        stats.put("weapons",new ValueList());
        stats.put("abilities",new ValueList());
        stats.put("wheel",(new ValueMap()));
    }

    private static final ObjectSet<Class<? extends ModularPartStat>> statPost = new ObjectSet<>();
    private static final Seq<PartStatPair> statPairs = new Seq<>();
    private static int pairsStored = 0;

    private void setPair( ModularPart part, ModularPartStat stat, boolean post){
        if(statPairs.size<=pairsStored){
            statPairs.add(new PartStatPair(part, stat, post, this));
            pairsStored++;
            return;
        }
        statPairs.get(pairsStored).set(part,stat,post, this);
        pairsStored++;
    }

    @Override
    public void calculateStat(Seq<ModularPart> partseq){
        Log.info("begining stat calc, "+ partseq.size+" parts found");
        pairsStored = 0;
        statPost.clear();
        for(int p_i = 0; p_i < partseq.size; p_i++){
            var stats = partseq.get(p_i).type.stats;
            for(int s_i = 0; s_i < stats.size; s_i++){
                setPair(partseq.get(p_i),stats.get(s_i),false);
                if(!statPost.contains(stats.get(s_i).getClass())){
                    setPair(partseq.get(p_i),stats.get(s_i),true);
                    statPost.add(stats.get(s_i).getClass());
                }
            }
        }
        int osize = statPairs.size;
        statPairs.size = pairsStored;
        statPairs.sort(p -> p.post? p.stat.mergePostPriority * 2 + 1 : p.stat.mergePriority * 2);
        //Log.info(statPairs.toString());
        statPairs.size = osize;
        //Log.info("stat pairs:"+pairsStored);
        for(int i = 0; i < pairsStored; i++){
            statPairs.get(i).merge();
        }
    }

    @Override
    public void drawEditor(PartsEditorElement editor){
        float ex = editor.x;
        float ey = editor.y;
        Draw.color();
        editor.text(Fonts.outline, Core.bundle.format("ui.parts.editor.shaftspeed",Strings.fixed(rps,2)), ex + 48,ey + 32, EditorTextAlign.LEFT);
        Fill.rect(ex + 24,ey + 32,12,12,Time.time * rps * 6);
    }


    @Override
    public String toString(){
        return "ModularUnitStatMap{" +
        "health=" + health +
        ", mass=" + mass +
        ", power=" + power +
        ", powerUsage=" + powerUsage +
        ", armour=" + armour +
        ", armourPoints=" + armourPoints +
        ", rps=" + rps +
        ", speed=" + speed +
        ", turningspeed=" + turningspeed +
        ", itemcapacity=" + itemcapacity +
        ", abilityslotuse=" + abilityslotuse +
        ", abilityslots=" + abilityslots +
        ", weaponSlots=" + weaponSlots +
        ", weaponslotuse=" + weaponslotuse +
        ", differentialSteering=" + differentialSteering +
        '}';
    }


    public static class PartStatPair{
        ModularPart part;
        ModularPartStat stat;
        boolean post;
        ModularUnitStatMap map;

        public PartStatPair(ModularPart part, ModularPartStat stat, boolean post,  ModularUnitStatMap map){
            this.part = part;
            this.stat = stat;
            this.post = post;
            this.map = map;
        }

        public void set(ModularPart part, ModularPartStat stat, boolean post,  ModularUnitStatMap map){
            this.part = part;
            this.stat = stat;
            this.post = post;
            this.map = map;
        }

        void merge(){
            if(post){
                stat.mergePost(map,part);
            }else{
                stat.merge(map,part);
            }
        }

        @Override
        public String toString(){
            return "PartStatPair{" +
            "part=" + part.type.name +
            ", stat=" + stat.name +
            ", post=" + post +
            '}';
        }
    }
}
