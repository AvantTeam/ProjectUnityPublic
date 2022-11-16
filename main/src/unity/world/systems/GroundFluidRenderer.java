package unity.world.systems;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.util.*;
import unity.*;
import unity.graphics.*;

import static mindustry.Vars.*;

public class GroundFluidRenderer{
    public FrameBuffer fluidFrameBuffer = new FrameBuffer();
    public FrameBuffer fluidSprites = new FrameBuffer();
    public int fluidSpritesWidth;
    Pixmap fluidDataMap;
    //todo GroundFluidRenderer;

    byte[] bufferDirect;
    public boolean useOldRendering = false;
    boolean initialisedSprites = false;

    private GroundFluidBatch batch;

    public GroundFluidRenderer(){
        batch = new GroundFluidBatch(32, UnityShaders.batchedGroundLiquid);
    }

    public void draw(){
        if(!initialisedSprites){
            initialiseSprites();
        }

        GroundFluidControl control = Unity.groundFluidControl;
        Camera camera = Core.camera;
        float pad = tilesize / 2f;

        int
        minx = (int)((camera.position.x - camera.width / 2f - pad) / tilesize),
        miny = (int)((camera.position.y - camera.height / 2f - pad) / tilesize),
        maxx = Mathf.ceil((camera.position.x + camera.width / 2f + pad) / tilesize),
        maxy = Mathf.ceil((camera.position.y + camera.height / 2f + pad) / tilesize);

        minx = Mathf.clamp(minx, 0, control.w - 1);
        miny = Mathf.clamp(miny, 0, control.h - 1);
        maxx = Mathf.clamp(maxx, minx, control.w - 1);
        maxy = Mathf.clamp(maxy, miny, control.h - 1);

        drawFluidsBatched(minx, miny, maxx, maxy);

        //drawDebug();
    }

    private int[] fluidsToBeDrawn; // records the index in the fbo
    private int[] fluidsByteOffset; // records the index in the pixmap
    private int fluidsToBeDrawnCount;
    private byte[] clearArray;

    public void drawFluidsBatched(int minx, int miny, int maxx, int maxy){
        int batchIndX = 0;
        int batchIndY = 0;
        int bWidth = maxx - minx + 1;
        int bHeight = maxy - miny + 1;
        //reset counters
        if(fluidsToBeDrawn == null){
            fluidsToBeDrawn = new int[GroundFluidControl.liquidProperties.size+1];
            fluidsByteOffset = new int[GroundFluidControl.liquidProperties.size+1];
        }
        fluidsToBeDrawnCount = 0;
        for(int i = 0; i < fluidsToBeDrawn.length; i++){
            fluidsToBeDrawn[i] = -1;
        }
        //count fluids
        GroundFluidControl control = Unity.groundFluidControl;
        int tile, id;
        for(int y = miny; y <= maxy; y++){
            tile = control.tileIndexOf(minx, y);
            for(int x = 0; x <= bWidth; x++){
                id = control.getVisualFluidTypeID(tile);
                if(id == 0){
                    tile++;
                    continue;
                }
                if(fluidsToBeDrawn[id] == -1){
                    fluidsToBeDrawn[id] = fluidsToBeDrawnCount;
                    fluidsToBeDrawnCount++;
                }
                tile++;
            }
        }
        //Log.info("Fluids seen:" + fluidsToBeDrawnCount);
        if(fluidsToBeDrawnCount==0){
            return;
        }

        if(fluidDataMap==null){
            fluidDataMap = new Pixmap(Math.max(bWidth, bHeight),Math.max(bWidth, bHeight));
            fluidFrameBuffer.resizeCheck(fluidDataMap.width, fluidDataMap.height);
            Log.info("Making a new fluid map:" + fluidDataMap.width + "," + fluidDataMap.height);
        }
        // check for capacity
        if((fluidDataMap.width / bWidth) * (fluidDataMap.height / bHeight) < fluidsToBeDrawnCount){
            //if not, resize it
            // get (bWidth*i / bHeight*j) close to as 1 as possible where i*k = fluidsToBeDrawnCount and k>=j but also minimise k
            int currentColumns = (fluidDataMap.width / bWidth);
            int targetColumns = 0, targetRows;
            for(int i = Math.max(1, currentColumns); ; i++){
                int rows = Mathf.ceil((float)fluidsToBeDrawnCount / i);
                //initially it will (usually) be < 1, we stop when the ratio is > 1
                if((float)(i * bWidth) / (float)(rows * bHeight) > 1){
                    targetColumns = i;
                    targetRows = rows;
                    break;
                }
                if(i * bWidth> 4096){
                    targetColumns = i;
                    targetRows = rows;
                    Log.info("ERRORm TOO MUCH LIQUID D: gsize:"+bWidth + " by " + bHeight+": ftbd:"+fluidsToBeDrawnCount);
                    break;
                }
            }
            Log.info("Going to resize fluidFrameBuffer from:" + fluidDataMap.width + "," + fluidDataMap.height);
            Log.info("Target grid size :" + targetColumns + " by " + targetRows);
            Log.info("grid cell size :" + bWidth + " by " + bHeight);
            fluidDataMap.dispose();
            fluidDataMap = new Pixmap(targetColumns*bWidth, targetRows*bHeight);
            fluidFrameBuffer.resizeCheck(fluidDataMap.width, fluidDataMap.height);
            Log.info("Resized fluidFrameBuffer to:" + fluidDataMap.width + "," + fluidDataMap.height);
        }

        //calculate their byte offsets
        int currentColumns = (fluidDataMap.width / bWidth);
        for(int i = 0; i < fluidsToBeDrawn.length; i++){
            int index = fluidsToBeDrawn[i];
            if(index!=-1){
                fluidsByteOffset[i] = ((index%currentColumns) * bWidth + (index/currentColumns) * fluidDataMap.width * bHeight) * 4;
            }
        }

        //erase byte buffer :p (maybe) must be a dangerous operation

        /*
        if(clearArray == null){
            clearArray = new byte[fluidDataMap.width * fluidDataMap.height * 4];
        }
        fluidDataMap.pixels.clear();
        fluidDataMap.pixels.put(clearArray);
        Log.info("cleared pixels with array of length "+clearArray.length);
         */
        fluidDataMap.fill(0);

        //record fluids
        float transam;
        int byteOffsetRow,byteind;
        for(int y = 0; y < bHeight; y++){
            tile = control.tileIndexOf(minx, y+miny);
            byteOffsetRow = (y * fluidDataMap.width) * 4;
            for(int x = 0; x < bWidth; x++){
                id = control.getVisualFluidTypeID(tile);
                if(id != 0){
                    byteind = fluidsByteOffset[id] + byteOffsetRow;
                    fluidDataMap.pixels.put(byteind, transformDepth(control.previousLiquidAmount[tile]));
                    fluidDataMap.pixels.put(byteind+3, transformDepth(control.liquidAmount[tile]));

                    transam = 1f / (1f + Math.max(control.previousLiquidAmount[tile], control.liquidAmount[tile]));
                    fluidDataMap.pixels.put(byteind+1,toByte(transformVel(control.fluidVelX(tile) * transam)));
                    fluidDataMap.pixels.put(byteind+2,toByte(transformVel(control.fluidVelY(tile) * transam)));
                }else{
                    //think of a way to clear.. stuff?
                }
                tile++;
                byteOffsetRow+=4;
            }
        }
        //upload fluids.
        fluidFrameBuffer.begin(Color.clear);
        fluidFrameBuffer.getTexture().draw(fluidDataMap);
        fluidFrameBuffer.end();


        //draw to SCREEN! >:D
        Batch current = Core.batch;
        Core.batch = batch;
        Draw.proj(Core.camera);
        for(int i = 0; i < fluidsToBeDrawn.length; i++){
            int index = fluidsToBeDrawn[i];
            if(index != -1){
                var prop = GroundFluidControl.liquidProperties.get(i-1);
                Draw.color(prop.shallowColor);
                batch.drawGroundFluid(fluidFrameBuffer.getTexture(),(index%currentColumns) * bWidth, (index/currentColumns) * bHeight, bWidth, bHeight,minx,miny, i);
            }
        }
        prevChunkW = bWidth;
        prevChunkH = bHeight;
        prevMinX = minx;
        prevMinY = miny;
        Draw.flush();
        Core.batch = current;
    }
    public float prevChunkW;
    public float prevChunkH;
    public float prevMinX;
    public float prevMinY;


    public void drawDebug(){
        GroundFluidControl control = Unity.groundFluidControl;
        Camera camera = Core.camera;
        float pad = tilesize / 2f;
        int
        minx = (int)((camera.position.x - camera.width / 2f - pad) / tilesize),
        miny = (int)((camera.position.y - camera.height / 2f - pad) / tilesize),
        maxx = Mathf.ceil((camera.position.x + camera.width / 2f + pad) / tilesize),
        maxy = Mathf.ceil((camera.position.y + camera.height / 2f + pad) / tilesize);

        minx = Mathf.clamp(minx, 0, control.w - 1);
        miny = Mathf.clamp(miny, 0, control.h - 1);
        maxx = Mathf.clamp(maxx, minx, control.w - 1);
        maxy = Mathf.clamp(maxy, miny, control.h - 1);

        float t = Mathf.clamp(control.getTransition());
        Lines.stroke(0.5f);
        for(int y = miny; y <= maxy; y++){
            int tile = control.tileIndexOf(minx, y);
            for(int x = minx; x <= maxx; x += 1){
                if(control.liquidAmount[tile] + control.previousLiquidAmount[tile] > 0.001){
                    float tx = x * tilesize;
                    float ty = y * tilesize;
                    float transam = 1f / (1f + Mathf.lerp(control.previousLiquidAmount[tile], control.liquidAmount[tile], t));
                    Lines.stroke(0.5f);
                    Lines.line(tx, ty, tx + control.fluidVelX(tile), ty + control.fluidVelY(tile));
                    Lines.stroke(0.3f);
                    Lines.line(tx, ty, tx + control.fluidVelX(tile) * transam, ty + control.fluidVelY(tile) * transam);
                }
                tile += 1;
            }
        }
        Lines.stroke(1);

    }

    public void initialiseSprites(){
        var g_liquids = GroundFluidControl.liquidProperties;
        fluidSpritesWidth = (int)Math.max(Mathf.ceil(Mathf.sqrt(g_liquids.size*2 + 2)), 2);
        fluidSprites.resize(fluidSpritesWidth * 96, fluidSpritesWidth * 96);
        fluidSprites.begin(Color.clear);
        Draw.proj(0, 0, fluidSprites.getWidth(), fluidSprites.getHeight());
        Draw.color();
        Draw.blend(Blending.disabled);
        TextureRegion n = Core.atlas.find("unity-ground-liquid-noise");
        Draw.rect(n, (0.5f) * 96, (0.5f) * 96, 96, 96);

        for(int i = 0; i < g_liquids.size; i++){
            int x = ((i + 1)*2) % fluidSpritesWidth;
            int y = ((i + 1)*2) / fluidSpritesWidth;
            int x2 = ((i + 1)*2+1) % fluidSpritesWidth;
            int y2 = ((i + 1)*2+1) / fluidSpritesWidth;
            TextureRegion tr = Core.atlas.find("unity-ground-liquid-" + g_liquids.get(i).name);
            TextureRegion tr2 = Core.atlas.find("unity-ground-liquid-" + g_liquids.get(i).name+"-data");
            if(tr == null){
                Draw.color(g_liquids.get(i).shallowColor);
                Fill.crect(x * 96, y * 96, 96, 96);
                continue;
            }
            Draw.rect(tr, (x + 0.5f) * 96, (y + 0.5f) * 96, 96, 96);
            Draw.rect(tr2, (x2 + 0.5f) * 96, (y2 + 0.5f) * 96, 96, 96);
        }
        fluidSprites.end();
        Draw.proj(Core.camera);
        initialisedSprites = true;
        Draw.blend();
    }

    //normalises flow velocity
    private static float transformVel(float v){
        return Mathf.clamp(v * 0.5f + 0.5f);
    }

    //makes liquid depth a hyperbolic value, graphical cutoff is 0.2
    private static float transformDepth(float x, float x2, float min, float trans){
        float c = -min * 20;
        float a = 0.25f / (-c);
        return Mathf.lerp(x == 0 ? 0 : (1f - 1f / (a * x + 1 - a * c)), x2 == 0 ? 0 : (1f - 1f / (a * x2 + 1 - a * c)), trans);
    }

    private static float transformDepth(float x, float min){
        float a = 0.25f / (min * 20);
        return x <= 0 ? 0 : (1f - 1f / (a * x + 1.25f));
    }
    //min being 0.02
    private static byte transformDepth(float x){
        return x <= 0 ? 0 : (byte)(255*Mathf.clamp(1f - 1f / (0.625f * x + 1.25f)));
    }

    private static byte toByte(float x){
        return (byte)(x * 255);
    }

    private static void clearPixel(byte[] data, int index){
        data[index] = 0;
        data[index + 1] = 0;
        data[index + 2] = 0;
        data[index + 3] = 0;
    }

    private static void writePixel(byte[] data, int index, Color c){
        data[index] = (byte)(c.r * 255);
        data[index + 1] = (byte)(c.g * 255);
        data[index + 2] = (byte)(c.b * 255);
        data[index + 3] = (byte)(c.a * 255);
    }
}
