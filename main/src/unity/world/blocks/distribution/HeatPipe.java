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
    public float damagemul = 0.5f;
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
    public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
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
                heatNode().affectUnit(unit,damagemul);
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
