package unity.world.systems;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
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
    public boolean useTemp = false;
    boolean initialisedSprites = false;

    public void draw(){
        if(!initialisedSprites){
            initialiseSprites();
        }
        boolean resized = fluidFrameBuffer.resizeCheck(world.width(),world.height());
        if(fluidDataMap == null || resized){
            if(fluidDataMap!=null){
                fluidDataMap.dispose();
            }
            fluidDataMap = new Pixmap(world.width(),world.height());
        }
        GroundFluidControl control = Unity.groundFluidControl;
        Camera camera = Core.camera;
        float pad = tilesize/2f;

        int
            minx = (int)((camera.position.x - camera.width/2f - pad) / tilesize),
            miny = (int)((camera.position.y - camera.height/2f - pad) / tilesize),
            maxx = Mathf.ceil((camera.position.x + camera.width/2f + pad) / tilesize),
            maxy = Mathf.ceil((camera.position.y + camera.height/2f + pad) / tilesize);

        minx = Mathf.clamp(minx,0,control.w-1);
        miny = Mathf.clamp(miny,0,control.h-1);
        maxx = Mathf.clamp(maxx,minx,control.w-1);
        maxy = Mathf.clamp(maxy,miny,control.h-1);

        Color tmpCol = new Color();
        bufferDirect = new byte[(maxx-minx+1)*4];
        //red - type, green- X vel, blue - Y vel, alpha - depth.
        float t = Mathf.clamp(control.getTransition());
        float transam;
        for(int y=miny;y<=maxy;y++){
            int tile = control.tileIndexOf(minx,y);
            for(int x=0;x<bufferDirect.length;x+=4){
                if(control.liquidAmount[tile]+control.previousLiquidAmount[tile]>0.001){
                    var fluid = control.getVisualFluidType(tile);
                    tmpCol.r(fluid.id/256f);
                    transam = 1f/(1f+Mathf.lerp(control.previousLiquidAmount[tile],control.liquidAmount[tile],t));
                    tmpCol.g(transformVel(control.fluidVelX(tile) * transam));
                    tmpCol.b(transformVel(control.fluidVelY(tile) * transam));
                    tmpCol.a(transformDepth(control.previousLiquidAmount[tile],control.liquidAmount[tile],fluid.minamount*0.5f,t));
                    writePixel(bufferDirect,x,tmpCol);
                }else{
                    clearPixel(bufferDirect,x);
                }
                tile+=1; // tile index advances along the row
            }
            int byteOffset = (minx + y * fluidDataMap.width) * 4; //the offset of the raw byte data in the texture
            for(int i=0;i<bufferDirect.length;i++){
                fluidDataMap.pixels.put(byteOffset+i,bufferDirect[i]);
            }
        }
        fluidFrameBuffer.begin(Color.clear);
        fluidFrameBuffer.getTexture().draw(fluidDataMap);
        fluidFrameBuffer.end();

        fluidFrameBuffer.getTexture().setFilter(TextureFilter.linear);

        Draw.shader(UnityShaders.groundLiquid);
        Draw.fbo(fluidFrameBuffer.getTexture(), world.width(), world.height(), tilesize, tilesize*0.5f);
        Draw.shader();

        //drawDebug();
    }

    public void drawDebug(){
        GroundFluidControl control = Unity.groundFluidControl;
        Camera camera = Core.camera;
        float pad = tilesize/2f;
        int
            minx = (int)((camera.position.x - camera.width/2f - pad) / tilesize),
            miny = (int)((camera.position.y - camera.height/2f - pad) / tilesize),
            maxx = Mathf.ceil((camera.position.x + camera.width/2f + pad) / tilesize),
            maxy = Mathf.ceil((camera.position.y + camera.height/2f + pad) / tilesize);

        minx = Mathf.clamp(minx,0,control.w-1);
        miny = Mathf.clamp(miny,0,control.h-1);
        maxx = Mathf.clamp(maxx,minx,control.w-1);
        maxy = Mathf.clamp(maxy,miny,control.h-1);

        float t = Mathf.clamp(control.getTransition());
        Lines.stroke(0.5f);
        for(int y=miny;y<=maxy;y++){
            int tile = control.tileIndexOf(minx, y);
            for(int x = minx; x <= maxx; x += 1){
                if(control.liquidAmount[tile] + control.previousLiquidAmount[tile] > 0.001){
                    float tx = x*tilesize;
                    float ty = y*tilesize;
                    float transam = 1f/(1f+Mathf.lerp(control.previousLiquidAmount[tile],control.liquidAmount[tile],t));
                    Lines.stroke(0.5f);
                    Lines.line(tx ,ty,tx+control.fluidVelX(tile),ty+control.fluidVelY(tile));
                    Lines.stroke(0.3f);
                    Lines.line(tx ,ty,tx+control.fluidVelX(tile)*transam,ty+control.fluidVelY(tile)*transam);
                }
                tile+=1;
            }
        }
        Lines.stroke(1);

    }

    public void initialiseSprites(){
        var g_liquids = GroundFluidControl.liquidProperties;
        fluidSpritesWidth = (int)Math.max(Mathf.ceil(Mathf.sqrt(g_liquids.size+1)),2);
        fluidSprites.resize(fluidSpritesWidth*96,fluidSpritesWidth*96);
        fluidSprites.begin(Color.clear);
        Draw.proj(0, 0, fluidSprites.getWidth(), fluidSprites.getHeight());
        Draw.color();
        TextureRegion n= Core.atlas.find("unity-ground-liquid-noise");
        Draw.rect(n,(0.5f)*96,(0.5f)*96,96,96);

        for(int i = 0;i<g_liquids.size;i++){
            int x = (i+1)%fluidSpritesWidth;
            int y = (i+1)/fluidSpritesWidth;
            TextureRegion tr= Core.atlas.find("unity-ground-liquid-"+g_liquids.get(i).name);
            if(tr==null){
                Draw.color(g_liquids.get(i).color);
                Fill.crect(x*96,y*96,96,96);
                continue;
            }
            Draw.rect(tr,(x+0.5f)*96,(y+0.5f)*96,96,96);
        }
        fluidSprites.end();
        Draw.proj(Core.camera);
        initialisedSprites = true;
    }
    //normalises flow velocity
    private static float transformVel(float v){
        return Mathf.clamp(v*0.5f+0.5f);
    }

    //makes liquid depth a hyperbolic value, graphical cutoff is 0.2
    private static float transformDepth(float x,float x2,float min,float trans){
        float c = -min*20;
        float a = 0.25f /(-c);
        return Mathf.lerp(x==0?0:(1f-1f/(a*x+1-a*c)),x2==0?0:(1f-1f/(a*x2+1-a*c)),trans);
    }

    private static void clearPixel(byte[] data, int index){
        data[index] = 0;
        data[index+1] = 0;
        data[index+2] = 0;
        data[index+3] = 0;
    }
    private static void writePixel(byte[] data, int index, Color c){
        data[index] = (byte)(c.r * 255);
        data[index+1] = (byte)(c.g * 255);
        data[index+2] = (byte)(c.b * 255);
        data[index+3] = (byte)(c.a * 255);
    }
}
