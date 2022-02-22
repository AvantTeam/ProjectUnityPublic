package unity.world.draw;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.world.blocks.exp.*;

public class DrawLevel {
    /** Draws before the block's draw. */
    public <T extends Building & LevelHolder> void draw(T build){
        //Draw.rect(build.block.region, build.x, build.y, build.block.rotate ? build.rotdeg() : 0);
    }

    /** Draws any extra light for the block. */
    public <T extends Building & LevelHolder> void drawLight(T build){

    }

    /** Load any relevant texture regions. */
    public void load(Block block){

    }
}
