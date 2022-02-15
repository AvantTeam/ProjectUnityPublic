package unity.content.blocks;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import unity.annotations.*;
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
import unity.world.consumers.*;
import unity.world.draw.*;

import static arc.Core.bundle;
import static mindustry.Vars.tilesize;
import static mindustry.type.ItemStack.empty;
import static mindustry.type.ItemStack.with;

public class KoruhBlocks {
    public static @Annotations.FactionDef("koruh")
    Block
    //crafting
    denseSmelter, solidifier, steelSmelter, titaniumExtractor, lavaSmelter, diriumCrucible, coalExtractor,

    //defense
    stoneWall, denseWall, steelWall, steelWallLarge, diriumWall, diriumWallLarge, shieldProjector, diriumProjector,

    //distribution
    steelConveyor, teleporter;

    public static @Annotations.FactionDef("koruh")
    @Annotations.Dupe(base = ExpTurret.class, parent = KoruhConveyor.class)
    Block diriumConveyor;

    //unit
    //public static @Annotations.FactionDef("koruh") Block bufferPad, omegaPad, cachePad, convertPad,

    //power
    //uraniumReactor,

    //TODO
    public static @Annotations.FactionDef("koruh") Block expFountain, expVoid, expTank, expChest, expRouter, expTower, expTowerDiagonal, bufferTower, expHub, expNode, expNodeLarge;// expOutput, expUnloader;

    //turret
    public static @Annotations.FactionDef("koruh")
    @Annotations.LoadRegs("bt-laser-turret-top")
    Block laser, laserCharge, laserBranch, laserBreakthrough;

    public static @Annotations.FactionDef("koruh")
    Block laserFrost, laserKelvin;

    public static @Annotations.FactionDef("koruh")
    Block inferno;

    public static void load(){
        denseSmelter = new KoruhCrafter("dense-smelter"){{
            requirements(Category.crafting, with(Items.copper, 30, Items.lead, 20, UnityItems.stone, 35));

            health = 70;
            hasItems = true;
            craftTime = 46.2f;
            craftEffect = UnityFx.denseCraft;
            itemCapacity = 10;

            outputItem = new ItemStack(UnityItems.denseAlloy, 1);
            consumes.items(with(Items.copper, 1, Items.lead, 2, Items.coal, 1));

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

            consumes.add(new ConsumeLiquids(new LiquidStack[]{new LiquidStack(UnityLiquids.lava, 0.1f), new LiquidStack(Liquids.water, 0.1f)}));

            drawer = new DrawGlow(){
                @Override
                public void draw(GenericCrafterBuild build){
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

            consumes.power(2f);
            consumes.items(with(Items.coal, 2, Items.graphite, 2, UnityItems.denseAlloy, 3));

            drawer = new DrawGlow(){
                @Override
                public void draw(GenericCrafterBuild build){
                    Draw.rect(build.block.region, build.x, build.y);
                    Draw.color(1f, 1f, 1f, build.warmup * Mathf.absin(8f, 0.6f));
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
            consumes.items(with(Items.graphite, 7, UnityItems.denseAlloy, 7));
            consumes.power(2f);
            consumes.liquid(UnityLiquids.lava, 0.4f);

            expUse = 10;
            expCapacity = 60;
            drawer = new DrawLiquid();
        }};

        /*liquifier = new BurnerSmelter("liquifier"){{
            requirements(Category.crafting, BuildVisibility.hidden, with(Items.titanium, 30, Items.silicon, 15, UnityItems.steel, 10));
            health = 100;
            hasLiquids = true;
            updateEffect = Fx.fuelburn;
            craftTime = 30f;
            outputLiquid = new LiquidStack(UnityLiquids.lava, 0.1f);

            configClear(b -> Fires.create(b.tile));
            consumes.power(3.7f);

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

            consumes.power(1f);
            consumes.items(with(UnityItems.denseAlloy, 3, UnityItems.steel, 2));
            consumes.liquid(Liquids.water, 0.3f);

            drawer = new DrawGlow(){
                @Override
                public void draw(GenericCrafterBuild build){
                    Draw.rect(build.block.region, build.x, build.y);
                    Draw.color(UnityItems.denseAlloy.color, Items.titanium.color, build.progress);
                    Draw.alpha(0.6f);
                    Draw.rect(top, build.x, build.y);
                    Draw.reset();
                }
            };
        }};

        diriumCrucible = new KoruhCrafter("dirium-crucible"){{
            requirements(Category.crafting, with(Items.plastanium, 60, UnityItems.stone, 90, UnityItems.denseAlloy, 90, UnityItems.steel, 150));

            health = 320;
            hasItems = true;
            craftTime = 250f;
            craftEffect = UnityFx.diriumCraft;
            itemCapacity = 40;
            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.02f;

            outputItem = new ItemStack(UnityItems.dirium, 1);
            consumes.items(with(Items.titanium, 6, Items.pyratite, 3, Items.surgeAlloy, 3, UnityItems.steel, 9));
            consumes.power(8.28f);

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

            consumes.items(with(UnityItems.stone, 6, Items.scrap, 2));
            consumes.liquid(Liquids.water, 0.5f);
            consumes.power(6f);
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

            consumes.power(1.5f);

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

            consumes.item(Items.phaseFabric).boost();
            consumes.power(5f);

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

            draw = new DrawOver();
        }};

        /*bufferPad = new MechPad("buffer-pad"){{
            requirements(Category.units, with(UnityItems.stone, 120, Items.copper, 170, Items.lead, 150, Items.titanium, 150, Items.silicon, 180));
            size = 2;
            craftTime = 100;
            consumes.power(0.7f);
            unitType = UnityUnitTypes.buffer;
        }};

        omegaPad = new MechPad("omega-pad"){{
            requirements(Category.units, with(UnityItems.stone, 220, Items.lead, 200, Items.silicon, 230, Items.thorium, 260, Items.surgeAlloy, 100));
            size = 3;
            craftTime = 300f;
            consumes.power(1.2f);
            unitType = UnityUnitTypes.omega;
        }};

        cachePad = new MechPad("cache-pad"){{
            requirements(Category.units, with(UnityItems.stone, 150, Items.lead, 160, Items.silicon, 100, Items.titanium, 60, Items.plastanium, 120, Items.phaseFabric, 60));
            size = 2;
            craftTime = 130f;
            consumes.power(0.8f);
            unitType = UnityUnitTypes.cache;
        }};

        convertPad = new ConversionPad("conversion-pad"){{
            requirements(Category.units, BuildVisibility.sandboxOnly, empty);
            size = 2;
            craftTime = 60f;
            consumes.power(1f);
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
            consumes.item(UnityItems.uranium, 2);
            consumes.liquid(Liquids.cryofluid, 0.7f);
            consumes.power(20f);

            itemCapacity = 20;
            powerProduction = 150f;
            health = 1000;
        }};*/

        teleporter = new Teleporter("teleporter"){{
            requirements(Category.distribution, with(Items.lead, 22, Items.silicon, 10, Items.phaseFabric, 32, UnityItems.dirium, 32));
        }};

        /*teleunit = new TeleUnit("teleunit"){{
            requirements(Category.units, with(Items.lead, 180, Items.titanium, 80, Items.silicon, 90, Items.phaseFabric, 64, UnityItems.dirium, 48));
            size = 3;
            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.02f;
            consumes.power(3f);
        }};*/

        laser = new ExpPowerTurret("laser-turret"){{
            requirements(Category.turret, with(Items.copper, 90, Items.silicon, 40, Items.titanium, 15));
            size = 2;
            health = 600;

            reloadTime = 35f;
            coolantMultiplier = 2f;
            range = 140f;
            targetAir = false;
            shootSound = Sounds.laser;

            powerUse = 7f;
            shootType = UnityBullets.laser;

            maxLevel = 10;
            expFields = new EField[]{
                    new LinearReloadTime(v -> reloadTime = v, 45f, -2f),
                    new EField.ELinear(v -> range = v, 120f, 2f, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks"),
                    new EField.EBool(v -> targetAir = v, false, 5, Stat.targetsAir)
            };
        }};

        laserCharge = new ExpPowerTurret("charge-laser-turret"){{
            requirements(Category.turret, with(UnityItems.denseAlloy, 60, Items.graphite, 15));
            size = 2;
            health = 1400;

            reloadTime = 60f;
            coolantMultiplier = 2f;
            range = 140f;

            chargeTime = 50f;
            chargeMaxDelay = 30f;
            chargeEffects = 4;
            recoilAmount = 2f;
            cooldown = 0.03f;
            targetAir = true;
            shootShake = 2f;

            powerUse = 7f;

            shootEffect = ShootFx.laserChargeShoot;
            smokeEffect = Fx.none;
            chargeEffect = UnityFx.laserCharge;
            chargeBeginEffect = UnityFx.laserChargeBegin;
            heatColor = Color.red;
            shootSound = Sounds.laser;

            shootType = UnityBullets.shardLaser;

            maxLevel = 30;
            expFields = new EField[]{
                    new LinearReloadTime(v -> reloadTime = v, 60f, -1f),
                    new EField.ELinear(v -> range = v, 140f, 1.3f, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks")
            };
            pregrade = (ExpTurret) laser;
            effectColors = new Color[]{Pal.lancerLaser, UnityPal.lancerSap1, UnityPal.lancerSap2, UnityPal.lancerSap3, UnityPal.lancerSap4, UnityPal.lancerSap5, Pal.sapBullet};
        }};

        laserFrost = new ExpLiquidTurret("frost-laser-turret"){{
            ammo(Liquids.cryofluid, UnityBullets.frostLaser);
            requirements(Category.turret, with(UnityItems.denseAlloy, 60, Items.metaglass, 15));
            size = 2;
            health = 1000;

            range = 160f;
            reloadTime = 80f;
            targetAir = true;
            liquidCapacity = 10f;
            shootSound = Sounds.laser;
            extinguish = false;

            maxLevel = 30;

            consumes.powerCond(1f, TurretBuild::isActive);
            pregrade = (ExpTurret) laser;
        }};

        /*laserFractal = new ExpPowerTurret("fractal-laser-turret"){{
            requirements(Category.turret, with(UnityItems.steel, 50, Items.graphite, 90, Items.thorium, 95));
            size = 3;
            health = 2000;

            reloadTime = UnityBullets.distField.lifetime / 3f;
            coolantMultiplier = 2f;
            range = 140f;

            chargeTime = 50f;
            chargeMaxDelay = 40f;
            chargeEffects = 5;
            recoilAmount = 4f;

            cooldown = 0.03f;
            targetAir = true;
            shootShake = 5f;
            powerUse = 13f;

            shootEffect = ShootFx.laserChargeShoot;
            smokeEffect = Fx.none;
            chargeEffect = UnityFx.laserCharge;
            chargeBeginEffect = UnityFx.laserChargeBegin;
            heatColor = Color.red;
            shootSound = Sounds.laser;

            fromColor = UnityPal.lancerSap3;
            toColor = Pal.place;

            shootType = UnityBullets.fractalLaser;

            //todo fractal laser (handle radius on the bullet's side
            //basicFieldRadius = 85f;

            maxLevel = 30;
            expFields = new EField[]{
                    new LinearReloadTime(v -> reloadTime = v, UnityBullets.distField.lifetime / 3f, -2f),
                    new EField.ELinear(v -> range = v, 140f, 0.25f * tilesize, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks")
            };
            //progression.linear(basicFieldRadius, 0.2f * tilesize, val -> basicFieldRadius = val);

            //bulletCons((ExpLaserFieldBulletType type, Bullet b) -> type.basicFieldRadius = basicFieldRadius);
            pregrade = (ExpTurret) laserCharge;
            pregradeLevel = 15;
            effectColors = new Color[]{fromColor, Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.75f), Pal.sapBullet};
        }};*/

        laserBranch = new BurstChargePowerTurret("swarm-laser-turret"){{
            requirements(Category.turret, with(UnityItems.steel, 50, Items.silicon, 90, Items.thorium, 95));

            size = 3;
            health = 2400;

            reloadTime = 90f;
            coolantMultiplier = 2.25f;
            powerUse = 15f;
            targetAir = true;
            range = 150f;

            chargeTime = 50f;
            chargeMaxDelay = 30f;
            chargeEffects = 4;
            recoilAmount = 2f;

            cooldown = 0.03f;
            shootShake = 2f;
            shootEffect = ShootFx.laserChargeShootShort;
            smokeEffect = Fx.none;
            chargeEffect = UnityFx.laserChargeShort;
            chargeBeginEffect = UnityFx.laserChargeBegin;
            heatColor = Color.red;
            fromColor = UnityPal.lancerSap3;
            shootSound = Sounds.plasmaboom;
            shootType = UnityBullets.branchLaser;

            shootLength = size * tilesize / 2.7f;
            shots = 4;
            burstSpacing = 20f;
            inaccuracy = 1f;
            spread = 0f;
            xRand = 6f;

            maxLevel = 30;
            expFields = new EField[]{
                    new EField.ELinearCap(v -> shots = (int)v, 2, 0.35f, 15, Stat.shots),
                    new EField.ELinearCap(v -> inaccuracy = v, 1f, 0.25f, 10, Stat.inaccuracy, v -> Strings.autoFixed(v, 1) + " degrees"),
                    new EField.ELinear(v -> burstSpacing = v, 20f, -0.5f, null),
                    new EField.ELinear(v -> range = v, 150f, 2f, Stat.shootRange, v -> Strings.autoFixed(v / tilesize, 2) + " blocks")
            };
            pregrade = (ExpTurret) laserCharge;
            pregradeLevel = 15;
            effectColors = new Color[]{UnityPal.lancerSap3, UnityPal.lancerSap4, UnityPal.lancerSap5, Pal.sapBullet};
        }};

        laserKelvin = new OmniLiquidTurret("kelvin-laser-turret"){{
            requirements(Category.turret, with(Items.phaseFabric, 50, Items.metaglass, 90, Items.thorium, 95));
            size = 3;
            health = 2100;

            range = 180f;
            reloadTime = 120f;
            targetAir = true;
            liquidCapacity = 15f;
            shootAmount = 3f;
            shootSound = Sounds.laser;

            shootType = new GeyserLaserBulletType(185f, 30f){{
                geyser = UnityBullets.laserGeyser;
                damageInc = 5f;
                maxRange = 185f;
            }};

            consumes.powerCond(2.5f, TurretBuild::isActive);

            maxLevel = 30;
            pregrade = (ExpTurret) laserFrost;
            pregradeLevel = 15;
        }};

        laserBreakthrough = new ExpPowerTurret("bt-laser-turret"){{
            requirements(Category.turret, with(UnityItems.dirium, 190, Items.silicon, 230, Items.thorium, 450, UnityItems.steel, 230));
            size = 4;
            health = 2800;

            range = 500f;
            coolantMultiplier = 1.5f;
            targetAir = true;
            reloadTime = 500f;

            chargeTime = 100f;
            chargeMaxDelay = 100f;
            chargeEffects = 0;

            recoilAmount = 5f;
            cooldown = 0.03f;
            powerUse = 17f;

            shootShake = 4f;
            shootEffect = ShootFx.laserBreakthroughShoot;
            smokeEffect = Fx.none;
            chargeEffect = Fx.none;
            chargeBeginEffect = UnityFx.laserBreakthroughChargeBegin;

            heatColor = fromColor = Pal.lancerLaser;
            toColor = UnityPal.exp;
            shootSound = Sounds.laserblast;
            chargeSound = Sounds.lasercharge;
            shootType = UnityBullets.breakthroughLaser;

            maxLevel = 1;
            expScale = 30;
            pregrade = (ExpTurret) laserCharge;
            pregradeLevel = 30;
            expFields = new EField[]{
                    new EField.EList<>(v -> chargeBeginEffect = v, new Effect[]{UnityFx.laserBreakthroughChargeBegin, UnityFx.laserBreakthroughChargeBegin2}, null)
            };
            effectColors = new Color[]{Pal.lancerLaser, UnityPal.exp};

            drawer = b -> {
                if(b instanceof ExpPowerTurretBuild tile){
                    Draw.rect(region, tile.x + tr2.x, tile.y + tr2.y, tile.rotation - 90f);
                    if(tile.level() >= tile.maxLevel()){
                        //Draw.blend(Blending.additive);
                        Draw.color(tile.shootColor(Tmp.c2));
                        Draw.alpha(Mathf.absin(Time.time, 20f, 0.6f));
                        Draw.rect(Regions.btLaserTurretTopRegion, tile.x + tr2.x, tile.y + tr2.y, tile.rotation - 90f);
                        Draw.color();
                        //Draw.blend();
                    }
                }else{
                    throw new IllegalStateException("building isn't an instance of ExpPowerTurretBuild");
                }
            };
        }};

        inferno = new ExpItemTurret("inferno"){{
            requirements(Category.turret, with(UnityItems.stone, 150, UnityItems.denseAlloy, 65, Items.graphite, 60));
            ammo(
                    Items.scrap, Bullets.slagShot,
                    Items.coal, UnityBullets.coalBlaze,
                    Items.pyratite, UnityBullets.pyraBlaze
            );

            size = 3;
            range = 80f;
            reloadTime = 6f;
            coolantMultiplier = 2f;
            recoilAmount = 0f;
            shootCone = 5f;
            shootSound = Sounds.flame;

            maxLevel = 10;
            expFields = new EField[]{
                    new EField.EList<>(v -> shots = v, new Integer[]{1, 1, 2, 2, 2, 3, 3, 4, 4, 5, 5}, Stat.shots),
                    new EField.EList<>(v -> spread = v, new Float[]{0f, 0f, 5f, 10f, 15f, 7f, 14f, 8f, 10f, 6f, 9f}, null)
            };
        }};

        expHub = new ExpHub("exp-output"){{
            requirements(Category.effect, with(UnityItems.stone, 30, Items.copper, 15));
            expCapacity = 100;
        }};

        expRouter = new ExpRouter("exp-router"){{
            requirements(Category.effect, with(UnityItems.stone, 5));
        }};

        expTower = new ExpTower("exp-tower"){{
            requirements(Category.effect, with(UnityItems.denseAlloy, 10, Items.silicon, 5));
            expCapacity = 100;
        }};

        expTowerDiagonal = new DiagonalTower("diagonal-tower"){{
            requirements(Category.effect, with(UnityItems.steel, 10, Items.silicon, 5));
            range = 7;
            expCapacity = 150;
        }};

        bufferTower = new ExpTower("buffer-tower"){{
            requirements(Category.effect, with(Items.thorium, 5, Items.graphite, 10));
            manualReload = reloadTime = 20f;
            expCapacity = 180;
            buffer = true;
            health = 300;
        }};

        expNode = new ExpNode("exp-node"){{
            requirements(Category.effect, with(UnityItems.denseAlloy, 30, Items.silicon, 30, UnityItems.steel, 8));
            expCapacity = 200;
            consumes.power(0.6f);
        }};

        expNodeLarge = new ExpNode("exp-node-large"){{
            requirements(Category.effect, with(UnityItems.denseAlloy, 120, Items.silicon, 120, UnityItems.steel, 24));
            expCapacity = 600;
            range = 10;
            health = 200;
            size = 2;
            consumes.power(1.4f);
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
            requirements(Category.effect, BuildVisibility.sandboxOnly, with());
        }};

        expVoid = new ExpVoid("exp-void"){{
            requirements(Category.effect, BuildVisibility.sandboxOnly, with());
        }};
    }
}
