package unity.world.blocks.distribution;


import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import unity.util.*;
import unity.world.blocks.*;
import unity.world.graph.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class HeatPipe extends GenericGraphBlock{
    public float damagemul = 2.5f;
    public final static Color baseColor = Color.valueOf("6e7080");
    final static int[] shift = new int[]{0, 3, 2, 1};
    protected static Color tempCol = new Color();
    TextureRegion[] heatRegions, regions;//bottom

    public HeatPipe(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        heatRegions = GraphicUtils.getRegions(atlas.find(name + "-heat"), 8, 2);//todo
        regions = GraphicUtils.getRegions(atlas.find(name + "-tiles"), 8, 2);
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        float scl = tilesize * req.animScale;
        int spriteIndex = 0;
        for(int i =0;i<4;i++){
            var pt = Geometry.d4((4-i)%4).cpy().add(req.x,req.y);
            if(world.build(pt.x,pt.y) instanceof HeatPipeBuild){
                spriteIndex += 1 << i;
            }else{
                final boolean[] f ={false};
                list.each(plan->{
                    if(!f[0] && plan.x == pt.x && plan.y == pt.y){
                        f[0] = true;
                    }
                });
                if(f[0]){
                    spriteIndex += 1 << i;
                }
            }
        }
        Draw.rect(regions[spriteIndex], req.drawx(), req.drawy(), scl, scl);
    }

    public class HeatPipeBuild extends GenericGraphBuild{
        int spriteIndex;

        public void onConnectionChanged(GraphConnector g){
            spriteIndex = 0;
            for(int i =0;i<4;i++){
                if(nearby((4-i)%4) instanceof GraphBuild gbuild){
                    if(!g.isConnected(gbuild)){continue;}
                    spriteIndex += 1 << i;
                }
            }
        }

        @Override
        public void unitOn(Unit unit){
            float temp = heatNode().getTemp();
            if(timer(timerDump, dumpTime)){
                if(temp > HeatGraphNode.celsiusZero+150){
                    float intensity = Mathf.clamp(Mathf.map(temp, HeatGraphNode.celsiusZero + 400, HeatGraphNode.celsiusZero + 2000f, 0f, 1f));
                    unit.apply(StatusEffects.burning, (intensity * 40f + 7f) * damagemul);
                    if(unit.isImmune(StatusEffects.burning)){
                        intensity*=0.2;
                    }
                    if(unit.isImmune(StatusEffects.melting)){
                        intensity*=0.2;
                    }
                    unit.damage(intensity * 50f * damagemul);
                }else if(temp < HeatGraphNode.celsiusZero-100){
                    float intensity = Mathf.clamp(Mathf.map(temp, HeatGraphNode.celsiusZero - 100, 0, 0f, 1f));
                    unit.apply(StatusEffects.freezing, (intensity * 40f + 7f) * damagemul);
                    if(unit.isImmune(StatusEffects.freezing)){
                        intensity*=0.2;
                    }
                    if(unit.hasEffect(StatusEffects.wet)){
                        intensity*=2;
                        unit.apply(StatusEffects.slow, (intensity * 20f + 7f) * damagemul);
                    }
                    unit.damage(intensity * 50f * damagemul);
                }
            }
        }

        @Override
        public void draw(){
            float temp = heatNode().getTemp();
            Draw.rect(regions[spriteIndex], x, y);
            if(temp < HeatGraphNode.celsiusZero || temp > HeatGraphNode.celsiusZero+150){
                heatNode().heatColor(tempCol);
                Draw.color(tempCol.add(baseColor));
                Draw.rect(heatRegions[spriteIndex], x, y);
                Draw.color();
            }
            drawTeamTop();
        }
    }
}
