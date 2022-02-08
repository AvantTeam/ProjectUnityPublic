package unity.world.graph;

import arc.util.*;
import arc.util.io.*;

//rotGraph
public class TorqueGraph extends Graph<TorqueGraph>{
    public float lastInertia, lastGrossForceApplied, lastNetForceApplied, lastVelocity, lastFrictionCoefficient;
    public float rotation = 0;
    @Override
    public TorqueGraph copy(){
        var t = new TorqueGraph();
        t.lastVelocity = lastVelocity;
        t.rotation = rotation;
        return t;
    }

    @Override public void onMergeBegin(TorqueGraph graph){
        float momentumA = lastVelocity * lastInertia;
        float mementumB = graph.lastVelocity * graph.lastInertia;
        lastVelocity = (momentumA + mementumB) / (lastInertia + graph.lastInertia);
    }

    @Override
    public void authoritativeOverride(TorqueGraph g){
        g.lastVelocity = lastVelocity;
        g.rotation = rotation;
    }

    @Override
    public boolean canConnect(GraphConnector v1, GraphConnector v2){
        return v1.getNode().build().team ==  v2.getNode().build().team;
    }

    @Override
    public void onGraphChanged(){
        refreshGraphStats();
    }


    public void refreshGraphStats(){
        float forceApply = 0f;
        float fricCoeff = 0f;
        float iner = 0f;
        for(var module : vertexes){//building, GraphTorqueModule
            if(module.getNode() instanceof TorqueGraphNode torqueNode){
                forceApply += torqueNode.getForce();
                fricCoeff += torqueNode.getFriction();
                iner += torqueNode.getInertia();
            }
        }
        lastFrictionCoefficient = fricCoeff;
        lastGrossForceApplied = forceApply;
        lastInertia = iner;
    }

    ///this is on vertex added...
    @Override
    public void onUpdate(){
        refreshGraphStats();
        float netForce = lastGrossForceApplied;
        netForce -= lastFrictionCoefficient;
        netForce -= lastFrictionCoefficient*lastVelocity*0.15;
        lastNetForceApplied = netForce;
        float acceleration = lastInertia == 0f ? 0f : netForce / lastInertia;
        lastVelocity += acceleration * Time.delta;
        lastVelocity = Math.max(0f, lastVelocity);
        rotation += lastVelocity * Time.delta;
        if(rotation>360*2520){
            rotation-=360*2520;
        }
    }

    public void injectInertia(float iner){
        float inerSum = lastInertia + iner;
        lastVelocity *= inerSum == 0f ? 0f : lastInertia / inerSum;
    }

    @Override
    public void read(Reads read){
        super.read(read);
        lastVelocity = read.f();
        rotation = read.f();
    }

    @Override
    public void write(Writes write){
        write.f(lastVelocity);
        write.f(rotation);
    }
}
