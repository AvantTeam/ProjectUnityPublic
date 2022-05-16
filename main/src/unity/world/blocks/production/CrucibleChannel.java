package unity.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.units.*;
import unity.util.*;
import unity.world.blocks.*;
import unity.world.blocks.distribution.HeatPipe.*;
import unity.world.graph.*;

import static arc.Core.atlas;
import static mindustry.Vars.*;

public class CrucibleChannel extends GenericGraphBlock{
    TextureRegion[] regions;
    TextureRegion floor;
    public CrucibleChannel(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        regions = GraphicUtils.getRegions(atlas.find(name + "-tiles"), 8, 2);
        floor = loadTex("floor");
    }

    @Override
    public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list){
        float scl = tilesize * req.animScale;
        int spriteIndex = 0;
        for(int i =0;i<4;i++){
            var pt = Geometry.d4((4-i)%4).cpy().add(req.x,req.y);
            if(world.build(pt.x,pt.y) instanceof CrucibleChannelBuild){
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

    public class CrucibleChannelBuild extends GenericGraphBuild{
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
        public void draw(){
            Draw.color();
            Draw.rect(floor,x,y);
            Draw.color(crucibleNode().getColor());
            Fill.rect(x,y,tilesize,tilesize);
            Draw.color();
            Draw.rect(regions[spriteIndex], x, y);
            drawTeamTop();
        }
    }
}
