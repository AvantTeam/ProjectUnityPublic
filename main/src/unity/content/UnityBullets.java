package unity.content;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.content.fx.*;
import unity.entities.bullet.energy.*;

public class UnityBullets{
    public static BulletType
    basicMissile,

    citadelFlame,

    sapLaser, sapArtilleryFrag, continuousSapLaser;

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
    }
}
