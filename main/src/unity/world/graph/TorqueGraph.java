package unity.world.graph;

import arc.util.*;

//rotGraph
public class TorqueGraph extends Graph<TorqueGraph>{
    public float lastInertia, lastGrossForceApplied, lastNetForceApplied, lastVelocity, lastFrictionCoefficient;
    public float rotation = 0;
    @Override
    public TorqueGraph createFromThis(){
        var t = new TorqueGraph();
        t.lastVelocity = lastVelocity;
        return t;
    }

    @Override public <U extends Graph<TorqueGraph>> void onMergeBegin(TorqueGraph graph){
        float momentumA = lastVelocity * lastInertia;
        float mementumB = graph.lastVelocity * graph.lastInertia;
        lastVelocity = (momentumA + mementumB) / (lastInertia + graph.lastInertia);
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
            if(module.getNode() instanceof GraphTorqueNode torqueNode){
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
}
