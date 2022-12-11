package unity.world.graph.nodes;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.content.*;
import unity.content.MonolithFx.*;
import unity.gen.graph.*;
import unity.ui.*;
import unity.ui.SegmentBar.*;
import unity.ui.SegmentBar.Segment;
import unity.util.*;
import unity.world.blocks.power.*;
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
    protected static final Rand rand = new Rand();

    public static final float laserWidth = 2f, laserWidthInner = 1f;
    public static final Color laserColor = monolithLighter.cpy().a(0.5f), laserColorInner = Color.white.cpy().a(0.67f);
    public static final float lineWidthMin = 0.6f, lineWidthMax = 1.2f, lineLenMin = 2.4f, lineLenMax = 3.6f;
    public static final Color lineColorA = monolithMid.cpy().lerp(monolithLight, 0.5f), lineColorB = monolithLighter;
    public static final float blobRadMin = 0.3f, blobRadMax = 0.6f;
    public static final Color blobColorA = monolithMid, blobColorB = monolithLight.cpy().lerp(monolithLighter, 0.5f);
    public static final float fade = 1.5f, matterAlphaMin = 0.33f;

    public Color coolColor = new Color(1f, 1f, 1f, 0f);
    public Color hotColor = new Color(0xff9575a3);
    public float criticalStart = 0.33f;

    public TextureRegion heatRegion, soulRegion;

    public Effect smokeEffect = MonolithFx.overloadSmoke;
    public float smokeChance = 0.14f;

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

    @Override
    public <E extends Block & GraphBlock> void apply(E block){
        block.configurable = true;
        block.config(Integer.class, (build, value) -> {
            DistanceConnector<SoulGraph> conn, other;
            if(
                !(build instanceof GraphBuild b) ||
                !(world.build(value) instanceof GraphBuild o) ||
                (conn = b.graphConnector(Graphs.soul, DistanceConnector.class)) == null ||
                (other = o.graphConnector(Graphs.soul, DistanceConnector.class)) == null
            ) return;

            if(conn.isConnected(other)){
                conn.disconnectTo(other);
            }else if(linkValid(b.as(), o.as())){
                conn.connectTo(other);
            }
        });
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

    public static <E extends Building & GraphBuild> boolean linkValid(E tile, E link){
        return linkValid(tile, link, true);
    }

    public static <E extends Building & GraphBuild> boolean linkValid(E tile, E link, boolean checkMax){
        if(tile == link || link == null || tile.team != link.team) return false;

        DistanceConnector<SoulGraph>
            tileConn = tile.graphConnector(Graphs.soul, DistanceConnector.class),
            linkConn = link.graphConnector(Graphs.soul, DistanceConnector.class);
        if(tileConn == null || linkConn == null) return false;

        float range = Float.NEGATIVE_INFINITY;
        if(tile.block instanceof SoulTransmitter tr) range = Math.max(range, tr.laserRange);
        if(link.block instanceof SoulTransmitter tr) range = Math.max(range, tr.laserRange);
        if(range == Float.NEGATIVE_INFINITY) return false;

        return
            overlaps(tile, link, range * tilesize) &&
            !checkMax ||
            tileConn.validConnections() < tileConn.maxConnections ||
            linkConn.validConnections() < linkConn.maxConnections;
    }

    protected static boolean overlaps(float srcx, float srcy, Tile other, Block otherBlock, float range){
        return Intersector.overlaps(
            Tmp.cr1.set(srcx, srcy, range),
            Tmp.r1.setCentered(
                other.worldx() + otherBlock.offset, other.worldy() + otherBlock.offset,
                otherBlock.size * tilesize, otherBlock.size * tilesize
            )
        );
    }

    protected static boolean overlaps(float srcx, float srcy, Tile other, float range){
        return Intersector.overlaps(Tmp.cr1.set(srcx, srcy, range), other.getHitbox(Tmp.r1));
    }

    protected static boolean overlaps(Building src, Building other, float range){
        return overlaps(src.x, src.y, other.tile(), range);
    }

    protected static boolean overlaps(Tile src, Tile other, float range){
        return overlaps(src.drawx(), src.drawy(), other, range);
    }

    public class SoulNode extends GraphNode<SoulGraph> implements SoulNodeI<SoulGraph>{
        public @Nullable ConsumeSoul cons;

        public float prodEfficiency;
        public float amount, transferred;
        public float visualAmount, visualTransferred;

        public float produced;
        public float transmitPower, totalTransmit;
        public float beamAlpha, matterAlpha;

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

            if(production > 0){
                float prod = production * prodEfficiency * build().delta();
                amount += prod;

                produced = prod / Time.delta;
            }
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

            transmitPower = Mathf.log(1.5f, visualTransferred + 1f);
            totalTransmit += transmitPower * Time.delta;
            beamAlpha = Mathf.approachDelta(beamAlpha, transferred > 0f ? 1f : 0f, 0.07f);
            matterAlpha = Mathf.approachDelta(matterAlpha, transferred > 0f ? 1f : matterAlphaMin, 0.016f);
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

                Tmp.v1.set(other).sub(build).setLength(build.block.size * tilesize / 2f - fade);
                float sx = build.x + Tmp.v1.x, sy = build.y + Tmp.v1.y;

                Tmp.v1.set(build).sub(other).setLength(other.block.size * tilesize / 2f - fade);
                float ex = other.x + Tmp.v1.x, ey = other.y + Tmp.v1.y;

                Draw.z(Layer.power + 0.01f);
                Lines.stroke(laserWidth, laserColor);
                Draw.alpha((0.4f + 0.6f * beamAlpha) * laserColor.a);
                DrawUtils.line(sx, sy, ex, ey);

                Draw.z(Layer.power + 0.02f);
                Lines.stroke(laserWidthInner, laserColorInner);
                Draw.alpha((0.4f + 0.6f * beamAlpha) * laserColorInner.a);
                DrawUtils.line(sx, sy, ex, ey);

                float angle = Angles.angle(sx, sy, ex, ey);
                float dst = Mathf.dst(sx, sy, ex, ey);
                float dstScl = dst / tilesize;
                float alphaScale = Mathf.curve(matterAlpha, matterAlphaMin, 1f);

                float lines = dstScl * 2.4f;
                for(int i = 0, len = Mathf.ceilPositive(lines); i < len; i++){
                    rand.setSeed(e.id + i);

                    float lifetime = (24f + rand.range(4f)) * dstScl;
                    float time = (totalTransmit + (lifetime / lines) * i + rand.range(dstScl)) % lifetime;
                    float in = time / lifetime;

                    float width = rand.random(lineWidthMin, lineWidthMax);
                    float length = rand.random(lineLenMin, lineLenMax) * Interp.smoother.apply(Interp.pow2Out.apply(alphaScale));
                    float off = dst * in;
                    float alpha = off <= fade
                        ? (off / fade)
                        : off >= (dst - fade)
                            ? (dst - off) / fade
                            : 1f;

                    Tmp.v1.trns(angle - 90f, rand.range(laserWidth / 2f + 1.2f), off).add(sx, sy);
                    float lx = Tmp.v1.x, ly = Tmp.v1.y;

                    Lines.stroke(width, Tmp.c1.set(lineColorA).lerp(lineColorB, rand.nextFloat()));
                    Draw.alpha(matterAlpha * alpha);

                    Draw.z(Layer.power + 0.015f + rand.range(0.015f));
                    DrawUtils.lineAngleCenter(lx, ly, angle, length);

                    Draw.z(Layer.power + 0.04f);
                    Lines.stroke(Lines.getStroke() + 3.6f);
                    Draw.color(Draw.getColor(), Color.black, 0.9f);
                    Draw.alpha(Draw.getColor().a * alphaScale);
                    Draw.blend(Blending.additive);
                    DrawUtils.lineAngleCenter(Core.atlas.find("circle-mid"), Core.atlas.find("unity-circle-end"), lx, ly, angle, length);
                    Draw.blend();
                }

                float blobs = dstScl * 3.6f;
                for(int i = 0, len = Mathf.ceilPositive(blobs); i < len; i++){
                    rand.setSeed(e.id - i - 1);

                    float lifetime = (18f + rand.range(6f)) * dstScl;
                    float time = (totalTransmit + (lifetime / blobs) * i + rand.range(dstScl)) % lifetime;
                    float in = time / lifetime;

                    float rad = rand.random(blobRadMin, blobRadMax);
                    float off = dst * in;
                    float alpha = off <= fade
                        ? (off / fade)
                        : off >= (dst - fade)
                            ? (dst - off) / fade
                            : 1f;

                    Tmp.v1.trns(angle - 90f, rand.range(laserWidth / 2f + 1.6f), off).add(sx, sy);
                    float bx = Tmp.v1.x, by = Tmp.v1.y;

                    Draw.color(Tmp.c1.set(blobColorA).lerp(blobColorB, rand.nextFloat()));
                    Draw.alpha(alpha * matterAlpha * Interp.pow2In.apply(alphaScale));
                    Draw.z(Layer.power + 0.015f + rand.range(0.015f));
                    Fill.circle(bx, by, rad);

                    Draw.z(Layer.power + 0.04f);
                    Draw.color(Draw.getColor(), Color.black, 0.9f);
                    Draw.alpha(Draw.getColor().a * alphaScale);
                    Draw.blend(Blending.additive);
                    Draw.rect(Core.atlas.find("circle-shadow"), bx, by, rad * 2f + 3.6f, rad * 2f + 3.6f);
                    Draw.blend();
                }
            }

            Draw.reset();
            Draw.z(z);
        }

        public void drawConfigure(){
            var build = build();

            DistanceConnector<SoulGraph> conn = build.graphConnector(Graphs.soul, DistanceConnector.class);
            if(conn == null) return;

            for(var e : conn.connections){
                var other = e.other(conn).node().build();
                Tmp.v1.set(other.x, other.y).sub(build.x, build.y).limit(build.block.size * tilesize / 2f + 1.5f);
                float sx = build.x + Tmp.v1.x, sy = build.y + Tmp.v1.y;
                float ex = other.x, ey = other.y;

                Color color = e.n2 == conn ? Pal.accent : Pal.place;
                float angle = e.n2 == conn
                ? Angles.angle(sx, sy, ex, ey)
                : Angles.angle(ex, ey, sx, sy);

                float in = (Time.time % 72f) / 72f;
                if(e.n2 == conn){
                    Tmp.v1
                        .set(ex, ey).sub(sx, sy)
                        .scl(Interp.smoother.apply(in))
                        .add(sx, sy);
                }else{
                    Tmp.v1
                        .set(sx, sy).sub(ex, ey)
                        .scl(Interp.smoother.apply(in))
                        .add(ex, ey);
                }

                float vx = Tmp.v1.x, vy = Tmp.v1.y;
                float radius = Interp.pow5Out.apply(Mathf.slope(in)) * 3f;

                Lines.stroke(3f, Pal.gray);
                Lines.line(sx, sy, ex, ey);
                Fill.poly(vx, vy, 3, radius + 2f, angle);

                Lines.stroke(1f, color);
                Lines.line(sx, sy, ex, ey);
                Fill.poly(vx, vy, 3, radius, angle);

                Draw.reset();
            }
        }

        public boolean onConfigureBuildTapped(Building other){
            var build = build();

            DistanceConnector<SoulGraph> conn = build.graphConnector(Graphs.soul, DistanceConnector.class);
            if(conn == null) return true;

            if(other instanceof GraphBuild && linkValid(build, (Building & GraphBuild)other)){
                build.configure(other.pos());
                return false;
            }

            if(build == other){
                if(conn.validConnections() > 0) conn.disconnect();
                return false;
            }

            return true;
        }

        @Override
        public float consumption(){
            return cons == null ? 0f : cons.amount * build().edelta();
        }

        @Override
        public void displayBars(Table table){
            DistanceConnector<SoulGraph> conn = build().graphConnector(Graphs.soul, DistanceConnector.class);
            if(conn == null) return;

            table.row().add(new Bar(
                () -> Core.bundle.format("bar.powerlines", conn.validConnections(), conn.maxConnections),
                () -> Pal.items,
                () -> (float)conn.validConnections() / conn.maxConnections
            ));

            table.row().add(new SegmentBar(
                production > 0f
                    ? () -> { return Core.bundle.formatFloat("bar.unity-soul-produce", produced * 60f, 2); }
                    : () -> { return Core.bundle.get("bar.unity-soul"); },
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
