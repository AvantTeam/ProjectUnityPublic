package unity.world.graph.nodes;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;
import unity.graphics.*;
import unity.ui.*;
import unity.ui.SegmentBar.*;
import unity.world.consumers.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.meta.*;

/** @author GlennFolker */
public class SoulNodeType extends GraphNodeType<SoulGraph> implements SoulNodeTypeI<SoulGraph>{
    public float production = 0f;
    public float resistance = 0.04f;

    public float safeLimit = 30f;
    public float absoluteLimit = 42f;
    public float criticalLimit = 48f;
    
    public float overloadDump = 0.8f;
    public float overloadScale = 0.4f;

    @Override
    public void setStats(Stats stats){
        if(production > 0f) stats.add(PUStat.soulProduction, production * 60f, PUStatUnit.soulSecond);
        stats.add(PUStat.soulResistance, resistance * 60f, PUStatUnit.soulSecond);
        stats.add(PUStat.soulSafeLimit, safeLimit, PUStatUnit.soulUnit);
        stats.add(PUStat.soulAbsLimit, absoluteLimit, PUStatUnit.soulUnit);
        stats.add(PUStat.soulCritLimit, criticalLimit, PUStatUnit.soulUnit);
        stats.add(PUStat.soulOverDump, overloadDump * 60f, PUStatUnit.soulSecond);
        stats.add(PUStat.soulOverScale, overloadScale, PUStatUnit.soulPer);
    }

    @Override
    public <E extends Building & GraphBuild> SoulNode create(E build){
        return new SoulNode(build, this);
    }

    public static class SoulNode extends GraphNode<SoulGraph> implements SoulNodeI<SoulGraph>{
        public @Nullable ConsumeSoul cons;

        public float production;
        public float resistance;

        public float safeLimit;
        public float absoluteLimit;
        public float criticalLimit;
        
        public float overloadDump;
        public float overloadScale;

        public float prodEfficiency;
        public float amount, transferred;
        public float visualAmount, visualTransferred;

        public <E extends Building & GraphBuild> SoulNode(E build, SoulNodeType type){
            super(build);
            production = type.production;
            resistance = type.resistance;
            safeLimit = type.safeLimit;
            absoluteLimit = type.absoluteLimit;
            criticalLimit = type.criticalLimit;
            overloadDump = type.overloadDump;
            overloadScale = type.overloadScale;

            cons = build.block.findConsumer(c -> c instanceof ConsumeSoul);
        }

        @Override
        public float consumption(){
            return cons == null ? 0f : cons.amount;
        }

        @Override
        public void displayBars(Table table){
            Seq<Segment> segments = Seq.with(
                new Segment(() -> MonolithPal.monolithLighter, 0f),
                new Segment(() -> Pal.redLight, (consumption() + safeLimit) / criticalLimit),
                new Segment(() -> Pal.removeBack, (consumption() + absoluteLimit) / criticalLimit)
            );

            if(consumption() > 0f) segments.insert(1, new Segment(
                () -> MonolithPal.monolithLight,
                consumption() / criticalLimit
            ));

            table.row();
            table.add(new SegmentBar(
                () -> Core.bundle.get("category.unity-soul"),
                () -> amount / criticalLimit,
                segments.toArray(Segment.class)
            ));
        }

        @Override
        public float production(){ return production; }
        @Override
        public float resistance(){ return resistance; }
        @Override
        public float safeLimit(){ return safeLimit; }
        @Override
        public float absoluteLimit(){ return absoluteLimit; }
        @Override
        public float overloadDump(){ return overloadDump; }
        @Override
        public float overloadScale(){ return overloadScale; }
        @Override
        public float amount(){ return amount; }
        @Override
        public float transferred(){ return transferred; }
        @Override
        public float visualAmount(){ return visualAmount; }
        @Override
        public float visualTransferred(){ return visualTransferred; }
        @Override
        public float prodEfficiency(){ return prodEfficiency; }
        @Override
        public void prodEfficiency(float prodEfficiency){ this.prodEfficiency = prodEfficiency; }
    }
}
