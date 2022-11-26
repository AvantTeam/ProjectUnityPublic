package unity.content;

import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.mod.*;
import unity.world.blocks.environment.*;

import static unity.content.MonolithAttributes.*;
import static unity.mod.FactionRegistry.*;

/**
 * Defines all {@linkplain Faction#monolith monolith} block types.
 * @author GlennFolker
 */
//TODO env{ludellyte-wall}, ore{proximite}
public final class MonolithBlocks{
    public static Block
    liquefiedEneraphyte,

    ludellyte,
    erodedSlate, infusedErodedSlate, archaicErodedSlate, sharpSlate, infusedSharpSlate, archaicSharpSlate,
    erodedEneraphyteVent, eneraphyteVent,

    oreEneraphyteCrystal,

    erodedSlateWall, infusedErodedSlateWall, archaicErodedSlateWall, sharpSlateWall, infusedSharpSlateWall, archaicSharpSlateWall;

    private MonolithBlocks(){
        throw new AssertionError();
    }

    public static void load(){
        liquefiedEneraphyte = register(Faction.monolith, new Floor("liquefied-eneraphyte"){{
            speedMultiplier = 0.5f;
            variants = 0;
            status = MonolithStatusEffects.eneraphyteSupercharge;
            statusDuration = 60f;
            liquidDrop = MonolithFluids.eneraphyte;
            isLiquid = true;
            drownTime = 360f;
            cacheLayer = PUCacheLayer.eneraphyte;
            albedo = 0.9f;
            supportsOverlay = true;
        }});

        ludellyte = register(Faction.monolith, new Floor("ludellyte"){{
            attributes.set(Attribute.water, -0.1f);
        }});

        erodedSlate = register(Faction.monolith, new Floor("eroded-slate"){{
            attributes.set(Attribute.water, -0.3f);
            attributes.set(eneraphyteInfusion, 0.08f);
        }});

        infusedErodedSlate = register(Faction.monolith, new Floor("infused-eroded-slate"){{
            attributes.set(Attribute.water, -0.3f);
            attributes.set(eneraphyteInfusion, 0.24f);
        }});

        archaicErodedSlate = register(Faction.monolith, new Floor("archaic-eroded-slate"){{
            attributes.set(Attribute.water, -0.3f);
            attributes.set(eneraphyteInfusion, 0.65f);
        }});

        sharpSlate = register(Faction.monolith, new Floor("sharp-slate"){{
            attributes.set(Attribute.water, -0.2f);
            attributes.set(eneraphyteInfusion, 0.15f);
        }});

        infusedSharpSlate = register(Faction.monolith, new Floor("infused-sharp-slate"){{
            attributes.set(Attribute.water, -0.2f);
            attributes.set(eneraphyteInfusion, 0.4f);
        }});

        archaicSharpSlate = register(Faction.monolith, new Floor("archaic-sharp-slate"){{
            attributes.set(Attribute.water, -0.2f);
            attributes.set(eneraphyteInfusion, 1f);
        }});

        erodedEneraphyteVent = register(Faction.monolith, new SizedVent("eroded-eneraphyte-vent"){{
            attributes.set(Attribute.water, -0.3f);
            attributes.set(eneraphyteEmission, 0.75f);
            attributes.set(eneraphyteInfusion, 0.24f);

            size = 1;
            parent = blendGroup = infusedErodedSlate;
            effect = MonolithFx.erodedEneraphyteSteam;
            effectSpacing = 20f;
        }});

        eneraphyteVent = register(Faction.monolith, new SizedVent("eneraphyte-vent"){{
            attributes.set(Attribute.water, -0.2f);
            attributes.set(eneraphyteEmission, 1f);
            attributes.set(eneraphyteInfusion, 0.4f);

            size = 1;
            parent = blendGroup = infusedSharpSlate;
            effect = MonolithFx.eneraphyteSteam;
            effectSpacing = 20f;
        }});

        oreEneraphyteCrystal = register(Faction.monolith, new OreBlock("ore-eneraphyte-crystal", MonolithItems.eneraphyteCrystal));

        erodedSlateWall = register(Faction.monolith, new StaticWall("eroded-slate-wall"){{
            attributes.set(eneraphyteInfusion, 0.08f);
            erodedSlate.asFloor().wall = this;
        }});

        infusedErodedSlateWall = register(Faction.monolith, new StaticWall("infused-eroded-slate-wall"){{
            attributes.set(eneraphyteInfusion, 0.24f);
            infusedErodedSlate.asFloor().wall = this;
        }});

        archaicErodedSlateWall = register(Faction.monolith, new StaticWall("archaic-eroded-slate-wall"){{
            attributes.set(eneraphyteInfusion, 0.65f);
            archaicErodedSlate.asFloor().wall = this;
        }});

        sharpSlateWall = register(Faction.monolith, new StaticWall("sharp-slate-wall"){{
            attributes.set(eneraphyteInfusion, 0.15f);
            sharpSlate.asFloor().wall = this;
        }});

        infusedSharpSlateWall = register(Faction.monolith, new StaticWall("infused-sharp-slate-wall"){{
            attributes.set(eneraphyteInfusion, 0.4f);
            infusedSharpSlate.asFloor().wall = this;
        }});

        archaicSharpSlateWall = register(Faction.monolith, new StaticWall("archaic-sharp-slate-wall"){{
            attributes.set(eneraphyteInfusion, 1f);
            archaicSharpSlate.asFloor().wall = this;
        }});
    }
}
