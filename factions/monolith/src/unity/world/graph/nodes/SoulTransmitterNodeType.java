package unity.world.graph.nodes;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import unity.gen.graph.*;
import unity.util.*;
import unity.world.blocks.power.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.connectors.DistanceConnectorType.*;

import static mindustry.Vars.*;
import static unity.graphics.MonolithPal.*;

public class SoulTransmitterNodeType extends SoulNodeType{
    protected static final Rand rand = new Rand();

    public static final float laserWidth = 2f, laserWidthInner = 1f;
    public static final Color laserColor = monolithLighter.cpy().a(0.5f), laserColorInner = Color.white.cpy().a(0.67f);
    public static final float lineWidthMin = 0.6f, lineWidthMax = 1.2f, lineLenMin = 2.4f, lineLenMax = 3.6f;
    public static final Color lineColorA = monolithMid.cpy().lerp(monolithLight, 0.5f), lineColorB = monolithLighter;
    public static final float blobRadMin = 0.5f, blobRadMax = 0.8f;
    public static final Color blobColorA = monolithMid, blobColorB = monolithLight.cpy().lerp(monolithLighter, 0.5f);
    public static final float fade = 1.5f, matterAlphaMin = 0.33f;

    {
        canTransfer = true;
    }

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
    public <E extends Building & GraphBuild> SoulTransmitterNode create(E build){
        return new SoulTransmitterNode(build);
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
        if(range <= 0f) return false;

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

    public class SoulTransmitterNode extends SoulNode{
        public float transmitPower, totalTransmit;
        public float beamAlpha, matterAlpha;

        public <E extends Building & GraphBuild> SoulTransmitterNode(E build){
            super(build);
        }

        @Override
        public void update(){
            super.update();
            transmitPower = Mathf.log(1.5f, visualTransferred + 1f);
            totalTransmit += transmitPower * Time.delta;
            beamAlpha = Mathf.approachDelta(beamAlpha, transferred > 0f ? 1f : 0f, 0.07f);
            matterAlpha = Mathf.approachDelta(matterAlpha, transferred > 0f ? 1f : matterAlphaMin, 0.016f);
        }

        @Override
        public void draw(){
            super.draw();
            var build = build();

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

                    float lifetime = (18f + rand.range(6f)) * dstScl;
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
                    DrawUtils.lineAngleCenter(
                        Core.atlas.find("circle-mid"), Core.atlas.find("unity-circle-end"),
                        lx, ly, angle, length
                    );

                    Draw.blend();
                }

                float blobs = dstScl * 3.6f;
                for(int i = 0, len = Mathf.ceilPositive(blobs); i < len; i++){
                    rand.setSeed(e.id - i - 1);

                    float lifetime = (36f + rand.range(4f)) * dstScl;
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

        @Override
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

        @Override
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
        public void displayBars(Table table){
            DistanceConnector<SoulGraph> conn = build().graphConnector(Graphs.soul, DistanceConnector.class);
            if(conn == null) return;

            table.row().add(new Bar(
                () -> Core.bundle.format("bar.unity-soul-lines", conn.validConnections(), conn.maxConnections),
                () -> Pal.items,
                () -> (float)conn.validConnections() / conn.maxConnections
            ));

            super.displayBars(table);
        }
    }
}
