package unity.world.graph;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import unity.gen.graph.*;
import unity.world.graph.connectors.*;
import unity.world.graph.nodes.SoulNodeType.*;

/** @author GlennFolker */
@SuppressWarnings("unchecked")
public class SoulGraph extends Graph<SoulGraph> implements SoulGraphI<SoulGraph>{
    protected final OrderedSet<SoulNode> nodes = new OrderedSet<>();

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
        nodes.clear();
        for(var v : vertices){
            SoulNode node = v.node();
            if(nodes.add(node)){
                node.transferred = 0f;
                node.amount += node.production * node.prodEfficiency * Time.delta;
            }
        }

        for(int i = 0; i < 3; i++){
            for(var n : nodes){
                if(!n.canTransfer) continue;

                int total = 0;
                for(var v : n.connectors){
                    for(var e : v.connections){
                        if(e.n2 == v && e.n1.<SoulNode>node().canReceive) total++;
                    }
                }

                if(total == 0) continue;

                float transferAmount = n.amount - n.resistance - n.consumption();
                if(transferAmount <= 0f) continue;

                for(var v : n.connectors){
                    int count = 0;
                    for(var e : v.connections){
                        if(e.n2 == v && e.n1.<SoulNode>node().canReceive) count++;
                    }

                    if(count == 0) continue;
                    float amount = transferAmount / count;

                    for(var e : v.connections){
                        SoulNode dst = e.n1.node();
                        if(e.n2 != v || !dst.canReceive) continue;

                        dst.amount += amount;
                    }
                }

                n.amount -= transferAmount + n.resistance;
                n.transferred += transferAmount;
            }
        }

        for(var n : nodes){
            float cons = n.consumption();
            float excess = n.amount - cons;

            if(excess >= n.criticalLimit){
                n.build().kill();
                continue;
            }

            if(excess > n.safeLimit){
                excess = (n.amount = Math.max(n.safeLimit, n.amount - n.overloadDump * Time.delta)) - cons;
            }

            if(excess > n.absoluteLimit){
                float over = excess - n.absoluteLimit;
                n.build().damageContinuous(over * n.overloadScale);
            }

            n.visualAmount = Mathf.lerpDelta(n.visualAmount, n.amount, 0.1f);
            n.visualTransferred = Mathf.lerpDelta(n.visualTransferred, n.transferred / Time.delta, 0.1f);
        }
    }
}
