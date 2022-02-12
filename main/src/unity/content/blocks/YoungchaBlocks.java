package unity.content.blocks;

import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.content.*;
import unity.world.blocks.*;
import unity.world.blocks.distribution.*;
import unity.world.blocks.envrionment.*;
import unity.world.blocks.power.*;
import unity.world.blocks.production.*;
import unity.world.blocks.units.*;
import unity.world.graph.*;

import static mindustry.type.ItemStack.with;

//frankly i do not wish to have my ide lag from an enormous unnavigable UnityBlocks file
public class YoungchaBlocks{
    ///environmental
    public static @FactionDef("youngcha")
    Block
    oreNickel, concreteBlank, concreteFill, concreteNumber, concreteStripe, concrete, stoneFullTiles, stoneFull,
    stoneHalf, stoneTiles;
    //non environmental
    public static @FactionDef("youngcha")
    Block
    //torque
        //transmission
        driveShaft, shaftRouter, smallTransmission, torqueMeter,
        //power
        windTurbine, rotaryWaterExtractor, flywheel,
        //production
        augerDrill,
    ///heat
        //transmission
        heatPipe, steamPiston,
        //power
        combustionHeater,thermalHeater,seebeckGenerator,smallRadiator,
    //crucible
        //crafting
        crucible,crucibleChannel,
    //other
    sandboxAssembler; // monomial, binomial then polynomial (maybe meromorphic for the t6-t7 equiv massive unit)

    public static void load(){
        oreNickel = new UnityOreBlock(UnityItems.nickel){{
            oreScale = 24.77f;
            oreThreshold = 0.913f;
            oreDefault = false;
        }};

        concreteBlank = new Floor("concrete-blank");
        concreteFill = new Floor("concrete-fill"){{
            variants = 0;
        }};
        concreteNumber = new Floor("concrete-number"){{
            variants = 10;
        }};
        concreteStripe = new Floor("concrete-stripe");
        concrete = new Floor("concrete");
        stoneFullTiles = new Floor("stone-full-tiles");
        stoneFull = new Floor("stone-full");
        stoneHalf = new Floor("stone-half");
        stoneTiles = new Floor("stone-tiles");


        driveShaft = new DriveShaft("drive-shaft"){{
            health = 200;

            config.nodeConfig.put(TorqueGraph.class, b -> new TorqueGraphNode(0.005f, 3f, b));
            config.fixedConnection(TorqueGraph.class, 1, 0, 1, 0);
            requirements(Category.distribution, with(Items.copper, 10, Items.lead, 10));
        }};
        shaftRouter = new GenericGraphBlock("shaft-router"){{
            requirements(Category.distribution, with(Items.copper, 20, Items.lead, 20));
            health = 150;
            solid = true;

            config.nodeConfig.put(TorqueGraph.class, b -> new TorqueGraphNode(0.04f, 4f, b));
            config.fixedConnection(TorqueGraph.class, 1, 1, 1, 1);
        }};
        smallTransmission = new SimpleTransmission("small-transmission"){{
            requirements(Category.distribution, with(UnityItems.nickel, 20, Items.copper, 20, Items.lead, 20));
            health = 700;
            size = 2;
            config.nodeConfig.put(TorqueGraph.class, b -> new TransmissionTorqueGraphNode(0.05f, 8f, b));
            config.fixedConnection(TorqueGraph.class, 0, 1, 0, 0, 1, 0, 0, 0);
            config.fixedConnection(TorqueGraph.class, 1, 0, 0, 0, 0, 1, 0, 0);
        }};
        torqueMeter = new TorqueMeter("torque-meter"){{
            requirements(Category.distribution, with(UnityItems.nickel, 20, Items.lead, 30));
            health = 150;
            rotate = true;
            solid = true;

            config.nodeConfig.put(TorqueGraph.class, b -> new TorqueMeterGraphNode(0.02f, 5f, b));
            config.fixedConnection(TorqueGraph.class, 1, 0, 1, 0);

        }};
        windTurbine = new WindTurbine("wind-turbine"){{
            requirements(Category.power, with(Items.titanium, 20, Items.lead, 80, Items.copper, 70));
            health = 1750;
            size = 3;

            config.nodeConfig.put(TorqueGraph.class, b -> new WindTurbineTorqueGraphNode(0.03f, 20f, 1f, 20f, b));
            config.fixedConnection(TorqueGraph.class, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
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

            config.nodeConfig.put(TorqueGraph.class, b -> new TorqueGraphNode(0.15f, 30f, b));
            config.fixedConnection(TorqueGraph.class, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0);
            requirements(Category.production, with(Items.titanium, 50, UnityItems.nickel, 80, Items.metaglass, 30));

        }};
        augerDrill = new TorqueDrill("auger-drill"){{
            health = 1750;
            size = 3;
            tier = 3;
            drillTime = 400;
            requirements(Category.production, with(Items.lead, 60, Items.copper, 150));
            consumes.liquid(Liquids.water, 0.08f).boost();

            config.nodeConfig.put(TorqueGraph.class, b -> new TorqueGraphNode(0.1f, 50f, b));
            config.fixedConnection(TorqueGraph.class, 0, 1, 0,  0, 0, 0,  0, 1, 0,  0, 0, 0);
        }};

        heatPipe = new HeatPipe("heat-pipe"){{
            requirements(Category.distribution, with(UnityItems.nickel, 5, Items.copper, 10));
            health = 250;
            solid = false;
            config.nodeConfig.put(HeatGraph.class, b -> new HeatGraphNode(b, 0.005f, 0.4f, 1,2500 + HeatGraphNode.celsiusZero));
            config.fixedConnection(HeatGraph.class, 1, 1, 1, 1);
        }};

        flywheel = new FlyWheel("flywheel"){{
            requirements(Category.power, with(UnityItems.nickel, 50, Items.copper, 50, Items.lead, 150));
            size = 3;
            rotate = true;
            health = 2350;
            solid = true;
            config.nodeConfig.put(TorqueGraph.class, b -> new TorqueGraphNode(0.05f, 1000f, 30f,5,b));
            config.fixedConnection(TorqueGraph.class, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0);
        }};

        steamPiston = new SteamPiston("steam-piston"){{
            requirements(Category.power, with(Items.graphite, 20, UnityItems.nickel, 30, Items.copper, 50, Items.lead, 150));
            size = 3;
            rotate = true;
            health = 1300;
            solid = true;
            consumes.liquid(Liquids.water, 0.1f);
            config.nodeConfig.put(HeatGraph.class, b -> new HeatGraphNode(b, 0.01f, 0.1f, 9, 1200 + HeatGraphNode.celsiusZero));
            config.fixedConnection(HeatGraph.class, 0,0,0, 0,0,0 ,0,1,0 ,0,0,0);
        }};

        combustionHeater = new CombustionHeater("combustion-heater"){{
            requirements(Category.power, with(UnityItems.nickel, 30, Items.lead, 70, Items.copper, 70));
            size = 2;
            rotate = true;
            health = 700;
            solid = true;
            config.nodeConfig.put(HeatGraph.class, b -> new HeatGraphNode(b, 0.01f, 0.1f, 4, 1500 + HeatGraphNode.celsiusZero, 1000 + HeatGraphNode.celsiusZero,0.015f));
            config.fixedConnection(HeatGraph.class, 1, 1,  0, 0,  0, 0,  0, 0);
        }};

        thermalHeater = new ThermalHeater("thermal-heater"){{
            size = 2;
            rotate = true;
            health = 700;
            solid = true;
            config.nodeConfig.put(HeatGraph.class, b -> new HeatGraphNode(b, 0.01f, 0.1f, 1500 + HeatGraphNode.celsiusZero,4, 1000 + HeatGraphNode.celsiusZero,0.03f));
            config.fixedConnection(HeatGraph.class, 1, 1,  0, 0,  0, 0,  0, 0);
            requirements(Category.power, with(UnityItems.nickel, 30, Items.graphite, 30, Items.copper, 100, Items.silicon, 30));
        }};

        seebeckGenerator = new SeebeckGenerator("seebeck-generator"){{
            size = 3;
            rotate = true;
            health = 1700;
            solid = true;
            hasPower = true;
            config.nodeConfig.put(HeatGraph.class, b -> new HeatGraphNode(b, 0.01f, 0.01f,9, 1800 + HeatGraphNode.celsiusZero));
            config.fixedConnection(HeatGraph.class, 0,1,0,  0,0,0,  0,1,0  ,0,0,0);
            requirements(Category.power, with(UnityItems.nickel, 50, Items.graphite, 30, Items.copper, 120,Items.lead, 100, Items.silicon, 30));
        }};
        smallRadiator = new HeatRadiator("small-radiator"){{

            size = 2;
            rotate = true;
            health = 700;
            solid = true;
            config.nodeConfig.put(HeatGraph.class, b -> new HeatGraphNode(b, 0.4f, 0.15f, 4,1800 + HeatGraphNode.celsiusZero));
            config.fixedConnection(HeatGraph.class, 0, 0,  1, 1,  0, 0,  1, 1);
            requirements(Category.power, with(UnityItems.nickel, 30, Items.graphite, 30, Items.copper, 100, Items.silicon, 30));
        }};

        crucible = new CrucibleBlock("crucible"){{
            requirements(Category.crafting, with(UnityItems.nickel, 30, Items.graphite, 30, Items.copper, 50));
            size = 3;
            health = 1700;
            solid = true;

            config.nodeConfig.put(HeatGraph.class, b -> new HeatGraphNode(b, 0.02f, 0.15f, 9,1800 + HeatGraphNode.celsiusZero));
            config.fixedConnection(HeatGraph.class, 1,0,1,  1,0,1,  1,0,1,  1,0,1);
            config.nodeConfig.put(CrucibleGraph.class, b -> new CrucibleGraphNode(b,50));
            config.fixedConnection(CrucibleGraph.class, 0,1,0,  0,1,0,  0,1,0,  0,1,0);
        }};

        crucibleChannel = new CrucibleChannel("crucible-channel"){{
            requirements(Category.crafting, with(UnityItems.nickel, 10, Items.graphite, 10));
            health = 200;
            config.nodeConfig.put(CrucibleGraph.class, b -> new CrucibleGraphNode(b,5));
            config.fixedConnection(CrucibleGraph.class, 1,1,1,1);
        }};


        sandboxAssembler = new ModularUnitAssembler("sandbox-assembler"){{
            requirements(Category.units, BuildVisibility.sandboxOnly, with());
            size = 3;
            health = 1700;
        }};
        //
    }
}
