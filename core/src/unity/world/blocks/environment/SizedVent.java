package unity.world.blocks.environment;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

/**
 * A {@link SteamVent} that can be of any size. Spans multiple tiles; only the middle tile (or in case of {@code size % 2 == 0},
 * the bottom-left middle) should update and draw the actual sprite.
 * @author GlennFolker
 */
//TODO doesn't really properly check for tile clearance in renderUpdate()
public class SizedVent extends Floor{
    private static Point2[][] offsets = {};

    static{
        checkOffsets(3);
    }

    public SizedVent(String name){
        super(name);
        variants = 3;
    }

    public Block parent = Blocks.air;
    public Effect effect = Fx.ventSteam;
    public Color effectColor = Pal.vent;
    public float effectSpacing = 15f;

    @Override
    public void drawBase(Tile tile){
        parent.drawBase(tile);
        if(checkAdjacent(tile)){
            float x = tile.worldx(), y = tile.worldy();
            if(size % 2 == 0){
                x += tilesize / 2f;
                y += tilesize / 2f;
            }

            Mathf.rand.setSeed(tile.pos());
            Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], x, y);
        }
    }

    @Override
    public boolean updateRender(Tile tile){
        return checkAdjacent(tile);
    }

    @Override
    public void renderUpdate(UpdateRenderState state){
        Tile tile = state.tile;
        if(clear(tile) && (state.data += Time.delta) >= effectSpacing){
            float x = tile.worldx(), y = tile.worldy();
            if(size % 2 == 0){
                x += tilesize / 2f;
                y += tilesize / 2f;
            }

            effect.at(x, y, effectColor);
            state.data = 0f;
        }
    }

    public boolean clearSingle(int x, int y){
        Tile tile = world.tile(x, y);
        return tile != null && tile.block() == Blocks.air;
    }

    public boolean clear(Tile tile){
        return switch(size){
            case 1 -> tile.block() == Blocks.air;
            case 2 -> tile.block() == Blocks.air &&
            clearSingle(tile.x, tile.y + 1) &&
            clearSingle(tile.x + 1, tile.y) &&
            clearSingle(tile.x + 1, tile.y + 1);

            default -> {
                for(Point2 point : getOffsets((size - 1) % 2 + 1)){
                    Tile other = world.tile(tile.x + point.x, tile.y + point.y);
                    if(other != null && other.block() != Blocks.air) yield false;
                }

                yield true;
            }
        };
    }

    public boolean checkAdjacent(Tile tile){
        for(Point2 point : getOffsets(size)){
            Tile other = world.tile(tile.x + point.x, tile.y + point.y);
            if(other == null || other.floor() != this) return false;
        }

        return true;
    }

    private static void checkOffsets(int size){
        int index = size - 1;
        if(offsets.length <= index){
            Point2[][] prev = offsets;
            int prevLen = prev.length;

            offsets = new Point2[size][];
            System.arraycopy(prev, 0, offsets, 0, prevLen);

            for(int i = prevLen; i < size; i++) offsets[i] = createOffsets(i + 1);
        }
    }

    private static Point2[] createOffsets(int size){
        if(size == 0) throw new IllegalArgumentException("Size must be at least 1 (given: " + size + ").");
        if(size == 1) return new Point2[]{new Point2(0, 0)}; // Don't bother calculating if it's just 1.

        int offset = (size - 1) / 2;

        Point2[] out = new Point2[size * size];
        for(int y = 0; y < size; y++){
            int row = y * size;
            for(int x = 0; x < size; x++) out[row + x] = new Point2(x - offset, y - offset);
        }

        return out;
    }

    public static Point2[] getOffsets(int size){
        checkOffsets(size);
        return offsets[size - 1];
    }
}
