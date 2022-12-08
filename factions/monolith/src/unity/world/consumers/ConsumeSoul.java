package unity.world.consumers;

import arc.math.*;
import mindustry.gen.*;
import mindustry.world.consumers.*;
import unity.gen.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.SoulNodeType.*;

/** @author GlennFolker */
public class ConsumeSoul extends Consume{
    public float amount;

    public ConsumeSoul(float amount){
        this.amount = amount;
        update = false;
    }

    @Override
    public float efficiency(Building build){
        if(!(build instanceof GraphBuild b)) return 0f;

        SoulNode node = b.graphNode(Graphs.soul);
        return node == null ? 0f : Mathf.clamp(node.amount / (amount * build.edelta() * build.efficiencyScale()));
    }

    @Override
    public void update(Building build){
        SoulNode node;
        if(!(build instanceof GraphBuild b) || (node = b.graphNode(Graphs.soul)) == null) return;

        node.amount = Math.max(0f, node.amount - amount * b.edelta());
    }
}
