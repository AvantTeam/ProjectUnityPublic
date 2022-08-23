package unity.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.graphics.*;
import unity.world.blocks.*;
import unity.world.blocks.power.FlyWheel.*;
import unity.world.graph.*;

import static mindustry.Vars.tilesize;

public class SteamPiston extends GenericGraphBlock{
    public float minTemp = HeatGraphNode.celsiusZero + 100;
    public float maxTemp = HeatGraphNode.celsiusZero + 400;
    TextureRegion[] sprite = new TextureRegion[4];
    TextureRegion base,liquid,arm,pivot,heat;
    int smokeTimer = timers++;

    public SteamPiston(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        base = Core.atlas.find(name+"-base");
        liquid = Core.atlas.find(name+"-liquid");
        arm = Core.atlas.find(name+"-arm");
        pivot = Core.atlas.find(name+"-pivot");
        for(int i  = 0;i<4;i++){
            sprite[i] = Core.atlas.find(name+"-rot"+(i+1));
        }
    }


    public class SteamPistonBuild extends GenericGraphBuild{
        FlyWheelBuild flywheel = null;
        Vec2 flywheeldir = new Vec2();
        boolean initConnect = false;
        float pushForce = 0;
        float rwater = 0;

        @Override
        public boolean shouldConsume(){
            return super.shouldConsume() &&  heatNode().getTemp()>minTemp;
        }

        @Override
        public void updateTile(){
            super.updateTile();
            if(!initConnect){
                tryConnect();
                initConnect = true;
            }
            if(flywheel ==null){
                 return;
            }
            var heatnode = heatNode();
            float temp = heatnode.getTemp();
            if(temp>minTemp){
                float eff = Mathf.clamp(Mathf.map(temp,minTemp,maxTemp,0,1));
                boolean pulling = flywheeldir.dot(flywheel.attachY-y,-(flywheel.attachX-x))>0;
                if(pulling ){
                    pushForce = 0;
                    if(timer(smokeTimer,5) && liquids.currentAmount()>1){
                        float rand = Mathf.random()>0.5f?-1:1;
                        Fx.fuelburn.at(x+flywheeldir.y*tilesize*rand,y-flywheeldir.x*tilesize*rand);
                    }
                }else{
                    if(rwater<=0 && canConsume()){
                        consume();
                        heatnode.addHeatEnergy(-eff*150);
                        rwater += 10;
                    }
                    if(rwater>0){
                        rwater -= eff* this.delta();
                        pushForce += (this.timeScale()*eff - pushForce) * 0.1f * this.delta();
                    }else{
                        pushForce = 0;
                    }
                }
            }
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();
            if(flywheel !=null){
                flywheel.connected.remove(this);
                setFlywheel(null);
            }
        }

        @Override
        public void onRemoved(){
            super.onRemoved();
            if(flywheel !=null){
                flywheel.connected.remove(this);
                setFlywheel(null);
            }
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();
            tryConnect();
        }
        public void tryConnect(){
            var fb = front();
            if(fb instanceof FlyWheelBuild fwb && (fb.x==x || fb.y==y)){
                if(flywheel !=fwb){
                    if(flywheel !=null){
                        flywheel.connected.remove(this);
                    }
                    setFlywheel(fwb);
                    flywheel.connected.add(this);
                }
            }else if(flywheel !=null){
                flywheel.connected.remove(this);
                setFlywheel(null);
            }
        }

        @Override
        public void draw(){
            float temp = heatNode().getTemp();
            Draw.rect(base,x,y,0);
            Drawf.liquid(liquid, x, y, liquids.currentAmount() / liquidCapacity, liquids.current().color);
            Draw.rect(sprite[rotation],x,y,0);
            drawTeamTop();

            if(flywheel !=null){
                float r = tilesize;
                float yd = flywheeldir.dot(flywheel.attachX-x,flywheel.attachY-y);
                float xd = flywheeldir.dot(flywheel.attachY-y,-(flywheel.attachX-x));
                boolean left = xd>0;
                xd = Math.abs(xd);
                float d = Math.max(0,yd-Mathf.sqrt(Math.max(0,r*r - xd*xd)));
                float px = x+flywheeldir.x*d;
                float py = y+flywheeldir.y*d;
                Draw.z(Layer.blockOver + 0.1f);
                Lines.stroke(4);
                Lines.line(arm,x+flywheeldir.x*10f,y+flywheeldir.y*10f,px,py,false);
                Lines.stroke(3);
                Lines.line(arm,flywheel.attachX,flywheel.attachY,px,py,false);
                Draw.rect(pivot,px,py,0);
                Draw.rect(pivot,flywheel.attachX,flywheel.attachY,0);
            }
        }

        public void setFlywheel(FlyWheelBuild flywheel){
            this.flywheel = flywheel;
            if(flywheel!=null){
                flywheeldir.set(flywheel.x-x,flywheel.y-y).nor();
            }
        }
    }
}
