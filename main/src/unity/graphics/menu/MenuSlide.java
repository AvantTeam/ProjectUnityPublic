package unity.graphics.menu;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.world.*;

import static mindustry.Vars.*;
public class MenuSlide implements Disposable{
    protected CacheBatch batch;
    protected FrameBuffer shadows;
    protected Mat mat = new Mat();
    protected Camera camera = new Camera();
    protected TextureRegion shadowTexture;

    protected int width, height;


    protected int cacheFloor, cacheWall;

    public void generateWorld(int width, int height){
        this.width = width;
        this.height = height;
        world.beginMapLoad();
        Tiles tiles = world.resize(width, height);
        shadows = new FrameBuffer(width, height);

        generate(tiles);
        world.endMapLoad();
        cache();
    };

    protected void generate(Tiles tiles){
        for(int x = 0; x < tiles.width; x++){
            for(int y = 0; y < tiles.height; y++){
                Block floor = Blocks.air;
                Block wall =  Blocks.air;
                Block overlay = Blocks.air;

                Tile tile;
                tiles.set(x, y, (tile = new CachedTile()));
                tile.x = (short)x;
                tile.y = (short)y;

                tile.setFloor(floor.asFloor());
                tile.setBlock(wall);
                tile.setOverlay(overlay);
            }
        }
    };

    protected void cache(){
        // draw shadows
        Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());
        shadows.begin(Color.clear);
        Draw.color(Color.black);

        for(Tile tile : world.tiles){
            if(tile.block() != Blocks.air){
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            }
        }
        Draw.color();
        shadows.end();

        Batch prev = Core.batch;

        // draw floors and overlays
        Core.batch = batch = new CacheBatch(new SpriteCache(width * height * 6, false));
        batch.beginCache();

        for(Tile tile : world.tiles){
            tile.floor().drawBase(tile);
            tile.overlay().drawBase(tile);
        }
        cacheFloor = batch.endCache();
        batch.beginCache();

        // draw walls
        for(Tile tile : world.tiles){
            tile.block().drawBase(tile);
        }
        cacheWall = batch.endCache();

        Core.batch = prev;

        // cache shadows
        shadowTexture = Draw.wrap(shadows.getTexture());
    };

    public void render(float time, float duration, int viewWidth, int viewHeight){
        float movement = width / duration;
        float scaling = Math.max(Scl.scl(4f), Math.max(Core.graphics.getWidth() / ((viewWidth - 1f) * tilesize), Core.graphics.getHeight() / ((viewHeight - 1f) * tilesize)));
        camera.position.set(viewWidth * tilesize / 2f + movement * time, viewHeight * tilesize / 2f);
        camera.resize(Core.graphics.getWidth() / scaling,
        Core.graphics.getHeight() / scaling);

        mat.set(Draw.proj());
        Draw.flush();
        Draw.proj(camera);
        batch.setProjection(camera.mat);
        batch.beginDraw();
        batch.drawCache(cacheFloor);
        batch.endDraw();
        Draw.color();
        Draw.rect(Draw.wrap(shadows.getTexture()),
        width * tilesize / 2f - 4f, height * tilesize / 2f - 4f,
        width * tilesize, -height * tilesize);
        Draw.flush();
        batch.beginDraw();
        batch.drawCache(cacheWall);
        batch.endDraw();

        Draw.proj(mat);
        Draw.color(0f, 0f, 0f, 0.3f);
        Fill.crect(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        Draw.color();
    };

    @Override
    public void dispose(){
        batch.dispose();
        shadows.dispose();
    }
}
