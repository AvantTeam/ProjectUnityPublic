package unity.world.blocks.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import unity.gen.graph.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.connectors.DistanceConnectorType.*;
import unity.world.graph.connectors.DistanceConnectorType.DistanceConnector.*;

import static mindustry.Vars.*;

/** @author GlennFolker */
@SuppressWarnings("unchecked")
public class SoulTransmitter extends SoulBlock{
    public float laserRange = 8f;

    public SoulTransmitter(String name){
        super(name);
        configurable = true;

        config(Integer.class, (SoulTransmitterBuild b, Integer value) -> {
            DistanceConnector<SoulGraph> other;
            if(
                !(world.build(value) instanceof GraphBuild o) ||
                (other = o.graphConnector(Graphs.soul, DistanceConnector.class)) == null
            ) return;

            if(b.connector.isConnected(other)){
                b.connector.disconnectTo(other);
            }else if(linkValid(b, o.as())){
                b.connector.connectTo(other);
            }
        });

        config(IntSeq.class, (SoulTransmitterBuild b, IntSeq connections) -> {
            b.connector.disconnect();
            for(int i = 0; i < connections.size; i += 2){
                int pos = connections.get(i);

                DistanceConnector<SoulGraph> other;
                if(
                    !(world.build(b.tileX() + Point2.x(pos), b.tileY() + Point2.y(pos)) instanceof GraphBuild o) ||
                    (other = o.graphConnector(Graphs.soul, DistanceConnector.class)) == null ||
                    !linkValid(b, o.as())
                ) return;

                if(connections.get(i + 1) == 1){
                    b.connector.connectTo(other);
                }else{
                    other.connectTo(b.connector);
                }
            }
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("connections", (SoulTransmitterBuild b) -> new Bar(
            () -> Core.bundle.format("bar.powerlines", b.connector.validConnections(), b.connector.maxConnections),
            () -> Pal.items,
            () -> (float)b.connector.validConnections() / b.connector.maxConnections
        ));
    }

    public <E extends Building & GraphBuild> boolean linkValid(E tile, E link){
        return linkValid(tile, link, true);
    }

    public <E extends Building & GraphBuild> boolean linkValid(E tile, E link, boolean checkMax){
        if(tile == link || link == null || tile.team != link.team) return false;

        DistanceConnector<SoulGraph>
            tileConn = tile.graphConnector(Graphs.soul, DistanceConnector.class),
            linkConn = link.graphConnector(Graphs.soul, DistanceConnector.class);
        if(tileConn == null || linkConn == null) return false;

        if(overlaps(tile, link, laserRange * tilesize) || (link.block instanceof SoulTransmitter tr && overlaps(link, tile, tr.laserRange * tilesize))){
            return !checkMax ||
                tileConn.isConnected(linkConn) ||
                tileConn.validConnections() < tileConn.maxConnections ||
                linkConn.validConnections() < linkConn.maxConnections;
        }

        return false;
    }

    protected boolean overlaps(float srcx, float srcy, Tile other, Block otherBlock, float range){
        return Intersector.overlaps(
            Tmp.cr1.set(srcx, srcy, range), Tmp.r1.setCentered(other.worldx() + otherBlock.offset, other.worldy() + otherBlock.offset,
            otherBlock.size * tilesize, otherBlock.size * tilesize)
        );
    }

    protected boolean overlaps(float srcx, float srcy, Tile other, float range){
        return Intersector.overlaps(Tmp.cr1.set(srcx, srcy, range), other.getHitbox(Tmp.r1));
    }

    protected boolean overlaps(Building src, Building other, float range){
        return overlaps(src.x, src.y, other.tile(), range);
    }

    protected boolean overlaps(Tile src, Tile other, float range){
        return overlaps(src.drawx(), src.drawy(), other, range);
    }

    public boolean overlaps(@Nullable Tile src, @Nullable Tile other){
        if(src == null || other == null) return true;
        return Intersector.overlaps(Tmp.cr1.set(src.worldx() + offset, src.worldy() + offset, laserRange * tilesize), Tmp.r1.setSize(size * tilesize).setCenter(other.worldx() + offset, other.worldy() + offset));
    }

    public class SoulTransmitterBuild extends SoulBuild{
        public DistanceConnector<SoulGraph> connector;

        @Override
        public void onGraphInit(){
            super.onGraphInit();

            connector = graphConnector(Graphs.soul, DistanceConnector.class);
            if(connector == null) throw new IllegalStateException("No distance connector set up.");
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            Lines.stroke(1f, Pal.accent);
            Drawf.circles(x, y, laserRange * tilesize);
            Draw.reset();
        }

        @Override
        public void drawConfigure(){
            for(var e : connector.connections){
                var other = e.other(connector).node().build();
                Tmp.v1.set(other.x, other.y).sub(x, y).limit(block.size * tilesize / 2f + 1.5f);
                float sx = x + Tmp.v1.x, sy = y + Tmp.v1.y;
                float ex = other.x, ey = other.y;

                Color color = e.n2 == connector ? Pal.accent : Pal.place;
                float angle = e.n2 == connector
                ? Angles.angle(sx, sy, ex, ey)
                : Angles.angle(ex, ey, sx, sy);

                float in = (Time.time % 72f) / 72f;
                if(e.n2 == connector){
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

            Lines.stroke(1f, Pal.accent);
            Drawf.circles(x, y, laserRange * tilesize);
            Drawf.circles(x, y, block.size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(other instanceof GraphBuild && linkValid(this, other.as())){
                configure(other.pos());
                return false;
            }

            if(this == other){
                if(connector.validConnections() > 0) connector.disconnect();
                return false;
            }

            return true;
        }

        @Override
        public IntSeq config(){
            IntSeq seq = new IntSeq();
            for(var conn : connector.distConnections){
                if(conn == null) continue;

                var p2 = conn.relpos;
                if(p2.x == 0 || p2.y == 0) continue;

                seq.add(p2.pack(), conn.isN2 ? 1 : 0);
            }

            return seq;
        }
    }
}
