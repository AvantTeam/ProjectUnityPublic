package unity.world.consumers;

import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import unity.gen.graph.*;
import unity.graphics.*;
import unity.ui.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.SoulNodeType.*;

import static mindustry.Vars.*;

/** @author GlennFolker */
public class ConsumeSoul extends Consume{
    public float amount;

    public ConsumeSoul(float amount){
        this.amount = amount;
    }

    @Override
    public float efficiency(Building build){
        SoulNode node;
        if(!(build instanceof GraphBuild b) || (node = b.graphNode(Graphs.soul)) == null) return 0f;

        return Mathf.clamp(node.amount / (amount * build.edelta()));
    }

    @Override
    public void update(Building build){
        SoulNode node;
        if(!(build instanceof GraphBuild b) || (node = b.graphNode(Graphs.soul)) == null) return;

        node.amount = Math.max(0f, node.amount - amount * b.edelta());
    }

    @Override
    public void build(Building build, Table table){
        SoulNode node;
        if(!(build instanceof GraphBuild b) || (node = b.graphNode(Graphs.soul)) == null) return;

        table.add(new ReqImage(new Image(PUIcon.soul).setScaling(Scaling.fit), () -> node.amount > 0)).size(iconMed).top().left();
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, table -> table.add(new SoulDisplay(amount * 60f, true)).padRight(5f));
    }
}
