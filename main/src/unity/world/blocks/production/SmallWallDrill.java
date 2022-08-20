package unity.world.blocks.production;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.content.effects.*;
import unity.util.*;
import unity.world.graph.*;


import static mindustry.Vars.tilesize;

public class SmallWallDrill extends GenericTorqueWallDrill{
    TextureRegion base[];
    TextureRegion floor,rotator,armbase,arm,bore;
    int hitTimer = timers++;
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

    @Override
    public void init(){
        super.init();
    }

    public class SmallWallDrillBuild extends GenericTorqueWallDrillBuild{

        @Override
        public void updateTile(){
            super.updateTile();
            float eff = torqueEfficiency();
            if(Mathf.chanceDelta(0.02*eff)){
                updateEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
            }
            if(Mathf.chanceDelta(0.3*eff)){
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
                    OtherFx.dust.at(rx + Mathf.range(1), ry + Mathf.range(1), 0, col, new Vec2(vx * 14.0f * eff, vy * 14.0f * eff));
                }
            }
            var tnode = torqueNode();
            tnode.baseForce = 0;
            float spd = torqueNode().getGraph().lastVelocity;
            if(tilesDrilling == 0 && timer(hitTimer,10) && spd>5){
                float ang = targetDrillAngle + rotdeg()+ 90;
                float s2 = size*0.5f;
                float vx = -Mathf.cosDeg(ang)+Mathf.range(0.5f);
                float vy = -Mathf.sinDeg(ang)+Mathf.range(0.5f);
                float rx = x + Geometry.d4x(rotation) * (s2+targetDrillExtend+0.5f) * tilesize, ry = y + Geometry.d4y(rotation) * (s2+targetDrillExtend+0.5f) * tilesize;
                s2 = size*4;
                Utils.collideLineRawEnemy(team, rx-vx*s2, ry-vy*s2, rx+vx*s2, ry+vy*s2, 4, true, true, true, (x, y, h, direct) -> {
                    float t = Mathf.clamp(spd,0,100);
                    float p = h.health();
                    float ratio = Math.min(p,t)/h.maxHealth();
                    if(h instanceof Unit unit){
                        if(unit.isFlying()){
                            return false;
                        }
                    }
                    h.damage(t);
                    ratio*=0.1;
                    if(h instanceof Unit unit){
                        var req= unit.type.getTotalRequirements();
                        for(var stack:req){
                            float am = stack.amount*ratio;
                            if(am>=1 || Mathf.random()<am){
                                items.add(stack.item,Math.max(1,Mathf.floor(am)));
                                lastItem = stack.item;
                            }
                        }
                    }
                    tnode.baseForce -= t;

                    Fx.hitBulletSmall.at(h.x(),h.y());
                    return true;
                });
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
            if(lastItem!=null){
                float progress = time/drillTime;
                Draw.rect(
                    lastItem.fullIcon,
                    Mathf.lerp(rx,x + Geometry.d4x(rotation) * (s2*0.5f) * tilesize,progress),
                    Mathf.lerp(ry,y + Geometry.d4y(rotation) * (s2*0.5f) * tilesize,progress),
                    Vars.itemSize,
                    Vars.itemSize
                );
            }

            Draw.rect(base[rotation],x,y);
            Drawf.spinSprite(rotator,x,y,r*0.2f);
            Draw.z(Layer.blockOver);
            Draw.rect(bore,rx + Mathf.sinDeg(-ang)*s2*4f,ry + Mathf.cosDeg(ang)*s2*4f,-r*0.2f);
            Draw.rect(bore,rx - Mathf.sinDeg(-ang)*s2*4f,ry - Mathf.cosDeg(ang)*s2*4f,r*0.2f);
            Draw.rect(arm,rx,ry,ang);

            drawTeamTop();
        }
    }
}
