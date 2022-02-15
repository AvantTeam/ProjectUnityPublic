package unity.world.consumers;

import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

public class ConsumeLiquids extends Consume{
    public final LiquidStack[] liquids;

    public ConsumeLiquids(LiquidStack[] liquids){
        this.liquids = liquids;
    }

    @Override
    public void applyLiquidFilter(Bits filter){
        for(LiquidStack stack : liquids) filter.set(stack.liquid.id);
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.liquid;
    }

    @Override
    public void build(Building build, Table table){
        for(LiquidStack stack : liquids){
            table.add(new ReqImage(stack.liquid.uiIcon, () -> build.liquids != null && build.liquids.get(stack.liquid) >= stack.amount)).padRight(8f);
        }
    }

    @Override
    public String getIcon(){
        return "icon-liquid-consume";
    }

    @Override
    public void update(Building build){
        for(LiquidStack stack : liquids){
            build.liquids.remove(stack.liquid, Math.min(use(build, stack.amount), build.liquids.get(stack.liquid)));
        }
    }

    @Override
    public boolean valid(Building build){
        if(build != null && build.liquids != null){
            for(LiquidStack stack : liquids){
                if(build.liquids.get(stack.liquid) < use(build, stack.amount)) return false;
            }
            return true;
        }else return false;
    }

    @Override
    public void display(Stats stats){
        for(LiquidStack stack : liquids){
            stats.add(booster ? Stat.booster : Stat.input, stack.liquid, stack.amount * 60f, false);
        }
    }

    private float use(Building build, float amount){
        return Math.min(amount * build.edelta(), build.block.liquidCapacity);
    }
}
