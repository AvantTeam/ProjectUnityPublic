package unity.world.graph.nodes;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.util.*;
import unity.world.blocks.power.SoulTransmitter.*;
import unity.world.graph.GraphBlock.*;

import static mindustry.Vars.*;
import static unity.graphics.MonolithPal.*;

/** @author GlennFolker */
public class SoulTransmitterNodeType extends SoulNodeType{
    public float laserWidth = 2f, laserWidthInner = 1f;
    public Color laserColor = monolithLight.cpy().a(0.5f), laserColorInner = monolithLighter.cpy().a(0.67f);

    public Color trailColor = monolithLighter;
    public Func2<SoulTransmitterBuild, SoulTransmitterNode, Trail> trailType = (build, node) -> new Trail(6);
    public float trailInterval = 60f;

    @Override
    public <E extends Building & GraphBuild> SoulTransmitterNode create(E build){
        return new SoulTransmitterNode(build);
    }

    public class SoulTransmitterNode extends SoulNode{
        public float totalTransmit;
        public float on;

        public <E extends Building & GraphBuild> SoulTransmitterNode(E build){
            super(build);
        }

        @Override
        public void update(){
            float last = visualTransferred;
            super.update();

            totalTransmit += Mathf.log(1.5f, visualTransferred - last + 1f) * Time.delta;
            on = Mathf.approachDelta(on, transferred > 0f ? 1f : 0f, 0.07f);
        }

        @Override
        public void draw(){
            SoulTransmitterBuild build = build();
            super.draw();

            float z = Draw.z();
            Draw.z(Layer.power + 0.01f);

            for(var e : build.connector.connections){
                if(e.n2 != build.connector && e.n2.node() instanceof SoulTransmitterNode) continue;
                var other = e.other(build.connector).node().build();

                Tmp.v1.set(other).sub(build).setLength(build.block.size * tilesize / 2f - 1.5f);
                float sx = build.x + Tmp.v1.x, sy = build.y + Tmp.v1.y;

                Tmp.v1.set(build).sub(other).setLength(other.block.size * tilesize / 2f - 1.5f);
                float ex = other.x + Tmp.v1.x, ey = other.y + Tmp.v1.y;

                Lines.stroke(laserWidth, laserColor);
                Draw.alpha(0.33f + 0.67f * on);
                DrawUtils.line(sx, sy, ex, ey);

                Lines.stroke(laserWidthInner, laserColorInner);
                Draw.alpha(0.3f + 0.67f * on);
                DrawUtils.line(sx, sy, ex, ey);
            }

            Draw.reset();
            Draw.z(z);
        }
    }
}
