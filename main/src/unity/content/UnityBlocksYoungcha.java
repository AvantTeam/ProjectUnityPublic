package unity.content;

import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.world.blocks.*;
import unity.world.blocks.distribution.*;
import unity.world.blocks.envrionment.*;
import unity.world.blocks.power.*;
import unity.world.blocks.production.*;
import unity.world.graph.*;

import static mindustry.type.ItemStack.with;

//frankly i do not wish to have my ide lag from an enormous unavigatable UnityBlocks file
public class UnityBlocksYoungcha{
    ///environmental
    public static @FactionDef("youngcha") Block
    oreNickel, concreteBlank, concreteFill, concreteNumber, concreteStripe, concrete, stoneFullTiles, stoneFull,
    stoneHalf, stoneTiles;
    //non environmental
    public static @FactionDef("youngcha") Block
    //transmission
    driveShaft,shaftRouter,smallTransmission,
    //power
    windTurbine, rotaryWaterExtractor,
    //production
    augerDrill;

    public static void load(){
        oreNickel = new UnityOreBlock(UnityItems.nickel){{
           oreScale = 24.77f;
           oreThreshold = 0.913f;
           oreDefault = false;
       }};

        concreteBlank = new Floor("concrete-blank");
        concreteFill = new Floor("concrete-fill"){{ variants = 0; }};
        concreteNumber = new Floor("concrete-number"){{ variants = 10; }};
        concreteStripe = new Floor("concrete-stripe");
        concrete = new Floor("concrete");
        stoneFullTiles = new Floor("stone-full-tiles");
        stoneFull = new Floor("stone-full");
        stoneHalf = new Floor("stone-half");
        stoneTiles = new Floor("stone-tiles");


        driveShaft = new DriveShaft("drive-shaft"){{
            health = 200;

            config.nodeConfig.put(TorqueGraph.class, b->new GraphTorqueNode(0.01f,3f,b));
            config.fixedConnection(TorqueGraph.class, 1, 0, 1 ,0);
            requirements(Category.distribution, with(Items.copper, 10, Items.lead, 10));
        }};
        shaftRouter = new GenericGraphBlock("shaft-router"){{
            requirements(Category.distribution, with(Items.copper, 20, Items.lead, 20));
            health = 150;

            config.nodeConfig.put(TorqueGraph.class, b->new GraphTorqueNode(0.05f,4f,b));
            config.fixedConnection(TorqueGraph.class,1, 1, 1 ,1);
        }};
        windTurbine = new WindTurbine("wind-turbine"){{
            health = 1750;
            size=3;

            config.nodeConfig.put(TorqueGraph.class, b->new WindTurbineTorqueNode(0.03f,20f,1f,20f,b));
            config.fixedConnection(TorqueGraph.class, 0, 1, 0,   0, 0, 0,   0, 0, 0,  0, 0, 0);

            requirements(Category.power, with(Items.titanium, 20, Items.lead, 80, Items.copper, 70));
        }};
        rotaryWaterExtractor = new RotaryWaterExtractor("rotary-water-extractor"){{
            health = 1750;
            size = 3;
            result = Liquids.water;
            pumpAmount = 0.5f;
            liquidCapacity = 60f;
            rotateSpeed = 1.4f;
            attribute = Attribute.water;
            maxSpeed = 40;

            config.nodeConfig.put(TorqueGraph.class, b->new GraphTorqueNode(0.15f,30f,b));
            config.fixedConnection(TorqueGraph.class, 0, 0, 0,   0, 1, 0,   0, 0, 0,  0, 1, 0);
            requirements(Category.production, with(Items.titanium, 50, UnityItems.nickel, 80, Items.metaglass, 30));

        }};
        augerDrill = new TorqueDrill("auger-drill"){{
            health = 1750;
            size = 3;
            tier = 3;
            drillTime = 400;
            requirements(Category.production, with( Items.lead, 60, Items.copper, 150));
            consumes.liquid(Liquids.water, 0.08f).boost();

            config.nodeConfig.put(TorqueGraph.class, b->new GraphTorqueNode(0.1f,50f,b));
            config.fixedConnection(TorqueGraph.class, 0, 1, 0,   0, 0, 0,   0, 1, 0,  0, 0, 0);
        }};
        smallTransmission  = new SimpleTransmission("small-transmission"){{
            requirements(Category.distribution, with(UnityItems.nickel, 20, Items.copper, 20, Items.lead, 20));
            health = 700;
            size = 2;
            config.nodeConfig.put(TorqueGraph.class, b->new TransmissionTorqueNode(0.05f,8f,b));
            config.fixedConnection(TorqueGraph.class,0,1,  0,0,  1,0 , 0,0);
            config.fixedConnection(TorqueGraph.class,1,0,  0,0,  0,1 , 0,0);
        }};



    }
}
