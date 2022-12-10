package unity.world.graph;

import arc.math.*;
import unity.gen.graph.*;
import unity.world.graph.connectors.*;
import unity.world.graph.nodes.SoulNodeType.*;

/** @author GlennFolker */
@SuppressWarnings("unchecked")
public class SoulGraph extends Graph<SoulGraph> implements SoulGraphI<SoulGraph>{
    @Override
    public SoulGraph copy(){
        return new SoulGraph();
    }

    @Override
    public int type(){
        return Graphs.soul;
    }

    @Override
    public void onUpdate(){
        for(int i = 0; i < 3; i++){
            for(var node : nodes){
                SoulNode n = node.as();
                if(!n.canTransfer()) continue;

                float delta = n.build().delta();

                int total = 0;
                for(var v : n.connectors){
                    for(var e : v.connections){
                        if(e.n2 == v && e.n1.<SoulNode>node().canReceive()) total++;
                    }
                }

                if(total == 0) continue;

                float resist = n.resistance() * delta;
                float transferAmount = Math.min(
                    n.amount - resist - n.consumption(),
                    n.maxThroughput() * delta - n.transferred
                );

                if(transferAmount <= 0f) continue;
                for(var v : n.connectors){
                    int count = 0;
                    for(var e : v.connections){
                        if(e.n2 == v && e.n1.<SoulNode>node().canReceive()) count++;
                    }

                    if(count == 0) continue;
                    float amount = transferAmount / count;

                    for(var e : v.connections){
                        SoulNode dst = e.n1.node();
                        if(e.n2 != v || !dst.canReceive()) continue;

                        dst.amount += amount;
                    }
                }

                n.amount -= transferAmount + resist;
                n.transferred += transferAmount;
            }
        }
    }
}
