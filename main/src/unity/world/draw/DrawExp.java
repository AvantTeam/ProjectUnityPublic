package unity.world.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;
import unity.graphics.*;
import unity.world.blocks.exp.*;

import static arc.Core.atlas;

public class DrawExp extends DrawBlock {
    public TextureRegion exp, top;
    public float glowAmount = 0.9f, glowScale = 8f;
    public Color flame = Color.yellow;

    @Override
    public void draw(Building build){
        Draw.rect(build.block.region, build.x, build.y);
        if(exp.found() && build instanceof KoruhCrafter.KoruhCrafterBuild kr){
            Draw.color(UnityPal.exp, Color.white, Mathf.absin(20, 0.6f));
            Draw.alpha(kr.expf());
            Draw.rect(exp, build.x, build.y);
        }

        if(top.found()){
            Draw.z(Layer.blockOver);
            Draw.color(flame);
            Draw.alpha(Mathf.absin(build.totalProgress(), glowScale, glowAmount) * build.warmup());
            Draw.rect(top, build.x, build.y);
        }
        Draw.reset();
    }

    @Override
    public void load(Block block){
        exp = atlas.find(block.name + "-exp");
        top = atlas.find(block.name + "-top");
    }
}
