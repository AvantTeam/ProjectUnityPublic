package unity.world.blocks.distribution;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import unity.gen.graph.*;
import unity.util.*;
import unity.world.graph.connectors.GraphConnectorTypeI.*;
import unity.world.graph.nodes.*;

import static mindustry.Vars.*;

public class HeatPipe extends HeatBlock{
    public static final Color baseColor = Color.valueOf("6e7080");
    protected static Color tempCol = new Color();
    protected static final int[] shift = new int[]{0, 3, 2, 1};

    public float damageMul = 0.5f;
    public TextureRegion[] heatRegions, regions;

    public HeatPipe(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        heatRegions = DrawUtils.getRegions(Core.atlas.find(name + "-heat"), 8, 2);
        regions = DrawUtils.getRegions(Core.atlas.find(name + "-tiles"), 8, 2);
    }

    @Override
    public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
        float scl = tilesize * req.animScale;
        int spriteIndex = 0;
        for(int i = 0; i < 4; i++){
            var pt = Geometry.d4((4 - i) % 4).cpy().add(req.x, req.y);
            if(world.build(pt.x, pt.y) instanceof HeatPipeBuild){
                spriteIndex += 1 << i;
            }else{
                boolean[] f = {false};
                list.each(plan -> {
                    if(!f[0] && plan.x == pt.x && plan.y == pt.y){
                        f[0] = true;
                    }
                });

                if(f[0]) spriteIndex += 1 << i;
            }
        }

        Draw.rect(regions[spriteIndex], req.drawx(), req.drawy(), scl, scl);
    }

    public class HeatPipeBuild extends HeatBuild{
        int spriteIndex;

        @Override
        public void onConnectionChanged(GraphConnectorI<?> g){
            spriteIndex = 0;
            for(int i = 0; i < 4; i++){
                if(nearby((4 - i) % 4) instanceof GraphBuild gbuild){
                    if(!g.isConnected((Building & GraphBuild)gbuild)) continue;
                    spriteIndex += 1 << i;
                }
            }
        }

        @Override
        public void unitOn(Unit unit){
            float temp = heatNode.temperature();
            if(timer(timerDump, dumpTime)){
                heatNode.affectUnit(unit, damageMul);
            }
        }

        @Override
        public void draw(){
            float temp = heatNode.temperature();
            Draw.rect(regions[spriteIndex], x, y);
            if(temp < HeatNodeType.celsiusZero || temp > HeatNodeType.celsiusZero + 150f){
                heatNode.heatColor(tempCol);
                Draw.color(tempCol.add(baseColor));
                Draw.rect(heatRegions[spriteIndex], x, y);
                Draw.color();
            }

            drawTeamTop();
        }
    }
}
