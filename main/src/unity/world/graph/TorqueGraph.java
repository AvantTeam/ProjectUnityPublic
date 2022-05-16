package unity.world.graph;

import arc.func.*;
import arc.struct.*;
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

    public OrderedSet<GraphConnector<TorqueGraph>> multiconnectors = new OrderedSet<>();

    @Override
    public void onVertexRemoved(GraphConnector<TorqueGraph> vertex){
        multiconnectors.remove(vertex);
    }

    @Override
    public void onVertexAdded(GraphConnector<TorqueGraph> vertex){
        if(vertex.getNode().connectors>1){
            multiconnectors.add(vertex);
        }
    }

    public void propagate(Cons<TorqueGraph> cons){
        ObjectSet<TorqueGraph> visited = new ObjectSet<>();
        Seq<TorqueGraph> toVisit = new Seq<>();
        visited.add(this);
        toVisit.add(this);
        while(!toVisit.isEmpty()){
            TorqueGraph graph = toVisit.pop();
            cons.get(graph);
            for(var mc: graph.multiconnectors){
                for(var other: mc.getNode().connector){
                    if(other!=mc && !visited.contains(other.getGraph())){
                        visited.add(other.getGraph());
                        toVisit.add(other.getGraph());
                    }
                }
            }
        }
    }

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
