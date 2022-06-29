package unity.content.blocks;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.pattern.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.content.*;
import unity.content.effects.*;
import unity.entities.bullet.exp.*;
import unity.gen.*;
import unity.graphics.*;
import unity.world.blocks.defense.*;
import unity.world.blocks.distribution.*;
import unity.world.blocks.exp.*;
import unity.world.blocks.exp.turrets.*;
import unity.world.blocks.production.*;
import unity.world.blocks.units.*;
import unity.world.draw.*;

import static arc.Core.bundle;
import static mindustry.Vars.tilesize;
import static mindustry.type.ItemStack.empty;
import static mindustry.type.ItemStack.with;
import static unity.content.UnityStatusEffects.*;

public class KoruhBlocks {
    public static final Category expCategory = Category.distribution;
    public static @FactionDef("koruh") Block
    //environment
    lava, shallowLava,

    //crafting
    denseSmelter, solidifier, steelSmelter, titaniumExtractor, coalExtractor;

    public static @FactionDef("koruh") Block lavaSmelter, diriumCrucible;

    //defense
    public static @FactionDef("koruh") Block stoneWall, denseWall, steelWall, steelWallLarge, diriumWall, diriumWallLarge, shieldProjector, diriumProjector,

    //distribution
    steelConveyor, teleporter, teleunit;

    public static @FactionDef("koruh")
    @Dupe(base = ExpTurret.class, parent = KoruhConveyor.class)
    Block diriumConveyor;

    //unit
    //public static @Annotations.FactionDef("koruh") Block bufferPad, omegaPad, cachePad, convertPad,

    //power
    //uraniumReactor,

    //exp
    public static @FactionDef("koruh") Block expFountain, expVoid, expTank, expChest, expRouter, expTower, expTowerDiagonal, bufferTower, expHub, expNode, expNodeLarge, skillCenter;// expOutput, expUnloader;

    //turret
    public static @FactionDef("koruh")
    @LoadRegs("bt-laser-turret-top")
    Block laser, laserCharge, laserBranch, laserFractal, laserBreakthrough;

    public static @FactionDef("koruh") Block laserFrost, laserKelvin;

    public static @FactionDef("koruh") Block inferno;

    public static void load(){
        Blocks.stone.itemDrop = UnityItems.stone;
        Blocks.stone.playerUnmineable = true;
        Blocks.craters.itemDrop = UnityItems.stone;
        Blocks.craters.playerUnmineable = true;

        lava = new Floor("lava-deposit"){
            {
                speedMultiplier = 0.1f;
                variants = 0;
                liquidDrop = UnityLiquids.lava;
                liquidMultiplier = 1f;
                isLiquid = true;
                status = UnityStatusEffects.molten;
                statusDuration = 120f;
                drownTime = 30f;
                cacheLayer = UnityShaders.lavaLayer;
                albedo = 0f;
                emitLight = true;
                lightRadius = 18f;
                lightColor = UnityPal.lava.cpy().a(0.7f);
                attributes.set(Attribute.heat, 1.5f);
            }

            @Override
            public TextureRegion[] icons(){
                return new TextureRegion[]{Core.atlas.find(name + "-icon", name)};
            }
        };

        shallowLava = new Floor("lava-shallow"){{
            buildVisibility = BuildVisibility.hidden;
            speedMultiplier = 0.5f;
            variants = 3;
            liquidDrop = UnityLiquids.lava;
            liquidMultiplier = 0.6f;
            isLiquid = true;
            status = UnityStatusEffects.molten;
            statusDuration = 90f;
            drownTime = 100f;
            cacheLayer = UnityShaders.lavaLayer;
            albedo = 0f;
            emitLight = true;
            lightRadius = 18f;
            lightColor = UnityPal.lava.cpy().a(0.4f);
            attributes.set(Attribute.heat, 1f);
        }};

        denseSmelter = new KoruhCrafter("dense-smelter"){{
            requirements(Category.crafting, with(Items.copper, 30, Items.lead, 20, UnityItems.stone, 35));

            health = 70;
            hasItems = true;
            craftTime = 46.2f;
            craftEffect = UnityFx.denseCraft;
            itemCapacity = 10;

            outputItem = new ItemStack(UnityItems.denseAlloy, 1);
            consumeItems(with(Items.copper, 1, Items.lead, 2, Items.coal, 1));

            expUse = 2;
            expCapacity = 24;
            drawer = new DrawExp(){{
                flame = Color.orange;
                glowAmount = 1f;
            }};
        }};

        solidifier = new LiquidsSmelter("solidifier"){{
            requirements(Category.crafting, with(Items.copper, 20, UnityItems.denseAlloy, 30));

            health = 150;
            hasItems = true;
            liquidCapacity = 12f;
            updateEffect = Fx.fuelburn;
            craftEffect = UnityFx.rockFx;
            craftTime = 60f;
            outputItem = new ItemStack(UnityItems.stone, 1);

            consume(new ConsumeLiquids(new LiquidStack[]{new LiquidStack(UnityLiquids.lava, 0.1f), new LiquidStack(Liquids.water, 0.1f)}));

            drawer = new DrawDefault(){
                public TextureRegion top;

                @Override
                public void load(Block block){
                    super.load(block);
                    top = Core.atlas.find(name + "-top");
                }

                @Override
                public void draw(Building build){
                    Draw.rect(build.block.region, build.x, build.y);
                    Draw.color(liquids[0].color, build.liquids.get(liquids[0]) / liquidCapacity);
                    Draw.rect(top, build.x, build.y);
                    Draw.reset();
                }
            };
        }};

        steelSmelter = new GenericCrafter("steel-smelter"){{
            requirements(Category.crafting, with(Items.lead, 45, Items.silicon, 20, UnityItems.denseAlloy, 30));
            health = 140;
            itemCapacity = 10;
            craftEffect = UnityFx.craft;
            updateEffect = Fx.fuelburn;
            craftTime = 300f;
            outputItem = new ItemStack(UnityItems.steel, 1);

            consumePower(2f);
            consumeItems(with(Items.coal, 2, Items.graphite, 2, UnityItems.denseAlloy, 3));

            drawer = new DrawDefault(){
                public TextureRegion top;

                @Override
                public void load(Block block){
                    super.load(block);
                    top = Core.atlas.find(name + "-top");
                }

                @Override
                public void draw(Building build){
                    Draw.rect(build.block.region, build.x, build.y);
                    Draw.color(1f, 1f, 1f, build.warmup() * Mathf.absin(8f, 0.6f));
                    Draw.rect(top, build.x, build.y);
                    Draw.reset();
                }
            };
        }};

        lavaSmelter = new MeltingCrafter("lava-smelter"){{
            requirements(Category.crafting, with(Items.silicon, 70, UnityItems.denseAlloy, 60, UnityItems.steel, 40));

            health = 190;
            hasLiquids = true;
            hasItems = true;
            craftTime = 70f;
            updateEffect = Fx.fuelburn;
            craftEffect = UnityFx.craft;
            itemCapacity = 21;

            outputItem = new ItemStack(UnityItems.steel, 5);
            consumeItems(with(Items.graphite, 7, UnityItems.denseAlloy, 7));
            consumePower(2f);
            consumeLiquid(UnityLiquids.lava, 0.4f);

            expUse = 10;
            expCapacity = 60;
            //TODO this needs to be tweaked to use animated liquid regions -Anuke
            drawer = new DrawMulti(new DrawDefault(), new DrawLiquidRegion(){{
                suffix = "-input-liquid";
            }});
        }};

        /*liquifier = new BurnerSmelter("liquifier"){{
            requirements(Category.crafting, BuildVisibility.hidden, with(Items.titanium, 30, Items.silicon, 15, UnityItems.steel, 10));
            health = 100;
            hasLiquids = true;
            updateEffect = Fx.fuelburn;
            craftTime = 30f;
            outputLiquid = new LiquidStack(UnityLiquids.lava, 0.1f);

            configClear(b -> Fires.create(b.tile));
            consumePower(3.7f);

            update((BurnerSmelterBuild e) -> {
                if(e.progress == 0f && e.warmup > 0.001f && (Vars.net.server() || !Vars.net.active()) && Mathf.chanceDelta(0.2f)){
                    e.configureAny(null);
                }
            });

            drawer = new DrawGlow(){
                @Override
                public void draw(GenericCrafterBuild build){
                    Draw.rect(build.block.region, build.x, build.y);

                    Liquid liquid = outputLiquid.liquid;
                    Draw.color(liquid.color, build.liquids.get(liquid) / liquidCapacity);
                    Draw.rect(top, build.x, build.y);
                    Draw.color();

                    Draw.reset();
                }
            };
        }};*/

        titaniumExtractor = new GenericCrafter("titanium-extractor"){{
            requirements(Category.crafting, with(Items.lead, 20, Items.metaglass, 10, UnityItems.denseAlloy, 30));

            health = 160;
            hasLiquids = true;
            updateEffect = UnityFx.craftFx;
            itemCapacity = 10;
            craftTime = 360f;
            outputItem = new ItemStack(Items.titanium, 1);

            consumePower(1f);
            consumeItems(with(UnityItems.denseAlloy, 3, UnityItems.steel, 2));
            consumeLiquid(Liquids.water, 0.3f);

            drawer = new DrawDefault(){
                public TextureRegion top;

                @Override
                public void load(Block block){
                    super.load(block);
                    top = Core.atlas.find(name + "-top");
                }

                @Override
                public void draw(Building build){
                    Draw.rect(build.block.region, build.x, build.y);
                    Draw.color(UnityItems.denseAlloy.color, Items.titanium.color, build.progress());
                    Draw.alpha(0.6f);
                    Draw.rect(top, build.x, build.y);
                    Draw.reset();
                }
            };
        }};

        diriumCrucible = new LevelKoruhCrafter("dirium-crucible"){{
            requirements(Category.crafting, with(Items.plastanium, 60, UnityItems.stone, 90, UnityItems.denseAlloy, 90, UnityItems.steel, 150));

            health = 320;
            hasItems = true;
            craftTime = 200f;
            craftEffect = UnityFx.diriumCraft;
            itemCapacity = 40;
            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.02f;

            outputItem = new ItemStack(UnityItems.dirium, 1);
            consumeItems(with(Items.titanium, 2, Items.coal, 4, UnityItems.uranium, 1, UnityItems.steel, 2));
            consumePower(8.28f);

            expUse = 40;
            expCapacity = 160;
            ignoreExp = false;
            craftDamage = 0;
            drawer = new DrawExp();
        }};

        coalExtractor = new KoruhCrafter("coal-extractor"){{
            requirements(Category.crafting, with(Items.silicon, 80, UnityItems.stone, 100, UnityItems.steel, 150));

            health = 250;
            hasItems = true;
            craftTime = 240f;
            craftEffect = UnityFx.craftFx;
            itemCapacity = 50;

            consumeItems(with(UnityItems.stone, 6, Items.scrap, 2));
            consumeLiquid(Liquids.water, 0.5f);
            consumePower(6f);
            outputItem = new ItemStack(Items.coal, 1);

            expUse = 30;
            expCapacity = 120;
            craftDamage = 0;
            drawer = new DrawExp();
            ignoreExp = false;

            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.01f;
        }};

        stoneWall = new LimitWall("ustone-wall"){{
            requirements(Category.defense, with(UnityItems.stone, 6));
            maxDamage = 40f;
            health = 200;
        }};

        denseWall = new LimitWall("dense-wall"){{
            requirements(Category.defense, with(UnityItems.denseAlloy, 6));
            maxDamage = 32f;
            health = 560;
        }};

        steelWall = new LevelLimitWall("steel-wall"){{
            requirements(Category.defense, with(UnityItems.steel, 6));
            maxDamage = 24f;
            health = 810;

            maxLevel = 6;
            expFields = new EField[]{
                    new EField.ERational(v -> maxDamage = v, 48f, 24f, -3f, Stat.abilities, v -> bundle.format("stat.unity.maxdamage", v)).formatAll(false)
            };
        }};

        steelWallLarge = new LevelLimitWall("steel-wall-large"){{
            requirements(Category.defense, with(UnityItems.steel, 24));
            maxDamage = 48f;
            health = 3240;
            size = 2;

            maxLevel = 12;
            expFields = new EField[]{
                    new EField.ERational(v -> maxDamage = v, 72f, 24f, -3f, Stat.abilities, v -> bundle.format("stat.unity.maxdamage", v)).formatAll(false)
            };
        }};

        diriumWall = new LevelLimitWall("dirium-wall"){{
            requirements(Category.defense, with(UnityItems.dirium, 6));
            maxDamage = 76f;
            blinkFrame = 30f;
            health = 760;
            updateEffect = UnityFx.sparkle;

            maxLevel = 6;
            expFields = new EField[]{
                    new EField.ERational(v -> maxDamage = v, 152f, 50f, -3f, Stat.abilities, v -> bundle.format("stat.unity.maxdamage", v)).formatAll(false),
                    new EField.ELinearCap(v -> blinkFrame = v, 10f, 10f, 2, Stat.abilities, v -> bundle.format("stat.unity.blinkframe", v)).formatAll(false)
            };
        }};

        diriumWallLarge = new LevelLimitWall("dirium-wall-large"){{
            requirements(Category.defense, with(UnityItems.dirium, 24));
            maxDamage = 152f;
            blinkFrame = 30f;
            health = 3040;
            size = 2;
            updateEffect = UnityFx.sparkle;

            maxLevel = 12;
            expFields = new EField[]{
                    new EField.ERational(v -> maxDamage = v, 304f, 50f, -2f, Stat.abilities, v -> bundle.format("stat.unity.maxdamage", v)).formatAll(false),
                    new EField.ELinearCap(v -> blinkFrame = v, 10f, 5f, 4, Stat.abilities, v -> bundle.format("stat.unity.blinkframe", v)).formatAll(false)
            };
        }};

        shieldProjector = new ClassicProjector("shield-generator"){{
            requirements(Category.effect, with(Items.silicon, 50, Items.titanium, 35, UnityItems.steel, 15));
            health = 200;
            cooldownNormal = 1f;
            cooldownBrokenBase = 0.3f;
            phaseRadiusBoost = 10f;
            phaseShieldBoost = 200;
            hasItems = hasLiquids = false;

            consumePower(1.5f);

            maxLevel = 15;
            expFields = new EField[]{
                    new EField.ELinear(v -> radius = v, 40f, 0.5f, Stat.range, v -> Strings.autoFixed(v / tilesize, 2) + " blocks"),
                    new EField.ELinear(v -> shieldHealth = v, 500f, 25f, Stat.shieldHealth)
            };
            fromColor = toColor = Pal.lancerLaser;
        }};

        diriumProjector = new ClassicProjector("deflect-generator"){{
            requirements(Category.effect, with(Items.silicon, 50, Items.titanium, 30, UnityItems.steel, 30, UnityItems.dirium, 8));
            health = 800;
            size = 2;
            cooldownNormal = 1.5f;
            cooldownLiquid = 1.2f;
            cooldownBrokenBase = 0.35f;
            phaseRadiusBoost = 40f;

            consumeItem(Items.phaseFabric).boost();
            consumePower(5f);

            fromColor = Pal.lancerLaser;
            toColor = UnityPal.diriumLight;
            maxLevel = 30;
            expFields = new EField[]{
                    new EField.ELinear(v -> radius = v, 60f, 0.75f, Stat.range, v -> Strings.autoFixed(v / tilesize, 2) + " blocks"),
                    new EField.ELinear(v -> shieldHealth = v, 820f, 35f, Stat.shieldHealth),
                    new EField.ELinear(v -> deflectChance = v, 0f, 0.1f, Stat.baseDeflectChance, v -> Strings.autoFixed(v * 100, 1) + "%")
            };
            pregrade = (ClassicProjector) shieldProjector;
            pregradeLevel = 5;
            effectColors = new Color[]{Pal.lancerLaser, UnityPal.lancerDir1, UnityPal.lancerDir2, UnityPal.lancerDir3, UnityPal.diriumLight};
        }};

        /*timeMine = new TimeMine("time-mine"){{
            requirements(Category.effect, with(Items.lead, 25, Items.silicon, 12));
            hasShadow = false;
            health = 45;
            pullTime = 6 * 60f;
        }};*/

        steelConveyor = new KoruhConveyor("steel-conveyor"){{
            requirements(Category.distribution, with(UnityItems.stone, 1, UnityItems.denseAlloy, 1, UnityItems.steel, 1));
            health = 140;
            speed = 0.1f;
            displayedSpeed = 12.5f;
            drawMultiplier = 1.9f;
        }};

        diriumConveyor = new ExpKoruhConveyor("dirium-conveyor"){{
            requirements(Category.distribution, with(UnityItems.steel, 1, Items.phaseFabric, 1, UnityItems.dirium, 1));
            health = 150;
            speed = 0.16f;
            displayedSpeed = 20f;
            drawMultiplier = 1.3f;

            passive = true;
            draw = new DrawOver();
        }};

        /*bufferPad = new MechPad("buffer-pad"){{
            requirements(Category.units, with(UnityItems.stone, 120, Items.copper, 170, Items.lead, 150, Items.titanium, 150, Items.silicon, 180));
            size = 2;
            craftTime = 100;
            consumePower(0.7f);
            unitType = UnityUnitTypes.buffer;
        }};

        omegaPad = new MechPad("omega-pad"){{
            requirements(Category.units, with(UnityItems.stone, 220, Items.lead, 200, Items.silicon, 230, Items.thorium, 260, Items.surgeAlloy, 100));
            size = 3;
            craftTime = 300f;
            consumePower(1.2f);
            unitType = UnityUnitTypes.omega;
        }};

        cachePad = new MechPad("cache-pad"){{
            requirements(Category.units, with(UnityItems.stone, 150, Items.lead, 160, Items.silicon, 100, Items.titanium, 60, Items.plastanium, 120, Items.phaseFabric, 60));
            size = 2;
            craftTime = 130f;
            consumePower(0.8f);
            unitType = UnityUnitTypes.cache;
        }};

        convertPad = new ConversionPad("conversion-pad"){{
            requirements(Category.units, BuildVisibility.sandboxOnly, empty);
            size = 2;
            craftTime = 60f;
            consumePower(1f);
            upgrades.add(
                    new UnitType[]{UnitTypes.dagger, UnitTypes.mace},
                    new UnitType[]{UnitTypes.flare, UnitTypes.horizon},
                    new UnitType[]{UnityUnitTypes.cache, UnityUnitTypes.dijkstra},
                    new UnitType[]{UnityUnitTypes.omega, UnitTypes.reign}
            );
        }};

        uraniumReactor = new KoruhReactor("uranium-reactor"){{
            requirements(Category.power, with(Items.plastanium, 80, Items.surgeAlloy, 100, Items.lead, 150, UnityItems.steel, 200));
            size = 3;

            itemDuration = 200f;
            consumeItem(UnityItems.uranium, 2);
            consumeLiquid(Liquids.cryofluid, 0.7f);
            consumePower(20f);

            itemCapacity = 20;
            powerProduction = 150f;
            health = 1000;
        }};*/

        teleporter = new Teleporter("teleporter"){{
            requirements(Category.distribution, with(Items.lead, 22, Items.silicon, 10, Items.phaseFabric, 32, UnityItems.dirium, 32));
        }};

        teleunit = new UnitTeleporter("teleunit"){{
            requirements(Category.units, with(Items.lead, 180, Items.titanium, 80, Items.silicon, 90, Items.phaseFabric, 64, UnityItems.dirium, 48));
            size = 3;
            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.02f;
            consumePower(3f);
        }};

        skillCenter = new SkillCenter("researchskill"){{
            requirements(Category.effect, BuildVisibility.hidden, with(Items.copper, 180, Items.lead, 80, UnityItems.stone, 180, UnityItems.denseAlloy, 48));
            size = 3;
            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.02f;
            consumePower(1f);

            expCapacity = 1000;
        }};

        laser = new ExpPowerTurret("laser-turret"){{
            requirements(Category.turret, with(Items.copper, 40, Items.silicon, 20, UnityItems.denseAlloy, 15));
            size = 2;
            health = 600;

            reload = 35f;
            coolantMultiplier = 2f;
            range = 140f;
            targetAir = false;
            shootSound = Sounds.laser;

            powerUse = 7f;
            shootType = new ExpLaserBulletType(150f, 30f){{
                damageInc = 7f;
                status = StatusEffects.shocked;
                statusDuration = 3 * 60f;
                expGain = buildingExpGain = 2;
                fromColor = Pal.accent;
                toColor = Pal.lancerLaser;
            }};

            maxLevel = 10;
            expFields = new EField[]{
                    new LinearReloadTime(v -> reload = v, 45f, -2f),
                    new EField.ELinear(v -> range = v, 120f, 2f, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks"),
                    new EField.EBool(v -> targetAir = v, false, 5, Stat.targetsAir)
            };
        }};

        laserCharge = new ExpPowerTurret("charge-laser-turret"){{
            requirements(Category.turret, with(UnityItems.denseAlloy, 20, Items.graphite, 15));
            size = 2;
            health = 1400;

            reload = 60f;
            coolantMultiplier = 2f;
            range = 140f;

            shoot.firstShotDelay = 50f;

            recoil = 2f;
            targetAir = true;
            shake = 2f;

            powerUse = 7f;

            shootEffect = ShootFx.laserChargeShoot;
            smokeEffect = Fx.none;
            heatColor = Color.red;
            shootSound = Sounds.laser;

            shootType = new ExpLaserBulletType(150f, 30f){{
                status = StatusEffects.shocked;
                statusDuration = 3 * 60f;
                fragBullet = new ExpBasicBulletType(2f, 10f){
                    {
                        lifetime = 20f;
                        pierceCap = 10;
                        pierceBuilding = true;
                        backColor = Color.white.cpy().lerp(Pal.lancerLaser, 0.1f);
                        frontColor = Color.white;
                        hitEffect = Fx.none;
                        despawnEffect = Fx.none;
                        smokeEffect = Fx.hitLaser;
                        hittable = false;
                        reflectable = false;
                        lightColor = Color.white;
                        lightOpacity = 0.6f;

                        expChance = 0.15f;
                        fromColor = Pal.lancerLaser;
                        toColor = Pal.sapBullet;
                    }

                    @Override
                    public void draw(Bullet b){
                        Draw.color(getColor(b));
                        Lines.stroke(2f * b.fout(0.7f) + 0.01f);
                        Lines.lineAngleCenter(b.x, b.y, b.rotation(), 8f);
                        Lines.stroke(1.3f * b.fout(0.7f) + 0.01f);
                        Draw.color(frontColor);
                        Lines.lineAngleCenter(b.x, b.y, b.rotation(), 5f);
                        Draw.reset();
                    }
                };

                expGain = buildingExpGain = 2;
                damageInc = 5f;
                fromColor = Pal.lancerLaser;
                toColor = Pal.sapBullet;

                chargeEffect = new MultiEffect(UnityFx.laserCharge, UnityFx.laserChargeBegin);
            }};

            maxLevel = 30;
            expFields = new EField[]{
                    new LinearReloadTime(v -> reload = v, 60f, -1f),
                    new EField.ELinear(v -> range = v, 140f, 1.3f, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks")
            };
            pregrade = (ExpTurret) laser;
            effectColors = new Color[]{Pal.lancerLaser, UnityPal.lancerSap1, UnityPal.lancerSap2, UnityPal.lancerSap3, UnityPal.lancerSap4, UnityPal.lancerSap5, Pal.sapBullet};
        }};

        laserFrost = new ExpLiquidTurret("frost-laser-turret"){{
            ammo(Liquids.cryofluid, new ExpLaserBulletType(170f, 130f){
                {
                    status = StatusEffects.freezing;
                    statusDuration = 3 * 60f;
                    shootEffect = UnityFx.shootFlake;

                    expGain = 2;
                    buildingExpGain = 3;
                    damageInc = 2.5f;
                    fromColor = Liquids.cryofluid.color;
                    toColor = Color.cyan;
                    blip = true;
                }

                @Override
                public void handleExp(Bullet b, float x, float y, int amount){
                    super.handleExp(b, x, y, amount);
                    freezePos(b, x, y);
                }

                public void freezePos(Bullet b, float x, float y){
                    int lvl = getLevel(b);
                    float rad = 3.5f;
                    UnityFx.freezeEffect.at(x, y, lvl / rad + 10f, getColor(b));
                    UnitySounds.laserFreeze.at(x, y);

                    Damage.status(b.team, x, y, 10f + lvl / rad, status, 60f + lvl * 6f, true, true);
                    Damage.status(b.team, x, y, 10f + lvl / rad, UnityStatusEffects.disabled, 2f * lvl, true, true);
                }
            });
            requirements(Category.turret, with(UnityItems.denseAlloy, 20, Items.metaglass, 15));
            size = 2;
            health = 1000;

            range = 160f;
            reload = 80f;
            targetAir = true;
            liquidCapacity = 10f;
            shootSound = Sounds.laser;
            extinguish = false;

            maxLevel = 30;

            consumePower(1f);
            pregrade = (ExpTurret) laser;
        }};

        laserFractal = new ExpPowerTurret("fractal-laser-turret"){{
            requirements(Category.turret, with(UnityItems.steel, 10, Items.graphite, 30, Items.thorium, 35));
            size = 3;
            health = 2000;

            float distFieldLife = 6 * 60;

            reload = distFieldLife / 3f;
            coolantMultiplier = 2f;
            range = 140f;

            shoot.firstShotDelay = 80f;

            recoil = 4f;

            targetAir = true;
            shake = 5f;
            powerUse = 13f;

            shootEffect = ShootFx.laserFractalShoot;
            smokeEffect = Fx.none;
            shootSound = Sounds.laser;

            heatColor = Color.red;
            fromColor = UnityPal.lancerSap3;
            toColor = Pal.place;

            shootType = new ExpLaserFieldBulletType(170f, 130f){{
                damageInc = 6f;
                lengthInc = 2f;
                fields = 2;
                fieldInc = 0.15f;
                width = 2;
                expGain = buildingExpGain = 1;
                fromColor = Pal.lancerLaser.cpy().lerp(Pal.place, 0.5f);
                toColor = Pal.place;
                maxRange = 150f + 2f * 30f; //Account for range increase

                distField = new DistFieldBulletType(0, -1){{
                    centerColor = Pal.lancerLaser.cpy().a(0);
                    edgeColor = Pal.place;
                    distSplashFx = UnityFx.distSplashFx;
                    distStart = UnityFx.distStart;
                    distStatus = distort;

                    collidesTiles = false;
                    collides = false;
                    collidesAir = false;
                    keepVelocity = false;

                    lifetime = distFieldLife;
                    radius = 3f*8;
                    radiusInc = 0.1f*8;
                    bulletSlow = 0.1f;
                    bulletSlowInc = 0.025f;
                    damageLimit = 100f;
                    distDamage = 0.1f;
                    expChance = 0.5f/60;
                    expGain = 1;
                }};


                smallDistField = new DistFieldBulletType(0, -1){{
                    centerColor = Pal.lancerLaser.cpy().a(0);
                    edgeColor = Pal.place;
                    distSplashFx = UnityFx.distSplashFx;
                    distStart = UnityFx.distStart;
                    distStatus = distort;

                    collidesTiles = false;
                    collides = false;
                    collidesAir = false;
                    keepVelocity = false;

                    lifetime = 2.5f * 60;
                    radius = 1.5f*8;
                    radiusInc = 0;
                    bulletSlow = 0.05f;
                    bulletSlowInc = 0;
                    damageLimit = 50f;
                    distDamage = 0.05f;
                    expChance = 0.1f/60;
                    expGain = 1;
                }};

                chargeEffect = new MultiEffect(UnityFx.laserFractalCharge, UnityFx.laserFractalChargeBegin);
            }};

            maxLevel = 30;
            expFields = new EField[]{
                    new LinearReloadTime(v -> reload = v, distFieldLife / 3f, -2f),
                    new EField.ELinear(v -> range = v, 140f, 0.25f * tilesize, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks")
            };

            pregrade = (ExpTurret) laserCharge;
            pregradeLevel = 15;
            effectColors = new Color[]{fromColor, Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.75f), Pal.sapBullet};
        }};

        laserBranch = new BurstChargePowerTurret("swarm-laser-turret"){{
            requirements(Category.turret, with(UnityItems.steel, 10, Items.silicon, 20, Items.thorium, 45));

            size = 3;
            health = 2400;

            reload = 90f;
            coolantMultiplier = 2.25f;
            powerUse = 15f;
            targetAir = true;
            range = 150f;

            shoot.firstShotDelay = 50f;
            recoil = 2f;

            shake = 2f;
            shootEffect = ShootFx.laserChargeShootShort;
            smokeEffect = Fx.none;
            heatColor = Color.red;
            fromColor = UnityPal.lancerSap3;
            shootSound = Sounds.plasmaboom;
            shootType = new ExpLaserBulletType(140f, 20f){{
                status = StatusEffects.shocked;
                statusDuration = 3 * 60f;
                fragBullets = 3;
                fragBullet = new ExpBulletType(3.5f, 15f){
                    {
                        trailWidth = 2f;
                        weaveScale = 0.6f;
                        weaveMag = 0.5f;
                        homingPower = 0.4f;
                        lifetime = 30f;
                        shootEffect = Fx.hitLancer;
                        hitEffect = despawnEffect = HitFx.branchFragHit;
                        pierceCap = 10;
                        pierceBuilding = true;
                        splashDamageRadius = 4f;
                        splashDamage = 4f;
                        status = UnityStatusEffects.plasmaed;
                        statusDuration = 180f;
                        trailLength = 6;
                        trailColor = Color.white;

                        fromColor = Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.5f);
                        toColor = Pal.sapBullet;
                        expGain = 1;
                        expOnHit = true;
                    }

                    @Override
                    public void init(){
                        super.init();
                        despawnHit = false;
                    }

                    @Override
                    public void draw(Bullet b){
                        drawTrail(b);

                        Draw.color(getColor(b));
                        Fill.square(b.x, b.y, trailWidth, b.rotation() + 45);
                        Draw.color();
                    }
                };
                maxRange = 150f + 2f * 30f; //Account for range increase

                expGain = buildingExpGain = 1;
                damageInc = 6f;
                lengthInc = 2f;
                fromColor = Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.5f);
                toColor = Pal.sapBullet;
                hitMissed = true;

                chargeEffect = new MultiEffect(UnityFx.laserChargeShort, UnityFx.laserChargeBegin);
            }};

            shootY = size * tilesize / 2.7f;
            shoot.shots = 4;
            shoot.shotDelay = 20f;
            inaccuracy = 1f;
            xRand = 6f; //TODO replace -Anuke

            maxLevel = 30;
            expFields = new EField[]{
                    new EField.ELinearCap(v -> shoot.shots = (int)v, 2, 0.35f, 15, Stat.shots),
                    new EField.ELinearCap(v -> inaccuracy = v, 1f, 0.25f, 10, Stat.inaccuracy, v -> Strings.autoFixed(v, 1) + " degrees"),
                    new EField.ELinear(v -> shoot.shotDelay = v, 20f, -0.5f, null),
                    new EField.ELinear(v -> range = v, 150f, 2f, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks")
            };
            pregrade = (ExpTurret) laserCharge;
            pregradeLevel = 15;
            effectColors = new Color[]{UnityPal.lancerSap3, UnityPal.lancerSap4, UnityPal.lancerSap5, Pal.sapBullet};
        }};

        laserKelvin = new OmniLiquidTurret("kelvin-laser-turret"){{
            requirements(Category.turret, with(Items.phaseFabric, 10, Items.metaglass, 30, Items.thorium, 35));
            size = 3;
            health = 2100;

            range = 180f;
            reload = 120f;
            targetAir = true;
            liquidCapacity = 15f;
            shootAmount = 3f;
            shootSound = Sounds.laser;

            shootType = new GeyserLaserBulletType(185f, 30f){{
                geyser = new GeyserBulletType(){{
                    damageInc = 2f;
                }};
                damageInc = 5f;
                maxRange = 185f;
            }};

            consumePower(2.5f);

            maxLevel = 30;
            pregrade = (ExpTurret) laserFrost;
            pregradeLevel = 15;
        }};

        laserBreakthrough = new ExpPowerTurret("bt-laser-turret"){{
            requirements(Category.turret, with(UnityItems.dirium, 90, Items.silicon, 130, Items.thorium, 150, UnityItems.steel, 130));
            size = 4;
            health = 2800;

            range = 500f;
            coolantMultiplier = 1.5f;
            targetAir = true;
            reload = 500f;

            shoot.firstShotDelay = 100f;
            recoil = 5f;
            powerUse = 17f;

            shake = 4f;
            shootEffect = ShootFx.laserBreakthroughShoot;
            smokeEffect = Fx.none;

            heatColor = fromColor = Pal.lancerLaser;
            toColor = UnityPal.exp;
            shootSound = Sounds.laserblast;
            chargeSound = Sounds.lasercharge;
            shootType = new ExpLaserBlastBulletType(500f, 1200f){{
                damageInc = 1000f;
                lengthInc = 150f;
                largeHit = true;
                width = 80f;
                widthInc = 10f;
                lifetime = 65f;
                lightningSpacingInc = -5f;
                lightningDamageInc = 30f;
                hitUnitExpGain = 1;
                hitBuildingExpGain = 1;
                sideLength = 0f;
                sideWidth = 0f;
                chargeEffect = UnityFx.laserBreakthroughChargeBegin;
            }};

            maxLevel = 1;
            expScale = 30;
            pregrade = (ExpTurret) laserCharge;
            pregradeLevel = 30;
            expFields = new EField[]{
                new EField.EList<>(v -> shootType.chargeEffect = v, new Effect[]{UnityFx.laserBreakthroughChargeBegin, UnityFx.laserBreakthroughChargeBegin2}, null)
            };
            effectColors = new Color[]{Pal.lancerLaser, UnityPal.exp};

            drawer = new DrawTurret(){
                @Override
                public void draw(Building build){
                    super.draw(build);

                    ExpPowerTurretBuild e = (ExpPowerTurretBuild)build;

                    if(e.level() >= e.maxLevel()){
                        //Draw.blend(Blending.additive);
                        Draw.color(e.shootColor(Tmp.c2));
                        Draw.alpha(Mathf.absin(Time.time, 20f, 0.6f));
                        Draw.rect(Regions.btLaserTurretTopRegion, e.x + e.recoilOffset.x, e.y + e.recoilOffset.y, e.rotation - 90f);
                        Draw.color();
                        //Draw.blend();
                    }
                }
            };
        }};

        inferno = new ExpItemTurret("inferno"){{
            requirements(Category.turret, with(UnityItems.stone, 50, UnityItems.denseAlloy, 45, Items.graphite, 40));
            ammo(
                    Items.scrap, new LiquidBulletType(Liquids.slag){{
                        damage = 4;
                        drag = 0.01f;
                    }},
                    Items.coal, new ExpBulletType(3.35f, 32f){{
                        ammoMultiplier = 3;
                        hitSize = 7f;
                        lifetime = 24f;
                        pierce = true;
                        statusDuration = 60 * 4f;
                        shootEffect = ShootFx.shootSmallBlaze;
                        hitEffect = Fx.hitFlameSmall;
                        despawnEffect = Fx.none;
                        status = StatusEffects.burning;
                        keepVelocity = true;
                        hittable = false;

                        expOnHit = true;
                        expChance = 0.5f;
                    }},
                    Items.pyratite, new ExpBulletType(3.35f, 46f){{
                        ammoMultiplier = 3;
                        hitSize = 7f;
                        lifetime = 24f;
                        pierce = true;
                        statusDuration = 60 * 4f;
                        shootEffect = ShootFx.shootPyraBlaze;
                        hitEffect = Fx.hitFlameSmall;
                        despawnEffect = Fx.none;
                        status = StatusEffects.burning;
                        keepVelocity = false;
                        hittable = false;

                        expOnHit = true;
                        expChance = 0.6f;
                    }}
            );

            size = 3;
            range = 80f;
            reload = 6f;
            coolantMultiplier = 2f;
            recoil = 0f;
            shootCone = 5f;
            shootSound = Sounds.flame;
            shoot = new ShootSpread();

            maxLevel = 10;
            expFields = new EField[]{
                    new EField.EList<>(v -> shoot.shots = v, new Integer[]{1, 1, 2, 2, 2, 3, 3, 4, 4, 5, 5}, Stat.shots),
                    new EField.EList<>(v -> ((ShootSpread)shoot).spread = v, new Float[]{0f, 0f, 5f, 10f, 15f, 7f, 14f, 8f, 10f, 6f, 9f}, null)
            };
        }};

        expHub = new ExpHub("exp-output"){{
            requirements(expCategory, with(UnityItems.stone, 30, Items.copper, 15));
            expCapacity = 100;
        }};

        expRouter = new ExpRouter("exp-router"){{
            requirements(expCategory, with(UnityItems.stone, 5));
        }};

        expTower = new ExpTower("exp-tower"){{
            requirements(expCategory, with(UnityItems.denseAlloy, 10, Items.silicon, 5));
            expCapacity = 100;
        }};

        expTowerDiagonal = new DiagonalTower("diagonal-tower"){{
            requirements(expCategory, with(UnityItems.steel, 10, Items.silicon, 5));
            range = 7;
            expCapacity = 150;
        }};

        bufferTower = new ExpTower("buffer-tower"){{
            requirements(expCategory, with(Items.thorium, 5, Items.graphite, 10));
            manualReload = reloadTime = 20f;
            expCapacity = 180;
            buffer = true;
            health = 300;
        }};

        expNode = new ExpNode("exp-node"){{
            requirements(expCategory, with(UnityItems.denseAlloy, 30, Items.silicon, 30, UnityItems.steel, 8));
            expCapacity = 200;
            consumePower(0.6f);
        }};

        expNodeLarge = new ExpNode("exp-node-large"){{
            requirements(expCategory, with(UnityItems.denseAlloy, 120, Items.silicon, 120, UnityItems.steel, 24));
            expCapacity = 600;
            range = 10;
            health = 200;
            size = 2;
            consumePower(1.4f);
        }};

        expTank = new ExpTank("exp-tank"){{
            requirements(Category.effect, with(Items.copper, 100, UnityItems.denseAlloy, 100, Items.graphite, 30));
            expCapacity = 800;
            health = 300;
            size = 2;
        }};

        expChest = new ExpTank("exp-chest"){{
            requirements(Category.effect, with(Items.copper, 400, UnityItems.steel, 250, Items.phaseFabric, 120));
            expCapacity = 3600;
            health = 1200;
            size = 4;
        }};

        expFountain = new ExpSource("exp-fountain"){{
            requirements(expCategory, BuildVisibility.sandboxOnly, with());
        }};

        expVoid = new ExpVoid("exp-void"){{
            requirements(expCategory, BuildVisibility.sandboxOnly, with());
        }};
    }
}
