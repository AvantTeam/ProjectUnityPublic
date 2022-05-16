package unity.world.draw;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import unity.world.blocks.exp.*;

import static arc.Core.*;

//draws level-specific regions over the default draw.
public class DrawOver extends DrawLevel{
    public TextureRegion[] levelRegions;
    public float layer;

    public DrawOver(float layer){
        this.layer = layer;
    }

    public DrawOver(){
        this(Layer.blockOver);
    }

    @Override
    public void load(Block block){
        int n = 1;
        while(n <= 100){ //worst-case scenario
            TextureRegion t = atlas.find(block.name + n);
            if(!t.found()) break;
            n++;
        }
        if(n > 1){
            //name+n-1 was the last sprite that was found
            levelRegions = new TextureRegion[n];
            levelRegions[0] = block.region;
            for(int i = 1; i < n; i++){
                levelRegions[i] = atlas.find(block.name + i);
            }
        }
    }

    @Override
    public <T extends Building & LevelHolder> void draw(T build){
        TextureRegion r = levelRegion(build);
        if(r != build.block.region){
            float z = Draw.z();
            Draw.z(layer);
            Draw.rect(r, build.x, build.y, build.block.rotate ? build.rotdeg() : 0);
            Draw.z(z);
        }
    }

    public <T extends Building & LevelHolder> TextureRegion levelRegion(T build){
        if(levelRegions == null) return build.block.region;
        return levelRegions[Math.min((int)(build.levelf() * levelRegions.length), levelRegions.length - 1)];
    }
}
