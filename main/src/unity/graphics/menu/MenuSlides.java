package unity.graphics.menu;

import arc.math.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.world.*;
import unity.content.blocks.*;

public class MenuSlides{
    public static MenuSlide

    stone = new MenuSlide(false){
        @Override
        protected void generate(Tiles tiles){
            boolean tech = Mathf.chance(0.25);
            for(int x = 0; x < tiles.width; x++){
                for(int y = 0; y < tiles.height; y++){
                    Block floor = Blocks.basalt;
                    Block wall = Blocks.air;

                    if(Simplex.noise2d(seed + 1, 3, 0.5, 1/20.0, x, y) > 0.4){
                        floor = Blocks.stone;
                    }

                    if(Simplex.noise2d(seed + 1, 3, 0.3, 1/20.0, x, y) > 0.5){
                        wall = Blocks.stoneWall;
                    }

                    if(tech){
                        int mx = x % 10, my = y % 10;
                        int sclx = x / 10, scly = y / 10;
                        if(Simplex.noise2d(seed + 2, 2, 1f / 10f, 1f, sclx, scly) > 0.6f && (mx == 0 || my == 0 || mx == 9 || my == 9)){
                            floor = Blocks.darkPanel3;
                            if(Mathf.dst(mx, my, 5, 5) > 6f){
                                floor = Blocks.darkPanel4;
                            }

                            if(wall != Blocks.air && Mathf.chance(0.7)){
                                wall = Blocks.darkMetal;
                            }
                        }
                    }

                    setTile(x, y, wall, Blocks.air, floor);
                }
            }
        }
    },

    grass = new MenuSlide(false){
        @Override
        protected void generate(Tiles tiles){
            for(int x = 0; x < tiles.width; x++){
                for(int y = 0; y < tiles.height; y++){
                    Block floor = YoungchaBlocks.concreteNumber;
                    Block wall = Blocks.air;

                    if(tiles.get(x, y) == null){
                        floor = YoungchaBlocks.concreteFill;
                        if(Mathf.chance(0.1)){
                            floor = YoungchaBlocks.concreteFill;
                        }
                    }

                    boolean c1, c2;
                    c1 = x % 10 == 0;
                    c2 = y % 10 == 0;
                    if(c1 || c2){
                        floor = YoungchaBlocks.concreteStripe;

                        if(c1 && c2){
                            setTile(x + 1, y + 1, Blocks.air, Blocks.air, YoungchaBlocks.concreteNumber);
                            setTile(x + 2, y + 1, Blocks.air, Blocks.air, YoungchaBlocks.concreteNumber);
                        }

                        if(Simplex.noise2d(seed + 3, 2, 0.6, 1/22.0, x, y) > 0.5){
                            wall = Blocks.stoneWall;

                            if(Simplex.noise2d(seed, 2, 0.6, 1/22.0, x, y) > 0.55){
                                wall = Blocks.shrubs;
                            }
                        }
                    }

                    if(Simplex.noise2d(seed, 2, 0.6, 1/22.0, x, y) > 0.5){
                        floor = Blocks.grass;

                        if(Mathf.chance(0.09)){
                            wall = Blocks.pine;
                        }
                    }

                    setTile(x, y, wall, Blocks.air, floor);
                }
            }
        }
    },

    warzone = new MenuSlide(true){
        @Override
        protected void generate(Tiles tiles){
            for(int x = 0; x < tiles.width; x++){
                for(int y = 0; y < tiles.height; y++){
                    Block floor = Blocks.stone;
                    Block wall = Blocks.air;

                    setTile(x, y, wall, Blocks.air, floor);
                }
            }
        }
    };
}
