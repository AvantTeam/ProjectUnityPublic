package unity.world.blocks.production;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.consumers.*;
import mindustry.world.modules.ItemModule.*;

import static arc.math.Mathf.pi;

public class BatchMixer extends GenericGraphCrafter{
    TextureRegion floor, base, spinner,top;
    @Nullable ConsumeItems consItems;

    public BatchMixer(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        floor = loadTex("floor");
        base = loadTex("base");
        top = loadTex("top");
        spinner = loadTex("spinner");
    }

    @Override
    public void init(){
        consItems = findConsumer(b -> b instanceof ConsumeItems);

        super.init();
    }

    public class BatchMixerBuild extends  GenericGraphCrafterBuild{
        Seq<ItemMixerParticle> particles = new Seq<>();

        @Override
        public void created(){
            super.created();
            particles = new Seq<>();
            for(int i =0;i<=50;i++){
                particles.add(new ItemMixerParticle());
            }
        }

        public class ItemMixerParticle{
            float r = Mathf.random(Mathf.PI2);
            float d = Mathf.lerp(1,8,Mathf.sqrt(Mathf.random()));
            float affect = Mathf.random(0.75f,1.0f);
            Item i;
            void update(float sr,float smul){
                r += (Math.max(0,Mathf.cos(Math.min(Math.abs(sr-r),Math.abs((sr+pi)%Mathf.PI2-r)) )) + 0.1)*smul*0.01*affect;
                r = r%(2*pi);
            }

            void draw(){
                if(i!=null){
                    Draw.rect(i.fullIcon,x + Mathf.cos(r)*d,y + Mathf.sin(r)*d, Vars.itemSize, Vars.itemSize);
                }
            }
        }

        public float saturation(){
            //ItemConsumer
            var itemsNeeded = consItems.items;
            float min = 1;
            float max = itemCapacity;
            for(var itemStack:itemsNeeded){
                max = Math.min(itemCapacity/(float)itemStack.amount,max);
            }
            for(var itemStack:itemsNeeded){
                min = Math.min(items.get(itemStack.item)/(max*itemStack.amount),min);
            }
            return Mathf.clamp(min);
        }

        @Override
        public void displayBars(Table table){
            super.displayBars(table);
            table.row();
            table.add(new Bar(() -> Core.bundle.format("bar.unity-craftspeed", Strings.fixed(efficiency()*100f, 1))+"%", () -> Pal.ammo, () -> Mathf.clamp(efficiency())));
        }

        @Override
        public float getProgressIncrease(float baseTime){
            return super.getProgressIncrease(baseTime) * Mathf.clamp((torqueNode().getGraph().lastVelocity / torqueNode().maxSpeed)) * saturation();
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(!Vars.headless){
                float r = (torqueNode().getGraph().rotation/10f) * Mathf.degreesToRadians;
                r = (r+pi)%(2*pi);
                for(var particle:particles){
                    particle.update(r, torqueNode().getGraph().lastVelocity/100f);
                }
                int total = items.total();
                if(total<=particles.size){
                    //total items less then particles, items get mapped 1:1 to particles
                    int[] index = {0};
                    items.each((item,a)->{
                        for(int i = 0; i < a; i++){
                            particles.get(index[0]).i = item;
                            index[0]++;
                        }
                    });
                    for(int i = 0; i < particles.size; i++){
                        if(i > total){
                            particles.get(i).i = null;
                        }
                    }
                }else{
                    float ratio = particles.size/(float)total;
                    int lratio = Mathf.floor(1f/ratio);
                    float[] index = {0};
                    items.each((item,a)->{
                        for(int i = 0; i < a; i+=lratio){
                            particles.get(Mathf.floor(index[0])).i = item;
                            index[0]+=ratio*lratio;
                            index[0] = Math.min(index[0],particles.size-1);
                        }
                    });
                }
            }
        }

        @Override
        public void draw(){
            float r = torqueNode().getGraph().rotation;
            Draw.rect(floor,x,y);
            for(var particle:particles){
                particle.draw();
            }
            Draw.rect(base,x,y);
            Draw.rect(spinner,x,y,r/10f);
            Draw.rect(top,x,y);
            drawTeamTop();
        }
    }
}
