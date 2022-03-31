package unity.world.blocks.production;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.payloads.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;
import unity.world.meta.*;

import static mindustry.Vars.*;

public class CrucibleBlock extends GenericGraphBlock{
    TextureRegion floor,liquid, heat,chunks;
    TextureRegion[] base,baseopen;
    Vec2[] pos;
    public CrucibleBlock(String name){
        super(name);
        sync = true;
        //cant accept liquids just yet...
    }

    @Override
    public void load(){
        super.load();
        floor = loadTex("floor");
        heat = loadTex("heat");
        liquid = loadTex("liquid");
        chunks = loadTex("solid");

        base = new TextureRegion[2];
        base[0] = loadTex("base1");
        base[1] = loadTex("base2");

        baseopen = new TextureRegion[2];
        baseopen[0] = loadTex("base-open1");
        baseopen[1] = loadTex("base-open2");

        pos = new Vec2[30];
        for(int i =0;i<pos.length;i++){
            pos[i] = new Vec2(Mathf.range(size*tilesize*0.5f*0.25f),Mathf.range(size*tilesize*0.5f*0.25f));
        }
    }

    public class CrucibleBuild extends GenericGraphBuild{
        protected static final Rand rand = new Rand();
        @Override
        public boolean acceptItem(Building source, Item item){
            var crucible = crucibleNode();
            var ingre = CrucibleRecipes.items.get(item);
            if( ingre!=null && crucible.capacity - crucible.getFluid(ingre).total() >= 1){
                return true;
            }
            return false;
        }

        @Override
        public void handleItem(Building source, Item item){
            var crucible = crucibleNode();
            crucible.addItem(item,1);
        }


        boolean[] connection = new boolean[4];

        @Override
        public void onConnectionChanged(GraphConnector g){
            for(int i = 0;i<4;i++){
                var build = nearby(Geometry.d4x(i)*2,Geometry.d4y(i)*2);
                connection[i] = build instanceof GraphBuild gb && crucibleNode().connector.get(0).isConnected(gb);
            }
        }

        @Override
        public void draw(){
            var crucible = crucibleNode();
            Color liquidColor = crucible.getColor();
            Draw.rect(floor,x,y);
            if(liquidColor.a>0.01){
                Draw.color(liquidColor);
                Draw.rect(liquid, x, y);
                float scl = 30f, mag = 0.2f;
                Draw.rectv(liquid,x,y,20,20,vec -> vec.add(
                Mathf.sin(vec.y*3 + Time.time, scl, mag) + Mathf.sin(vec.x*3 - Time.time, 70, 0.8f),
                Mathf.cos(vec.x*3 + Time.time + 8, scl + 6f, mag * 1.1f) + Mathf.sin(vec.y*3 - Time.time, 50, 0.2f)
                ));
                Draw.alpha(Mathf.curve(Mathf.sinDeg(Time.time*0.3f),-1,1));
                Draw.rectv(liquid,x,y,20,20,90,vec -> vec.add(
                Mathf.sin(vec.y*3 + Time.time, scl, mag) + Mathf.sin(vec.x*3 - Time.time, 70, 0.8f),
                Mathf.cos(vec.x*3 + Time.time + 8, scl + 6f, mag * 1.1f) + Mathf.sin(vec.y*3 - Time.time, 50, 0.2f)
                ));

                Draw.color(liquidColor, 0.5f);
                rand.setSeed(pos());
                for(int i = 0; i < 20; i++){
                    float x = rand.range(8), y = rand.range(8);
                    float life = 1f - ((Time.time / 20f + rand.random(6)) % 6);

                    if(life > 0){
                        Lines.stroke((life + 0.2f));
                        Lines.poly(this.x + x, this.y + y, 8, (1f - life) * 3);
                    }
                }
                Draw.color();
            }

            float total = 0;
            for(var fentry: crucible.fluids){
                total += fentry.value.solid;
            }
            int am = (int)Math.min(pos.length,total);
            if(total>0){
                int i =0;
                for(var fentry: crucible.fluids){
                    float pieces = am*fentry.value.solid/total;
                    Draw.color(fentry.key.color);
                    for(int a=0;a<(int)pieces;a++){
                        Draw.rect(chunks,x+pos[i].x,y+pos[i].y,itemSize,itemSize);
                        i++;
                    }
                }
            }
            Draw.color();
            for(int i = 0;i<4;i++){
                if(connection[i]){
                    Draw.rect(baseopen[i==2||i==3? 0 : 1],x,y,180 + i*90);
                }else{
                    Draw.rect(base[i==2||i==3? 0 : 1],x,y,180 + i*90);
                }
            }

            UnityDrawf.drawHeat(heat,x,y,0,heatNode().getTemp());
            drawTeamTop();


        }
    }
}
