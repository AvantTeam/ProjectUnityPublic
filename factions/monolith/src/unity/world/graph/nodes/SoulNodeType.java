package unity.world.graph.nodes;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.content.*;
import unity.content.MonolithFx.*;
import unity.gen.graph.*;
import unity.ui.*;
import unity.ui.SegmentBar.*;
import unity.util.*;
import unity.world.consumers.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.connectors.GraphConnectorType.*;
import unity.world.graph.connectors.DistanceConnectorType.*;
import unity.world.meta.*;

import static mindustry.Vars.*;
import static unity.graphics.MonolithPal.*;

/** @author GlennFolker */
@SuppressWarnings("unchecked")
public class SoulNodeType extends GraphNodeType<SoulGraph> implements SoulNodeTypeI<SoulGraph>{
    public Color coolColor = new Color(1f, 1f, 1f, 0f);
    public Color hotColor = new Color(0xff9575a3);
    public float criticalStart = 0.33f;

    public TextureRegion heatRegion, soulRegion;

    public Effect smokeEffect = MonolithFx.overloadSmoke;
    public float smokeChance = 0.14f;

    public float laserWidth = 2f, laserWidthInner = 1f;
    public Color laserColor = monolithLight.cpy().a(0.5f), laserColorInner = monolithLighter.cpy().a(0.67f);
    public Effect transferEffect = MonolithFx.nodeTransfer;
    public float transferEffectInterval = 8f;

    public Color trailColor = monolithLighter;
    public Func2<GraphBuild, SoulNode, Trail> trailType = (build, node) -> new Trail(6);
    public float trailInterval = 60f;

    public float production = 0f;
    public float resistance = 1f / 60f;
    public float maxThroughput = 4f;

    public float safeLimit = 30f;
    public float absoluteLimit = 42f;
    public float criticalLimit = 48f;
    
    public float overloadDump = 0.8f;
    public float overloadScale = 0.1f;

    public boolean canReceive = true;
    public boolean canTransfer = true;

    public <E extends GraphBuild> void trailType(Func2<E, SoulNode, Trail> trailType){
        this.trailType = (Func2<GraphBuild, SoulNode, Trail>)trailType;
    }

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

        public float transmitPower, totalTransmit;
        public float on;

        public float lastEffect;

        public <E extends Building & GraphBuild> SoulNode(E build){
            super(build);
            cons = build.block.findConsumer(c -> c instanceof ConsumeSoul);
        }

        public int transferCount(){
            int total = 0;
            for(var conn : connectors) total += transferCount(conn);
            return total;
        }

        public int transferCount(GraphConnector<SoulGraph> conn){
            int total = 0;
            for(var e : conn.connections){
                if(e.n2 == conn && e.n1.<SoulNode>node().canReceive()) total++;
            }
            return total;
        }

        @Override
        public void preUpdate(){
            transferred = 0f;
            amount += production * prodEfficiency * build().delta();
        }

        @Override
        public void update(){
            var build = build();
            var block = build.block;

            float delta = build.delta();
            if(amount > safeLimit){
                amount = Math.max(safeLimit, amount - overloadDump * delta);
            }

            if(amount > absoluteLimit){
                float over = (amount - absoluteLimit) / Time.delta;
                build.damageContinuous(over * overloadScale);
            }

            if(amount > criticalLimit){
                float excess = amount - criticalLimit;
                amount -= excess;

                excess /= Time.delta;
                //TODO do something with the critical excess
            }

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

            visualAmount = Mathf.lerpDelta(visualAmount, amount, 0.07f);

            int count = transferCount();
            if(count == 0){
                visualTransferred = Mathf.lerpDelta(visualTransferred, 0f, 0.07f);
            }else{
                visualTransferred = Mathf.lerpDelta(visualTransferred, (transferred / count) / Time.delta, 0.07f);
            }

            transmitPower = Mathf.log(2.4f, visualTransferred + 1f);
            totalTransmit += transmitPower * Time.delta;
            on = Mathf.approachDelta(on, transferred > 0f ? 1f : 0f, 0.07f);

            DistanceConnector<SoulGraph> conn = build.graphConnector(Graphs.soul, DistanceConnector.class);
            if(conn == null) return;

            if(totalTransmit - lastEffect >= transferEffectInterval){
                lastEffect = totalTransmit;
                for(var e : conn.connections){
                    if(e.n2 != conn) continue;
                    var other = e.other(conn).node().build();

                    Tmp.v1.set(other).sub(build).setLength(block.size * tilesize / 2f - 1.5f);
                    float sx = build.x + Tmp.v1.x, sy = build.y + Tmp.v1.y;

                    Tmp.v1.set(build).sub(other).setLength(other.block.size * tilesize / 2f - 1.5f);
                    float ex = other.x + Tmp.v1.x, ey = other.y + Tmp.v1.y;

                    transferEffect.at(sx, sy, 0f, new TransferData(this, e.n2.as(), e.n1.as(), sx, sy, ex, ey));
                }
            }
        }

        @Override
        public void draw(){
            var build = build();
            Draw.color(coolColor, hotColor,
                Mathf.curve(visualAmount, safeLimit, absoluteLimit) * criticalStart +
                Mathf.curve(visualAmount, absoluteLimit, criticalLimit) * (1f - criticalStart)
            );

            Draw.rect(heatRegion, build.x, build.y);

            Draw.color(monolithLighter, Mathf.clamp(visualAmount / safeLimit));
            Draw.rect(soulRegion, build.x, build.y);

            Draw.reset();

            DistanceConnector<SoulGraph> conn = build.graphConnector(Graphs.soul, DistanceConnector.class);
            if(conn == null) return;

            float z = Draw.z();
            for(var e : conn.connections){
                if(e.n2 != conn) continue;
                var other = e.other(conn).node().build();

                Tmp.v1.set(other).sub(build).setLength(build.block.size * tilesize / 2f - 1.5f);
                float sx = build.x + Tmp.v1.x, sy = build.y + Tmp.v1.y;

                Tmp.v1.set(build).sub(other).setLength(other.block.size * tilesize / 2f - 1.5f);
                float ex = other.x + Tmp.v1.x, ey = other.y + Tmp.v1.y;

                Draw.z(Layer.power + 0.01f);
                Lines.stroke(laserWidth, laserColor);
                Draw.alpha(0.33f + 0.67f * on);
                DrawUtils.line(sx, sy, ex, ey);

                Draw.z(Layer.power + 0.02f);
                Lines.stroke(laserWidthInner, laserColorInner);
                Draw.alpha(0.3f + 0.67f * on);
                DrawUtils.line(sx, sy, ex, ey);
            }

            Draw.reset();
            Draw.z(z);
        }

        @Override
        public float consumption(){
            return cons == null ? 0f : cons.amount * build().edelta();
        }

        @Override
        public void displayBars(Table table){
            table.row();
            table.add(new SegmentBar(
                () -> Core.bundle.get("bar.unity-soul"),
                () -> amount / criticalLimit,
                new Segment(() -> monolithLighter, 0f),
                new Segment(() -> Pal.redLight, safeLimit / criticalLimit),
                new Segment(() -> Pal.removeBack, absoluteLimit / criticalLimit)
            ));
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(prodEfficiency);
            write.f(amount);
        }

        @Override
        public void read(Reads read){
            super.read(read);
            prodEfficiency = read.f();
            amount = read.f();
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
