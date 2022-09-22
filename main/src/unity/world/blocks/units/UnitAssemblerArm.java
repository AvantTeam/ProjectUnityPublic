package unity.world.blocks.units;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import unity.net.*;
import unity.util.GraphicUtils.*;
import unity.world.blocks.*;
import unity.world.blocks.units.ModularUnitAssembler.*;

import static unity.content.effects.OtherFx.weldspark;

public class UnitAssemblerArm extends GenericGraphBlock{
    public float armfoldsize = 8;
    public float constructSpeed = 1.0f;
    TextureRegion arm[] = new TextureRegion[3];
    TextureRegion base[] = new TextureRegion[4];
    TextureRegion spinner;
    public UnitAssemblerArm(String name){
        super(name);
        acceptsItems = true;
        hasItems = true;
        sync = true;
    }

    @Override
    public void load(){
        super.load();
        arm[0] = loadTex("end");
        arm[1] = loadTex("mid");
        arm[2] = loadTex("end");
        for(int i = 0;i<4;i++){
            base[i] = loadTex("base"+(i+1));
        }
        spinner = loadTex("spinner");
    }

    public class UnitAssemblerArmBuild extends GenericGraphBuild{
        public ModuleConstructing currentJob;
        public ModularUnitAssemblerBuild attached = null;
        ZipperArm arm;
        public float buildProgress = 0;

        @Override
        public void onInit(){
            super.onInit();

            arm = new ZipperArm(0,0,armfoldsize,armfoldsize,armfoldsize*4,2);
        }

        @Override
        public void onPlaced(){
            super.onPlaced();
            tryConnect();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            if(attached==null){
                return false;
            }
            var itemreq = attached.blueprint.itemRequirements();
            for(var itemstack: itemreq){
                if(itemstack.item==item){
                    return items.get(item)<itemCapacity;
                }
            }
            return false;
        }


        @Override
        public void updateTile(){
            super.updateTile();
            if(!enabled){
                buildProgress = 0;
                if(currentJob!=null){
                    currentJob.takenby = -1;
                    currentJob = null;
                }
                return;
            }
            Item usedItem = null;
            if(attached!=null){
                if(currentJob==null){
                    items.each((item, am) -> {
                        if(currentJob!=null){
                            return;
                        }
                        currentJob = attached.getJob(this,item);
                    });
                }else{
                    buildProgress += edelta()*constructSpeed*torqueEfficiency();
                    boolean found = false;

                    for(var is:currentJob.remaining){
                        if(items.has(is.item) && is.amount>0){
                            found = true;
                            if(buildProgress<is.item.cost){
                                continue;
                            }
                            if(attached.constructModule(currentJob, is.item)){
                                items.remove(is.item, 1);
                                buildProgress -= is.item.cost;
                                usedItem = is.item;
                                break;
                            }
                        }
                    }
                    if(currentJob!=null && !Vars.net.client() && currentJob.isComplete()){
                        UnityCalls.moduleComplete(attached,this,currentJob);
                        currentJob.takenby = -1;
                        currentJob = null;
                        buildProgress = 0;
                    }else if(!found && currentJob!=null){
                        currentJob.takenby = -1;
                        currentJob = null;
                        buildProgress = 0;
                    }


                }
            }
            if(!Vars.net.server()){
                if(currentJob!=null){

                    var r =  attached.modulePos(currentJob.x+currentJob.type.w*0.5f, currentJob.y+currentJob.type.h*0.5f);
                    arm.end.lerp(r.x-x,r.y-y,0.1f);
                    if(usedItem!=null){
                        weldspark.at(r.x,r.y,currentJob.x*69,usedItem.color);
                    }
                }else{
                    arm.end.lerp(0.1f,0.1f,0.1f);
                }
                arm.update();
            }
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
            if( currentJob!=null){
                currentJob.takenby = -1;
                currentJob = null;
                buildProgress = 0;
            }
        }

        @Override
        public void draw(){
            Draw.rect(base[rotation],x,y);
            Drawf.spinSprite(spinner,x,y,Mathf.radiansToDegrees * Mathf.atan2(arm.jointPositions[0].x-arm.start.x,arm.jointPositions[0].y-arm.start.y));
            Draw.z(Layer.power-5);
            arm.draw((vec1, vec2, i) -> {
                var reg = UnitAssemblerArm.this.arm[i];
                Lines.stroke(reg.height*0.25f,Pal.shadow);
                Lines.line(reg,x+vec1.x - i*2,y+vec1.y - i*2,x+vec2.x- i*2-2,y+vec2.y- i*2-2,i==1);
            });
            Draw.color();
            arm.draw((vec1, vec2, integer) -> {
                var reg = UnitAssemblerArm.this.arm[integer];
                Lines.stroke(reg.height*0.25f);
                Lines.line(reg,x+vec1.x,y+vec1.y,x+vec2.x,y+vec2.y,integer==1);
            });
            drawTeamTop();
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            tryConnect();
        }

        public void tryConnect(){
            var fb = front();
            if(fb instanceof ModularUnitAssemblerBuild fwb && (fb.x==x || fb.y==y) && !fwb.isSandbox()){
                if(attached !=fwb){
                    attached = fwb;
                }
            }else if(attached !=null){
                attached = null;
                if(currentJob!=null){
                    currentJob = null;
                }
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(buildProgress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            buildProgress = read.f();
        }
    }
}
