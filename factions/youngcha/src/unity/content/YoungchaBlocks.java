package unity.content;

import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.mod.*;
import unity.world.blocks.distribution.*;
import unity.world.blocks.power.*;
import unity.world.graph.*;
import unity.world.graph.nodes.*;
import unity.world.graph.connectors.*;

import static mindustry.type.ItemStack.*;
import static unity.mod.FactionRegistry.*;

public final class YoungchaBlocks{
    public static Block
    heatPipe,
    heatSource;

    private YoungchaBlocks(){
        throw new AssertionError();
    }

    public static void load(){
        heatPipe = register(Faction.youngcha, new HeatPipe("heat-pipe"){{
            requirements(Category.power, with(Items.copper, 1)/*with(YoungchaItems.nickel, 5, Items.copper, 10)*/);

            health = 100;
            solid = false;
            envEnabled |= Env.scorching;// | PUEnv.acidic;
            underBullets = true;

            heatNodeConfig = new HeatNodeType(){{
                emissiveness = 0.005f;
                conductivity = 0.4f;
                heatCapacity = 1f;
                maxTemp = HeatNodeType.celsiusZero + 2500f;
            }};

            heatConnectorConfigs.add(new FixedConnectorType<>(HeatGraph::new, 1, 1, 1, 1));
        }});

        heatSource = register(Faction.youngcha, new HeatSource("heat-source"){{
            requirements(Category.power, BuildVisibility.sandboxOnly, with());

            solid = true;
            envEnabled |= Env.scorching;// | PUEnv.acidic;

            heatNodeConfig = new HeatNodeType(){{
                emissiveness = 0f;
                conductivity = 1f;
                heatCapacity = 900f;
                maxTemp = 5000f;
            }};

            heatConnectorConfigs.add(new FixedConnectorType<>(HeatGraph::new, 1, 1, 1, 1));
        }});
    }
}
