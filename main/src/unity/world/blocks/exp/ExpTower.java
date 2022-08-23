package unity.world.blocks.exp;

import arc.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.graphics.*;

import static arc.Core.atlas;
import static arc.math.geom.Geometry.d4x;
import static arc.math.geom.Geometry.d4y;
import static mindustry.Vars.*;

public class ExpTower extends ExpTank {
    public int range = 5;
    public float reloadTime = 10f;
    public float manualReload = 20f;
    public boolean buffer = false;
    public int bufferExp = 20;

    public float laserWidth = 0.5f;
    public TextureRegion laser, laserEnd;
    public float elevation = -1f;

    public Sound shootSound = Sounds.plasmadrop;
    public float shootSoundVolume = 0.05f;

    public ExpTower(String name){
        super(name);
        rotate = true;
        outlineIcon = true;
        drawArrow = false;
        noUpdateDisabled = false;
    }

    @Override
    public void init(){
        super.init();
        if(elevation < 0) elevation = size / 2f;
    }

    @Override
    public void load(){
        super.load();
        laser = atlas.find(name + "laser", "unity-exp-laser");
        laserEnd = atlas.find(name + "laser-end", "unity-exp-laser-end");
        topRegion = atlas.find(name + "-base", "block-"+size); //topRegion serves as the base region
    }

    @Override
    public void setStats(){
        super.setStats();
        if(buffer){
            stats.add(Stat.output, "@ [lightgray]@[]", Core.bundle.format("explib.expAmount", (bufferExp / manualReload) * 60), StatUnit.perSecond.localized());
        }
    }

    public void drawPlaceDash(int x, int y, int rotation){
        int dx = d4x(rotation), dy = d4y(rotation);

        Drawf.dashLine(UnityPal.exp,
                x * tilesize + dx * (tilesize / 2f + 2),
                y * tilesize + dy * (tilesize / 2f + 2),
                x * tilesize + dx * range * tilesize,
                y * tilesize + dy * range * tilesize
        );
    }

    @Override
    public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(topRegion, req.drawx(), req.drawy());
        Draw.rect(region, req.drawx(), req.drawy(), req.rotation * 90 - 90);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        drawPlaceDash(x, y, rotation);
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{topRegion, region}; //cursed
    }

    public class ExpTowerBuild extends ExpTankBuild {
        public float reload = 0f;
        protected Tile lastTarget = null;
        private float heat = 0f;
        private int lastSent = 0;

        @Override
        public int unloadExp(int amount){
            return 0; //nope
        }

        @Override
        public int handleTower(int amount, float angle){
            if(buffer && exp > 0) return 0;
            int a = handleExp(amount);
            if(a > 0 && reload >= reloadTime && !Angles.near(angle + 180, laserRotation(), 1f)) shoot();
            return a;
        }

        @Override
        public boolean handleOrb(int orbExp){
            int a = handleExp(orbExp);
            if(a <= 0) return false;
            if(reload >= manualReload) shoot();
            return true;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(!buffer || exp > 0) reload += delta();
            else reload = 0;
            if(heat > 0) heat -= delta();

            if(reload >= manualReload && exp > 0) shoot();
        }

        @Override
        public void draw(){
            Draw.rect(topRegion, x, y);
            Draw.color();

            Tmp.v1.trns(laserRotation(), heat / manualReload);
            Draw.z(Layer.turret);
            Drawf.shadow(region, x - elevation, y - elevation, laserRotation() - 90);
            Draw.rect(region, x - Tmp.v1.x, y - Tmp.v1.y, laserRotation() - 90);

            drawLaser();
        }

        @Override
        public void drawSelect(){
            drawSelectDash();
            super.drawSelect();
        }

        public void drawSelectDash(){
            int dx = d4x(rotation), dy = d4y(rotation);

            Drawf.dashLine(UnityPal.exp,
                    x + dx * (tilesize / 2f + 2),
                    y + dy * (tilesize / 2f + 2),
                    x + dx * range * tilesize,
                    y + dy * range * tilesize
            );
        }

        public float laserRotation(){
            return rotdeg();
        }

        public void drawLaser(){
            if(heat <= 0 || lastSent == 0 || lastTarget == null) return;
            Draw.z(Layer.bullet + 1f);
            float fout = heat / manualReload;
            float f = lastSent / (float)expCapacity;
            f = (0.7f + 0.3f * f) * laserWidth;

            Tmp.v2.set(lastTarget.worldx(), lastTarget.worldy());
            Tmp.v1.set(Tmp.v2).sub(this).nor().scl(size * tilesize / 2f);
            Tmp.v2.sub(Tmp.v1);
            Tmp.v1.add(this);
            Lines.stroke(f * fout);
            Drawf.laser(laser, laserEnd, Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, f * fout);
            Draw.reset();
        }

        public void shoot(){
            if(!enabled) return;
            reload = 0;
            if(exp <= 0) return;

            int a = shootExp(buffer ? Math.min(exp, bufferExp) : exp);
            if(a > 0){
                exp -= a;
                heat = manualReload;
                lastSent = a;
                shootSound.at(x, y, 1f, shootSoundVolume);
            }
        }

        public int shootExp(int amount){
            for(int i = 1; i <= range; i++){
                Tile t = world.tile(tile.x + d4x(rotation) * i, tile.y + d4y(rotation) * i);
                if(t != null && t.build instanceof ExpHolder exp){
                    int a = exp.handleTower(amount, laserRotation());
                    if(a > 0){
                        lastTarget = t;
                        return a;
                    }
                }
            }

            return 0;
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            reload = read.f();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(reload);
        }
    }
}
