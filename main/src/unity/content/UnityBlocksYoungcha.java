package unity.content;

import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import unity.annotations.Annotations.*;
import unity.world.blocks.*;
import unity.world.blocks.distribution.*;
import unity.world.blocks.power.*;
import unity.world.blocks.power.WindTurbine.*;
import unity.world.blocks.production.*;
import unity.world.graph.*;

import static mindustry.type.ItemStack.with;

//frankly i do not wish to have my ide lag from an enormous unavigatable UnityBlocks file
public class UnityBlocksYoungcha{
    public static @FactionDef("youngcha") Block
    driveShaft,shaftRouter,
    windTurbine,
    augerDrill;

    public static void load(){
        driveShaft = new DriveShaft("drive-shaft"){{
            health = 150;

            config.nodeConfig.put(TorqueGraph.class, b->new GraphTorqueNode(0.01f,3f,b));
            config.addFixedConnectionConfig(TorqueGraph.class,TorqueGraph::new, 1, 0, 1 ,0);

            requirements(Category.distribution, with(Items.copper, 10, Items.lead, 10));
        }};
        shaftRouter = new GraphBlock("shaft-router"){{
            requirements(Category.distribution, with(Items.copper, 20, Items.lead, 20));
            health = 100;

            config.nodeConfig.put(TorqueGraph.class, b->new GraphTorqueNode(0.05f,4f,b));
            config.addFixedConnectionConfig(TorqueGraph.class,TorqueGraph::new, 1, 1, 1 ,1);
        }};
        windTurbine = new WindTurbine("wind-turbine"){{
            health = 1350;
            size=3;

            config.nodeConfig.put(TorqueGraph.class, b->new WindTurbineTorqueNode(0.03f,20f,b));
            config.addFixedConnectionConfig(TorqueGraph.class,TorqueGraph::new, 0, 1, 0,   0, 0, 0,   0, 0, 0,  0, 0, 0);

            requirements(Category.power, with(Items.titanium, 20, Items.lead, 80, Items.copper, 70));

        }};
        augerDrill = new TorqueDrill("auger-drill"){{
            health = 1350;
            size=3;
            tier = 3;
            drillTime = 400;
            config.nodeConfig.put(TorqueGraph.class, b->new WindTurbineTorqueNode(0.1f,50f,b));
            config.addFixedConnectionConfig(TorqueGraph.class,TorqueGraph::new, 0, 1, 0,   0, 0, 0,   0, 1, 0,  0, 0, 0);

            requirements(Category.production, with( Items.lead, 60, Items.copper, 150));
        }};
    }
}
