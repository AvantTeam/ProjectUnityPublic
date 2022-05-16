package unity.world.blocks.exp;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import unity.graphics.*;

import static arc.math.geom.Geometry.*;
import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class DiagonalTower extends ExpTower{
    public DiagonalTower(String name){
        super(name);
        configurable = saveConfig = true;
        lastConfig = false;

        config(Boolean.class, (DiagonalTowerBuild build, Boolean value) -> {
            build.diagonal = value;
        });

        config(Integer.class, (DiagonalTowerBuild build, Integer value) -> {
            if(value < 0) return;
            value %= 8;
            build.diagonal = value % 2 == 1;
            build.rotation = value / 2;
        });
    }

    @Override
    public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(topRegion, req.drawx(), req.drawy());
        drawPlanConfig(req, list);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        //nope
    }

    @Override
    public void drawPlanConfig(BuildPlan req, Eachable<BuildPlan> list){
        if(!req.worldContext) return;
        Draw.rect(region, req.drawx(), req.drawy(), req.rotation * 90 - 90 + (req.config instanceof Boolean b && b ? 45 : 0));
        Draw.mixcol();
        if(req.config instanceof Boolean b){
            if(b){
                int dx = d8edge(req.rotation).x, dy = d8edge(req.rotation).y;

                Drawf.dashLine(UnityPal.exp,
                        req.x * tilesize + dx * (tilesize / 2f + 2),
                        req.y * tilesize + dy * (tilesize / 2f + 2),
                        req.x * tilesize + dx * range * tilesize,
                        req.y * tilesize + dy * range * tilesize
                );
            }
            else drawPlaceDash(req.x, req.y, req.rotation);
        }
    }

    public class DiagonalTowerBuild extends ExpTowerBuild{
        public boolean diagonal = false;

        @Override
        public void drawSelectDash(){
            if(!diagonal){
                super.drawSelectDash();
                return;
            }
            int dx = d8edge(rotation).x, dy = d8edge(rotation).y;

            Drawf.dashLine(UnityPal.exp,
                    x + dx * (tilesize / 2f + 2),
                    y + dy * (tilesize / 2f + 2),
                    x + dx * range * tilesize,
                    y + dy * range * tilesize
            );
        }

        @Override
        public float laserRotation(){
            return diagonal ? rotdeg() + 45f : rotdeg();
        }

        @Override
        public int shootExp(int amount){
            if(!diagonal) return super.shootExp(amount);

            for(int i = 1; i <= range; i++){
                Tile t = world.tile(tile.x + d8edge(rotation).x * i, tile.y + d8edge(rotation).y * i);
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

        public int rotint(){
            return rotation * 2 + (diagonal ? 1 : 0);
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.undo, Styles.cleari, () -> {
                configure(rotint() + 1);
            }).size(40);
            table.image().color(Pal.gray).size(4, 40).pad(0);
            table.button(Icon.redo, Styles.cleari, () -> {
                configure(rotint() + 7);
            }).size(40);
        }

        @Override
        public Object config(){
            return diagonal;
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            diagonal = read.bool();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(diagonal);
        }
    }
}
