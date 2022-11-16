package unity.ui;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.world.*;
import unity.util.*;

import static mindustry.Vars.*;

public class UnityMinimapRenderer extends MinimapRenderer{
    public Pixmap privatePixmap;
    public Texture privateTexture;
    public UnityMinimapRenderer(){
        super();
    }

    @Override
    public void reset(){
        super.reset();
        privatePixmap = ReflectUtils.getFieldValue(this,ReflectUtils.getField((MinimapRenderer)this,"pixmap"));
        privateTexture = ReflectUtils.getFieldValue(this,ReflectUtils.getField((MinimapRenderer)this,"texture"));
    }

    @Override
    public void update(Tile tile){
        if(world.isGenerating() || !state.isGame()) return;

        if(tile.build != null && tile.isCenter()){
            tile.getLinkedTiles(other -> {
                if(!other.isCenter()){
                    updatePixel(other);
                }

                if(tile.block().solid && other.y > 0){
                    Tile low = world.tile(other.x, other.y - 1);
                    if(!low.solid()){
                        updatePixel(low);
                    }
                }
            });
        }

        updatePixel(tile);
    }
    void updatePixel(Tile tile){
        int color = colorFor(tile);
        privatePixmap.set(tile.x, privatePixmap.height - 1 - tile.y, color);

        Pixmaps.drawPixel(privateTexture, tile.x, privatePixmap.height - 1 - tile.y, color);
    }


    private Block realBlock(Tile tile){
        //TODO doesn't work properly until player goes and looks at block
        return tile.build == null ? tile.block() : state.rules.fog && !tile.build.wasVisible ? Blocks.air : tile.block();
    }

    private int colorFor(Tile tile){
        if(tile == null) return 0;
        Block real = realBlock(tile);
        int bc = real.minimapColor(tile);

        Color color = Tmp.c1.set(bc == 0 ? MapIO.colorFor(real, tile.floor(), tile.overlay(), tile.team()) : bc);
        color.mul(1f - Mathf.clamp(world.getDarkness(tile.x, tile.y) / 4f));

        if(real == Blocks.air && tile.y < world.height() - 1 && realBlock(world.tile(tile.x, tile.y + 1)).solid){
            color.mul(0.7f);
        }else if(tile.floor().isLiquid && (tile.y >= world.height() - 1 || !world.tile(tile.x, tile.y + 1).floor().isLiquid)){
            color.mul(0.84f, 0.84f, 0.9f, 1f);
        }

        return color.rgba();
    }
}
