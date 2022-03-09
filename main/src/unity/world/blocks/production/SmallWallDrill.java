package unity.world.blocks.production;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.content.effects.*;
import unity.world.graph.*;


import static mindustry.Vars.tilesize;

public class SmallWallDrill extends GenericTorqueWallDrill{
    TextureRegion base[];
    TextureRegion floor,rotator,armbase,arm,bore;
    public SmallWallDrill(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        base = new TextureRegion[4];
        for(int i = 0;i<4;i++)
            base[i] = loadTex(""+(i+1));
        floor = loadTex("base");
        rotator = loadTex("top");
        arm = loadTex("arm");
        bore = loadTex("bore");
        armbase = loadTex("armbase");
    }

    public class SmallWallDrillBuild extends GenericTorqueWallDrillBuild{


        @Override
        public void updateTile(){
            super.updateTile();
            if(Mathf.chanceDelta(0.02*efficiency())){
                updateEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
            }
            if(Mathf.chanceDelta(0.5*efficiency())){
                float s2 = size*0.5f;
                float ang = targetDrillAngle + rotdeg();
                float vx = -Mathf.cos(ang)+Mathf.range(0.5f);
                float vy = -Mathf.sin(ang)+Mathf.range(0.5f);
                float rx = x + Geometry.d4x(rotation) * (s2+targetDrillExtend+0.5f) * tilesize, ry = y + Geometry.d4y(rotation) * (s2+targetDrillExtend+0.5f) * tilesize;
                Color col = tile.floor().mapColor;
                if(lastItem!=null){
                    col = col.cpy().lerp(lastItem.color,Mathf.random());
                }
                if(Mathf.chance(0.1)){
                    updateEffect.at(rx + Mathf.range(1), ry + Mathf.range(1), 0, col);
                }else{
                    OtherFx.dust.at(rx + Mathf.range(1), ry + Mathf.range(1), 0, col, new Vec2(vx * 8.0f, vy * 8.0f));
                }
            }
            //Effect

        }

        @Override
        public void draw(){
            float r  = getGraph(TorqueGraph.class).rotation;
            float s2 = size*0.5f;
            float rx = x + Geometry.d4x(rotation) * (s2+drillExtend) * tilesize, ry = y + Geometry.d4y(rotation) * (s2+drillExtend) * tilesize;
            float ang = drillAngle + rotdeg();
            Draw.rect(floor,x,y);
            Lines.stroke(3);
            Lines.line(armbase,x + Geometry.d4x(rotation) * (s2*0.5f) * tilesize,y + Geometry.d4y(rotation) * (s2*0.5f) * tilesize,rx,ry,false);
            Draw.rect(base[rotation],x,y);
            Drawf.spinSprite(rotator,x,y,r*0.2f);
            Draw.z(Layer.blockOver);
            Draw.rect(bore,rx + Mathf.sinDeg(-ang)*s2*4f,ry + Mathf.cosDeg(ang)*s2*4f,-r*0.2f);
            Draw.rect(bore,rx - Mathf.sinDeg(-ang)*s2*4f,ry - Mathf.cosDeg(ang)*s2*4f,r*0.2f);
            Draw.rect(arm,rx,ry,ang);
            Draw.z();
            drawTeamTop();
        }
    }
}
