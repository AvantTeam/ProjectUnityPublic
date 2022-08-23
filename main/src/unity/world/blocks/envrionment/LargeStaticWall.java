package unity.world.blocks.envrionment;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.world;

public class LargeStaticWall extends StaticWall{
    int[] size_variants;
    public int maxsize = 1;
    public TextureRegion[][][] splits;
    public boolean useStochastic = true;
    public int[][] taken;
    public int[][] takensize;

    public LargeStaticWall(String name){
        super(name);
        Events.on(EventType.WorldLoadEvent.class,e->{
            taken = new int[world.width()][world.height()];
            takensize= new int[world.width()][world.height()];
        });
    }

    @Override
    public void load(){
        super.load();
        splits = new TextureRegion[maxsize-1][][];
        for(int i=1;i<maxsize;i++){
            splits[i-1] = Core.atlas.find(name+"-"+(i+1)).split(32,32);
        }
    }

    @Override
    public void drawBase(Tile tile){
        boolean drawLarge = false;
        for(int size=maxsize;size>=2;size--){
            if(useStochastic){
                if(eq(tile.x,tile.y,size)){
                    take(tile.x,tile.y,size);
                    Draw.rect(splits[size - 2][0][size - 1], tile.worldx(), tile.worldy());
                    drawLarge = true;
                    break;
                }
                if(taken[tile.x][tile.y]!=0 && takensize[tile.x][tile.y] == size){
                    Draw.rect(splits[size - 2][Point2.x(taken[tile.x][tile.y])][size - 1-Point2.y(taken[tile.x][tile.y])], tile.worldx(), tile.worldy());
                    drawLarge = true;
                    break;
                }
            }else{
                int rx = tile.x / size * size;
                int ry = tile.y / size * size;
                if(eq(rx, ry, size) && Mathf.randomSeed(Point2.pack(rx, ry)) < 0.5){
                    Draw.rect(splits[size - 2][tile.x % size][(size - 1) - tile.y % size], tile.worldx(), tile.worldy());
                    drawLarge = true;
                    break;
                }
            }
        }
        if(!drawLarge){
            Draw.rect(variantRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());
        }

       //draw ore on top
       if(tile.overlay().wallOre){
           tile.overlay().drawBase(tile);
       }
    }
    void take(int rx, int ry, int s){
        if(!(rx < world.width() - 1 && ry < world.height() - 1)){
            return;
        }
        if(taken==null){
            taken = new int[world.width()][world.height()];
            takensize = new int[world.width()][world.height()];
        }
        for(int i = rx; i < rx + s; i++){
            for(int j = ry; j < ry + s; j++){
                taken[i][j] = Point2.pack(i-rx,j-ry);
                takensize[i][j] = s;
            }
        }
    }


    boolean eq(int rx, int ry, int s){
        if(!(rx < world.width() - 1 && ry < world.height() - 1)){
            return false;
        }
        if(useStochastic){
            if(taken == null || taken.length!=world.width()){
                taken = new int[world.width()][world.height()];
                takensize = new int[world.width()][world.height()];
            }
            for(int i = rx; i < rx + s; i++){
                for(int j = ry; j < ry + s; j++){
                    if(world.tile(i, j) == null || world.tile(i, j).block() != this || taken[i][j]!=0){
                        return false;
                    }
                }
            }
            return true;
        }else{
            //how vanilla does it.
            for(int i = rx; i < rx + s; i++){
                for(int j = ry; j < ry + s; j++){
                    if(world.tile(i, j).block() != this){
                        return false;
                    }
                    for(int size = s + 1; size <= maxsize; size++){
                        int cx = i / size * size;
                        int cy = j / size * size;
                        if(size % s == 0){
                            continue;
                        }
                        if(Mathf.randomSeed(Point2.pack(cx, cy)) < 0.5 && eq(cx, cy, size)){
                            return false;
                        }
                    }

                }
            }
            return true;
        }
    }
}
