package unity.world.graph;

import arc.math.*;
import arc.util.*;
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
        for(var v : vertices){
            SoulNode node = v.node();
            node.transferred = 0f;
            node.amount += node.production * node.prodEfficiency * Time.delta;
        }

        for(int i = 0; i < 3; i++){
            for(var v : vertices){
                if(v.connections.size == 0) continue;

                int count = 0;
                for(var e : v.connections){
                    if(e.n2 == v) count++;
                }

                if(count == 0) continue;
                SoulNode node = v.node();

                float transferAmount = (node.amount - node.resistance - node.consumption()) / count;
                if(transferAmount <= 0f) continue;

                for(var e : v.connections){
                    if(e.n2 != v) continue;
                    e.n1.<SoulNode>node().amount += transferAmount;
                }

                transferAmount *= count;
                node.amount -= transferAmount + node.resistance;
                node.transferred += transferAmount;
            }
        }

        for(var v : vertices){
            SoulNode node = v.node();
            float cons = node.consumption();
            float excess = node.amount - cons;

            if(excess >= node.criticalLimit){
                node.build().kill();
                continue;
            }

            if(excess > node.safeLimit){
                excess = (node.amount = Math.max(node.safeLimit, node.amount - node.overloadDump * Time.delta)) - cons;
            }

            if(excess > node.absoluteLimit){
                float over = excess - node.absoluteLimit;
                node.build().damageContinuous(over * node.overloadScale);
            }

            node.visualAmount = Mathf.lerpDelta(node.visualAmount, node.amount, 0.1f);
            node.visualTransferred = Mathf.lerpDelta(node.visualTransferred, node.transferred / Time.delta, 0.1f);
        }
    }
}
