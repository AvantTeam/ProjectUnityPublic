package unity.world.blocks.production;

import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import unity.annotations.Annotations.*;
import unity.gen.*;
import unity.world.consumers.*;

import static arc.Core.*;

@Merge(base = GenericCrafter.class, value = Stemc.class)
public class LiquidsSmelter extends StemGenericCrafter{
    protected Liquid[] liquids;

    public LiquidsSmelter(String name){
        super(name);
    }

    @Override
    public void init(){
        if(!consumes.has(ConsumeType.liquid) || !(consumes.get(ConsumeType.liquid) instanceof ConsumeLiquids)){
            throw new RuntimeException("LiquidSmelter must have a ConsumeLiquids. Note that filters are not supported.");
        }
        
        LiquidStack[] stacks = ((ConsumeLiquids)consumes.get(ConsumeType.liquid)).liquids;
        liquids = new Liquid[stacks.length];
        for(int i = 0; i < liquids.length; i++) liquids[i] = stacks[i].liquid;
        
        super.init();
    }

    @Override
    public void setBars(){
        super.setBars();
        
        bars.remove("liquid");
        for(Liquid liquid : liquids){
            bars.add(liquid.name, build -> new Bar(() -> build.liquids.get(liquid) <= 0.001f ? bundle.get("bar.liquid") : liquid.localizedName, liquid::barColor, () -> build.liquids.get(liquid) / liquidCapacity));
        }
    }
}
