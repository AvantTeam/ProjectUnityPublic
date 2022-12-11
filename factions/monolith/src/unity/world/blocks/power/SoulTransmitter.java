package unity.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.gen.graph.*;
import unity.world.graph.nodes.SoulNodeType.*;

import static mindustry.Vars.*;

/** @author GlennFolker */
@SuppressWarnings("unchecked")
public class SoulTransmitter extends SoulBlock{
    public float laserRange = 8f;

    public SoulTransmitter(String name){
        super(name);
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
            ((SoulNode)soulNode).drawConfigure();

            Lines.stroke(1f, Pal.accent);
            Drawf.circles(x, y, laserRange * tilesize);
            Drawf.circles(x, y, block.size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));
            Draw.reset();
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            return ((SoulNode)soulNode).onConfigureBuildTapped(other);
        }
    }
}
