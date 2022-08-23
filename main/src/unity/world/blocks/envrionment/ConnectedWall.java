package unity.world.blocks.envrionment;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import unity.util.*;

import static mindustry.Vars.world;

public class ConnectedWall extends StaticWall{
    TextureRegion[] tiles;
    public ConnectedWall(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        tiles = GraphicUtils.getRegions(Core.atlas.find(name+"-tiles"), 12, 4,32);
    }
    @Override
    public void drawBase(Tile tile){
        Tile[][] grid = new Tile[3][3];
        int avail = 0;
        for(int i = 0; i<3; i++){
            for(int j = 0; j<3; j++){
                grid[i][j] = world.tile(i+tile.x-1, j+tile.y-1);
                if(grid[i][j]!=null){
                    avail++;
                }
            }
        }
        int index = TilingUtils.getTilingIndex(grid,1,1,t->{
            return t !=null && t.block() == ConnectedWall.this;
        });
        if(avail==0){
            Draw.rect(region, tile.worldx(), tile.worldy());
        }else{
            Draw.rect(tiles[index], tile.worldx(), tile.worldy());
        }
        if(tile.overlay().wallOre){
           tile.overlay().drawBase(tile);
       }
    }
}
