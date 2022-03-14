package unity.world.blocks.production;

import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import unity.world.blocks.*;
import unity.world.graph.*;

import static mindustry.Vars.content;

public abstract class GenericCaster extends GenericGraphBlock{

    public float castTime = 30f;
    public float moveTime = 20f;

    public GenericCaster(String name){
        super(name);
    }

    public abstract class GenericCasterBuild extends GenericGraphBuild{
        public float progress;

        public abstract boolean isCasting();
        public abstract void tryStartCast();
        public abstract void offloadCast();
        public abstract void resetCast();
        public abstract boolean canOffloadCast();

        @Override
        public void updateTile(){
            super.updateTile();
            var torque = getGraph(TorqueGraph.class);
            var torqueNode = torqueNode();

            if(!isCasting()){
                tryStartCast();
            }else{
                if(progress<castTime){
                    progress += Time.delta;
                    if(progress>castTime){
                        float f = progress-castTime;
                        progress-=f;
                        progress+=f* Mathf.curve(torque.lastVelocity,0,torqueNode.maxSpeed);
                    }
                }else{
                    progress+=Time.delta * Mathf.curve(torque.lastVelocity,0,torqueNode.maxSpeed);
                        if(progress>=castTime+moveTime){
                        if(canOffloadCast()){
                            offloadCast();
                            resetCast();
                            progress = 0;
                        }else{
                            progress = castTime+moveTime;
                        }
                    }
                }
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
        }

    }
}
