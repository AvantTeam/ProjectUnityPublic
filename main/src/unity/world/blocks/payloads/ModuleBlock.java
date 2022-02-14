package unity.world.blocks.payloads;

import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class ModuleBlock extends Block{
    public boolean castable = true;
    public ModuleBlock(String name){
        super(name);
        buildVisibility = BuildVisibility.hidden;
        solid = true;
        destructible = true;
        buildCostMultiplier = 30f; //so you actually have to cast it
    }

    public class ModuleBuild extends Building{ }
}
