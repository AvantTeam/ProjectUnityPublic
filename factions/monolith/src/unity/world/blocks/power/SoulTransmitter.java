package unity.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;
import unity.gen.graph.*;
import unity.world.graph.*;
import unity.world.graph.connectors.*;
import unity.world.graph.nodes.SoulNodeType.*;
import unity.world.meta.*;

import static mindustry.Vars.*;

/** @author GlennFolker */
@SuppressWarnings("unchecked")
public class SoulTransmitter extends SoulBlock{
    public float laserRange = 8f;

    public SoulTransmitter(String name){
        super(name);
    }

    @Override
    public void init(){
        super.init();
        clipSize = Math.max(clipSize, laserRange * tilesize + 12f);
    }

    @Override
    public void setStats(){
        super.setStats();

        var conn = (DistanceConnectorType<SoulGraph>)soulConnectorConfigs.find(DistanceConnectorType.class::isInstance);
        if(conn == null) return;

        stats.add(PUStat.soulRange, laserRange, StatUnit.blocks);
        stats.add(PUStat.soulConnections, conn.connections, StatUnit.none);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Lines.stroke(1f, Pal.accent);
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, laserRange * tilesize);
        Draw.reset();
    }

    public class SoulTransmitterBuild extends SoulBuild{
        @Override
        public void drawSelect(){
            super.drawSelect();

            Lines.stroke(1f, Pal.accent);
            Drawf.circles(x, y, laserRange * tilesize);
            Draw.reset();
        }

        @Override
        public void drawConfigure(){
            soulNode.drawConfigure();

            Lines.stroke(1f, Pal.accent);
            Drawf.circles(x, y, laserRange * tilesize);
            Drawf.circles(x, y, block.size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
            Draw.reset();
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            return soulNode.onConfigureBuildTapped(other);
        }
    }
}
