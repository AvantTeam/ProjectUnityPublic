package unity.content;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.content.effects.*;
import unity.entities.bullet.energy.*;
import unity.entities.bullet.misc.*;
import unity.entities.bullet.exp.*;
import unity.gen.*;

import static unity.content.UnityStatusEffects.distort;

public class UnityBullets{
    public static BulletType
    basicMissile, boidMissle,

    citadelFlame,

    sapLaser, sapArtilleryFrag, continuousSapLaser,

    laser, shardLaserFrag, shardLaser, frostLaser, branchLaserFrag, branchLaser, distField, smallDistField, fractalLaser,
    breakthroughLaser, laserGeyser,

    coalBlaze, pyraBlaze;

    private UnityBullets(){
        throw new AssertionError();
    }

    public static void load(){
        basicMissile = new MissileBulletType(4.2f, 15){{
            homingPower = 0.12f;
            width = 8f;
            height = 8f;
            shrinkX = shrinkY = 0f;
            drag = -0.003f;
            homingRange = 80f;
            keepVelocity = false;
            splashDamageRadius = 35f;
            splashDamage = 30f;
            lifetime = 62f;
            trailColor = Pal.missileYellowBack;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            weaveScale = 8f;
            weaveMag = 2f;
        }};

        citadelFlame = new FlameBulletType(4.2f, 50f){{
            lifetime = 20f;
            particleAmount = 17;
        }};

        sapLaser = new LaserBulletType(80f){{
            colors = new Color[]{Pal.sapBulletBack.cpy().a(0.4f), Pal.sapBullet, Color.white};
            length = 150f;
            width = 25f;
            sideLength = sideWidth = 0f;
            shootEffect = ShootFx.sapPlasmaShoot;
            hitColor = lightColor = lightningColor = Pal.sapBullet;
            status = StatusEffects.sapped;
            statusDuration = 80f;
            lightningSpacing = 17f;
            lightningDelay = 0.12f;
            lightningDamage = 15f;
            lightningLength = 4;
            lightningLengthRand = 2;
            lightningAngleRand = 15f;
        }};

        sapArtilleryFrag = new ArtilleryBulletType(2.3f, 30){{
            hitEffect = Fx.sapExplosion;
            knockback = 0.8f;
            lifetime = 70f;
            width = height = 20f;
            collidesTiles = false;
            splashDamageRadius = 70f;
            splashDamage = 60f;
            backColor = Pal.sapBulletBack;
            frontColor = lightningColor = Pal.sapBullet;
            lightning = 2;
            lightningLength = 5;
            smokeEffect = Fx.shootBigSmoke2;
            hitShake = 5f;
            lightRadius = 30f;
            lightColor = Pal.sap;
            lightOpacity = 0.5f;

            status = StatusEffects.sapped;
            statusDuration = 60f * 10;
        }};

        boidMissle = new BoidBulletType(2.7f, 30){{
            damage = 50;
            homingPower = 0.02f;
            lifetime = 500f;
            keepVelocity = false;
            shootEffect = Fx.shootHeal;
            smokeEffect = Fx.hitLaser;
            hitEffect = despawnEffect = Fx.hitLaser;
            hitSound = Sounds.none;

            healPercent = 5.5f;
            collidesTeam = true;
            trailColor = Pal.heal;
            backColor = Pal.heal;
        }};

        continuousSapLaser = new ContinuousLaserBulletType(60f){
            {
                colors = new Color[]{Pal.sapBulletBack.cpy().a(0.3f), Pal.sapBullet.cpy().a(0.6f), Pal.sapBullet, Color.white};
                length = 190f;
                width = 5f;
                shootEffect = ShootFx.sapPlasmaShoot;
                hitColor = lightColor = lightningColor = Pal.sapBullet;
                hitEffect = HitFx.coloredHitSmall;
                status = StatusEffects.sapped;
                statusDuration = 80f;
                lifetime = 180f;
                incendChance = 0f;
                largeHit = false;
            }

            @Override
            public void hitTile(Bullet b, Building build, float initialHealth, boolean direct){
                super.hitTile(b, build, initialHealth, direct);
                if(b.owner instanceof Healthc owner){
                    owner.heal(Math.max(initialHealth - build.health(), 0f) * 0.2f);
                }
            }

            @Override
            public void hitEntity(Bullet b, Hitboxc entity, float health){
                super.hitEntity(b, entity, health);
                if(entity instanceof Healthc h && b.owner instanceof Healthc owner){
                    owner.heal(Math.max(health - h.health(), 0f) * 0.2f);
                }
            }
        };

        laser = new ExpLaserBulletType(150f, 30f){{
            damageInc = 7f;
            status = StatusEffects.shocked;
            statusDuration = 3 * 60f;
            expGain = buildingExpGain = 2;
            fromColor = Pal.accent;
            toColor = Pal.lancerLaser;
        }};

        shardLaserFrag = new ExpBasicBulletType(2f, 10f){
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

        shardLaser = new ExpLaserBulletType(150f, 30f){{
            status = StatusEffects.shocked;
            statusDuration = 3 * 60f;
            fragBullet = shardLaserFrag;

            expGain = buildingExpGain = 2;
            damageInc = 5f;
            fromColor = Pal.lancerLaser;
            toColor = Pal.sapBullet;
        }};

        frostLaser = new ExpLaserBulletType(170f, 130f){
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
        };

        branchLaserFrag = new ExpBulletType(3.5f, 15f){
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

        branchLaser = new ExpLaserBulletType(140f, 20f){{
            status = StatusEffects.shocked;
            statusDuration = 3 * 60f;
            fragBullets = 3;
            fragBullet = branchLaserFrag;
            maxRange = 150f + 2f * 30f; //Account for range increase

            expGain = buildingExpGain = 1;
            damageInc = 6f;
            lengthInc = 2f;
            fromColor = Pal.lancerLaser.cpy().lerp(Pal.sapBullet, 0.5f);
            toColor = Pal.sapBullet;
            hitMissed = true;
        }};

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

            lifetime = 6 * 60;
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

        fractalLaser = new ExpLaserFieldBulletType(170f, 130f){{
            damageInc = 6f;
            lengthInc = 2f;
            fields = 2;
            fieldInc = 0.15f;
            width = 2;
            expGain = buildingExpGain = 1;
            fromColor = Pal.lancerLaser.cpy().lerp(Pal.place, 0.5f);
            toColor = Pal.place;
            maxRange = 150f + 2f * 30f; //Account for range increase

            distField = UnityBullets.distField;
            smallDistField = UnityBullets.smallDistField;
        }};

        laserGeyser = new GeyserBulletType(){{
            damageInc = 2f;
        }};

        breakthroughLaser = new ExpLaserBlastBulletType(500f, 1200f){{
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
        }};

        coalBlaze = new ExpBulletType(3.35f, 32f){{
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
        }};

        pyraBlaze = new ExpBulletType(3.35f, 46f){{
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
        }};
    }
}
