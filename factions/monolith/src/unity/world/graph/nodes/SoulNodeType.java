package unity.world.graph.nodes;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.content.*;
import unity.graphics.*;
import unity.ui.*;
import unity.ui.SegmentBar.*;
import unity.world.consumers.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.meta.*;

import static mindustry.Vars.*;

/** @author GlennFolker */
public class SoulNodeType extends GraphNodeType<SoulGraph> implements SoulNodeTypeI<SoulGraph>{
    public Color coolColor = new Color(1f, 1f, 1f, 0f);
    public Color hotColor = new Color(0xff9575a3);
    public float criticalStart = 0.33f;

    public TextureRegion heatRegion, soulRegion;

    public float smokeChance = 0.14f;
    public Effect smokeEffect = MonolithFx.overloadSmoke;

    public float production = 0f;
    public float resistance = 0.04f;
    public float maxThroughput = 4f;

    public float safeLimit = 30f;
    public float absoluteLimit = 42f;
    public float criticalLimit = 48f;
    
    public float overloadDump = 0.8f;
    public float overloadScale = 0.1f;

    public boolean canReceive = true;
    public boolean canTransfer = true;

    @Override
    public <E extends Block & GraphBlock> void load(E block){
        heatRegion = Core.atlas.find(block.name + "-heat");
        soulRegion = Core.atlas.find(block.name + "-soul");
    }

    @Override
    public void setStats(Stats stats){
        if(production > 0f) stats.add(PUStat.soulProduction, production * 60f, PUStatUnit.soulSecond);
        stats.add(PUStat.soulResistance, resistance * 60f, PUStatUnit.soulSecond);
        stats.add(PUStat.soulMaxThrough, maxThroughput * 60f, PUStatUnit.soulSecond);

        stats.add(PUStat.soulSafeLimit, safeLimit, PUStatUnit.soulUnit);
        stats.add(PUStat.soulAbsLimit, absoluteLimit, PUStatUnit.soulUnit);
        stats.add(PUStat.soulCritLimit, criticalLimit, PUStatUnit.soulUnit);

        stats.add(PUStat.soulOverDump, overloadDump * 60f, PUStatUnit.soulSecond);
        stats.add(PUStat.soulOverScale, overloadScale, PUStatUnit.soulPer);
    }

    @Override
    public <E extends Building & GraphBuild> SoulNode create(E build){
        return new SoulNode(build);
    }

    public class SoulNode extends GraphNode<SoulGraph> implements SoulNodeI<SoulGraph>{
        public @Nullable ConsumeSoul cons;

        public float prodEfficiency;
        public float amount, transferred;
        public float visualAmount, visualTransferred;

        public float totalTransferred;

        public <E extends Building & GraphBuild> SoulNode(E build){
            super(build);
            cons = build.block.findConsumer(c -> c instanceof ConsumeSoul);
        }

        @Override
        public void preUpdate(){
            transferred = 0f;
            amount += production * prodEfficiency * Time.delta;
        }

        @Override
        public void update(){
            var build = build();
            var block = build.block;

            if(amount > safeLimit){
                amount = Math.max(safeLimit, amount - overloadDump * Time.delta);
            }

            if(amount > absoluteLimit){
                float over = amount - absoluteLimit;
                build.damageContinuous(over * overloadScale);
            }

            /*if(amount > criticalLimit){
                build.kill();
                return;
            }*/

            if(amount > safeLimit){
                float chance =
                    Mathf.curve(amount, safeLimit, absoluteLimit) * criticalStart +
                    Mathf.curve(amount, absoluteLimit, criticalLimit) * (1f - criticalStart);

                if(Mathf.chance(chance * smokeChance * build.delta())){
                    smokeEffect.at(
                        build.x + Mathf.range(block.size * tilesize / 2f),
                        build.y + Mathf.range(block.size * tilesize / 2f)
                    );
                }
            }

            visualAmount = Mathf.lerpDelta(visualAmount, amount, 0.1f);
            visualTransferred = Mathf.lerpDelta(visualTransferred, transferred / Time.delta, 0.1f);
            totalTransferred += visualTransferred * Time.delta;
        }

        @Override
        public void draw(){
            var build = build();
            Draw.color(coolColor, hotColor,
                Mathf.curve(amount, safeLimit, absoluteLimit) * criticalStart +
                Mathf.curve(amount, absoluteLimit, criticalLimit) * (1f - criticalStart)
            );

            Draw.rect(heatRegion, build.x, build.y);

            Draw.color(MonolithPal.monolithLighter, Mathf.clamp(amount / safeLimit));
            Draw.rect(soulRegion, build.x, build.y);

            Draw.reset();
        }

        @Override
        public float consumption(){
            return cons == null ? 0f : cons.amount * build().edelta();
        }

        @Override
        public void displayBars(Table table){
            table.row();
            table.add(new SegmentBar(
                () -> Core.bundle.get("category.unity-soul"),
                () -> amount / criticalLimit,
                new Segment(() -> MonolithPal.monolithLighter, 0f),
                new Segment(() -> Pal.redLight, safeLimit / criticalLimit),
                new Segment(() -> Pal.removeBack, absoluteLimit / criticalLimit)
            ));
        }

        @Override
        public float production(){ return production; }
        @Override
        public float resistance(){ return resistance; }
        @Override
        public float maxThroughput() { return maxThroughput; }
        @Override
        public float safeLimit(){ return safeLimit; }
        @Override
        public float absoluteLimit(){ return absoluteLimit; }
        @Override
        public float overloadDump(){ return overloadDump; }
        @Override
        public float overloadScale(){ return overloadScale; }
        @Override
        public boolean canReceive(){ return canReceive; }
        @Override
        public boolean canTransfer(){ return canTransfer; }

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
