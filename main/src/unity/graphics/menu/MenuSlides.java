package unity.graphics.menu;

import mindustry.content.*;
import mindustry.world.*;

public class MenuSlides{
    public static MenuSlide

    stone = new MenuSlide(){
        @Override
        protected void generate(Tiles tiles){
            for(int x = 0; x < tiles.width; x++){
                for(int y = 0; y < tiles.height; y++){
                    Block floor = Blocks.stone;
                    Block wall =  Blocks.air;

                    if(x % 10 == 0){
                        wall = Blocks.stoneWall;
                    }

                    Tile tile;
                    tiles.set(x, y, (tile = new CachedTile()));
                    tile.x = (short)x;
                    tile.y = (short)y;

                    tile.setFloor(floor.asFloor());
                    tile.setBlock(wall);
                    tile.setOverlay(Blocks.air);
                }
            }
        }
    },

    grass = new MenuSlide(){
        @Override
        protected void generate(Tiles tiles){
            for(int x = 0; x < tiles.width; x++){
                for(int y = 0; y < tiles.height; y++){
                    Block floor = Blocks.grass;
                    Block wall =  Blocks.air;

                    if(x % 10 == 0 || y % 10 == 0){
                        wall = Blocks.pine;
                    }

                    Tile tile;
                    tiles.set(x, y, (tile = new CachedTile()));
                    tile.x = (short)x;
                    tile.y = (short)y;

                    tile.setFloor(floor.asFloor());
                    tile.setBlock(wall);
                    tile.setOverlay(Blocks.air);
                }
            }
        }
    };
}
