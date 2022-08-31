package unity.world.blocks.defense;

import arc.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.util.*;
import unity.world.blocks.*;
import unity.world.blocks.envrionment.*;

import static mindustry.Vars.*;
import static unity.world.blocks.distribution.HeatPipe.baseColor;

public class HeatWall extends GenericGraphBlock{
    TextureRegion[] wallBorder;
    TextureRegion[] wallBorderHeat;
    public String borderTexturename;
    public HeatWall(String name){
        super(name);
        solid = true;
        destructible = true;
        group = BlockGroup.walls;
        buildCostMultiplier = 4f;
        canOverdrive = false;
        drawDisabled = false;
        crushDamageMultiplier = 5f;
        priority = TargetPriority.wall;
    }

    @Override
    public void load(){
        super.load();
        if(borderTexturename==null){
            borderTexturename = name;
        }
        wallBorder = GraphicUtils.getRegions(Core.atlas.find(borderTexturename+"-border"), 12, 4,32);
        wallBorderHeat = GraphicUtils.getRegions(Core.atlas.find(borderTexturename+"-border-heat"), 12, 4,32);
    }

    Tile[][] grid = new Tile[3][3];

    public class HeatWallBuild extends GenericGraphBuild implements CustomGroundFluidTerrain{
        boolean needsUpdating = true;

        public void onInit(){
            Events.on(EventType.TileChangeEvent.class, o -> {needsUpdating = true;});
            indexes = new int[size][size];
        }
        int[][] indexes;
        void updateIndexes(){
            int offset = (size-1)/2;
            for(int ax = 0; ax<size; ax++){
                for(int ay = 0; ay<size; ay++){
                    for(int i = 0; i < 3; i++){
                        for(int j = 0; j < 3; j++){
                            grid[i][j] = world.tile(i + tile.x - 1 - offset +ax, j + tile.y - 1 - offset+ay);
                        }
                    }
                    int index = TilingUtils.getTilingIndex(grid, 1, 1, t -> {
                        return t != null && t.build != null && t.block() instanceof HeatWall;
                    });
                    indexes[ax][ay] = index;
                }
            }
            needsUpdating = false;
        }

        @Override
        public void draw(){
            Draw.rect(region,x,y);
            heatNode().heatColor(Tmp.c1);
            Tmp.c1.add(baseColor);

            if(needsUpdating){
                updateIndexes();
            }

            int tx = tile.x-(size-1)/2;
            int ty = tile.y-(size-1)/2;
            for(int ax = 0; ax<size; ax++){
                for(int ay = 0; ay<size; ay++){
                    Draw.color();
                    Draw.rect(wallBorder[indexes[ax][ay]], (tx+ax)*tilesize, (ty+ay)*tilesize);
                    Draw.color(Tmp.c1);
                    Draw.rect(wallBorderHeat[indexes[ax][ay]], (tx+ax)*tilesize, (ty+ay)*tilesize);
                }
            }
            drawTeamTop();
        }

        @Override
        public float terrainHeight(){
            return 5*size;
        }
    }
}
