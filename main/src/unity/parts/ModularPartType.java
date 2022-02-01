package unity.parts;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import unity.parts.stat.*;
import unity.util.*;

//like Block, this is a singleton
public class ModularPartType{
    public static IntMap<ModularPartType> partMap = new IntMap<>();

    public static final float partSize = 8;

    public static final int TURRET_TYPE = 1;
    public static final int UNIT_TYPE = 2;
    protected int partType = 0;

    private static int idAcc = 0;
    public final int id = idAcc++;

    public String name;
    public int w=1,h=1;

    //graphics
    public TextureRegion icon;
    /**if true will not have paneling**/
    public boolean open = false;
    /** texture will/may have three variants for the front middle and back **/
    public TextureRegion[] top;
    public TextureRegion[] shadow;

    //stats
    protected Seq<ModularPartStat> stats = new Seq<>();

    //places it can connect to
    public boolean root = false;


    public ModularPart create(int x, int y){
        return new ModularPart(this,x,y);
    }


    public ModularPartType(String name){
        this.name = name;
        partMap.put(id,this);
    }

    public void load(){
        ///
        icon = Core.atlas.find("router");
    }

    public boolean canBeUsedIn(int type){
        return (type & partType) > 0;
    }

    public void draw(DrawTransform transform, ModularPart part){
        //something

        transform.drawRect(icon,part.ax*partSize,part.ay*partSize);
    }

    public static ModularPartType getPartFromId(int id){
        if(partMap.containsKey(id)){
            return partMap.get(id);
        }else{
            Log.info("Part of id "+ id+" not found");
            return partMap.get(0);
        }
    }


    //stats.
    public void appendStats(ModularPartStatMap statmap, ModularPart part, ModularPart[][] grid){
        for(var stat:stats){
            stat.merge(statmap,part);
        }
    }
    public void appendStatsPost(ModularPartStatMap statmap, ModularPart part, ModularPart[][] grid){
        for(var stat:stats){
            stat.mergePost(statmap,part);
        }
    }

    public void health(float amount){
        stats.add(new HealthStat(amount));
    }
    public void healthMul(float amount){
        stats.add(new HealthStat(amount));
    }


}


