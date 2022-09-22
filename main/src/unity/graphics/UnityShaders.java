package unity.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.graphics.Shaders.*;
import unity.*;

import static mindustry.Vars.*;
import static mindustry.graphics.CacheLayer.all;

public class UnityShaders {
    public static @Nullable ModSurfaceShader lava,pit,waterpit;

    public static CacheLayer.ShaderLayer lavaLayer,pitLayer,waterpitLayer;
    public static BatchedGroundLiquidShader batchedGroundLiquid;

    protected static boolean loaded;

    public static void load(){
        if(!headless){
            try{
                lava = new ModSurfaceShader("lava");
                pit = new PitShader("pit","unity-concrete-blank1", "unity-stone-sheet", "unity-truss");
                waterpit = new PitShader("waterpit","unity-concrete-blank1", "unity-stone-sheet", "unity-truss");
            }catch(Exception e){
                Log.err("There was an exception loading the shaders: @",e);
            }
            loaded = true;
        }
        Log.info("[accent]<FTE + POST (CACHELAYER)>[]");
        lavaLayer = new CacheLayer.ShaderLayer(lava);
        pitLayer = new CacheLayer.ShaderLayer(pit);
        waterpitLayer = new CacheLayer.ShaderLayer(waterpit);
        CacheLayer.add(lavaLayer);
        CacheLayer.add(pitLayer);
        CacheLayer.add(waterpitLayer);
        batchedGroundLiquid = new BatchedGroundLiquidShader();
    }

    public static void dispose(){
        if(!headless && loaded){
            lava.dispose();
        }
    }

    /** Register a new CacheLayer. */
    public static void addUnder(CacheLayer... layers){
        int newSize = all.length + layers.length;
        var prev = all;
        //reallocate the array and copy everything over; performance matters very little here anyway
        all = new CacheLayer[newSize];
        System.arraycopy(prev, 0, all, layers.length, prev.length);
        System.arraycopy(layers, 0, all, 0, layers.length);

        for(int i = 0; i < all.length; i++){
            all[i].id = i;
        }
    }

    public static class GroundLiquidShader extends Shader{

        public GroundLiquidShader(){
            super(Core.files.internal("shaders/default.vert"),tree.get("shaders/groundliquid.frag"));
        }
        @Override
        public void apply(){
            var control = Unity.groundFluidControl;
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_resolution", world.width(), world.height());

            control.renderer.fluidSprites.getTexture().bind(1);
            control.renderer.fluidFrameBuffer.getTexture().bind(0);
            setUniformi("u_sprites", 1);
            setUniformi("u_sprites_width", control.renderer.fluidSpritesWidth);
        }
    }
    public static class BatchedGroundLiquidShader extends Shader{
        public BatchedGroundLiquidShader(){
            super(tree.get("shaders/groundliquid.vert"),tree.get("shaders/groundliquid2.frag"));
        }
        @Override
        public void apply(){

            var control = Unity.groundFluidControl;
            var renderer =  control.renderer;
            var fbo = renderer.fluidFrameBuffer;
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_trans", Mathf.clamp(control.getTransition()));
            setUniformf("u_resolution", fbo.getWidth(), fbo.getHeight());
            setUniformf("u_chunksize", renderer.prevChunkW, renderer.prevChunkH);
            setUniformf("u_offset", renderer.prevMinX, renderer.prevMinY);

            renderer.fluidSprites.getTexture().bind(1);
            renderer.fluidFrameBuffer.getTexture().bind(0);
            setUniformi("u_sprites", 1);
            setUniformi("u_sprites_width", control.renderer.fluidSpritesWidth);
        }
    }

    /** Shaders that get plastered on blocks, notably walls. */
    public static class BlockShader extends Shader {
        public BlockShader(String name){
            super(Core.files.internal("shaders/default.vert"),
                    tree.get("shaders/" + name + ".frag"));
        }

        @Override
        public void apply(){
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_offset",
                    Core.camera.position.x,
                    Core.camera.position.y
            );
        }
    }

    /** Shaders that get plastered on blocks but with differing resolution. */
    public static class ResBlockShader extends BlockShader {
        public ResBlockShader(String name){
            super(name);
        }

        @Override
        public void apply(){
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_resolution", Core.graphics.getWidth(), Core.graphics.getHeight());
            setUniformf("u_offset",
                    Core.camera.position.x,
                    Core.camera.position.y
            );
        }
    }

    /** SurfaceShader but uses a mod fragment asset. */
    public static class ModSurfaceShader extends Shader{
        Texture noiseTex;
        String noiseTexName = "noise";

        public ModSurfaceShader(String frag){
            super(Core.files.internal("shaders/screenspace.vert"),
                    tree.get("shaders/" + frag + ".frag"));
            loadNoise();
        }

        public ModSurfaceShader(String vertRaw, String fragRaw){
            super(vertRaw, fragRaw);
            loadNoise();
        }

        public String textureName(){
            return noiseTexName;
        }

        public void loadNoise(){
            Core.assets.load("sprites/" + textureName() + ".png", Texture.class).loaded = t -> {
                t.setFilter(Texture.TextureFilter.linear);
                t.setWrap(Texture.TextureWrap.repeat);
            };
        }

        @Override
        public void apply(){
            setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_resolution", Core.camera.width, Core.camera.height);
            setUniformf("u_time", Time.time);

            if(hasUniform("u_noise")){
                if(noiseTex == null){
                    noiseTex = Core.assets.get("sprites/" + textureName() + ".png", Texture.class);
                }

                noiseTex.bind(1);
                renderer.effectBuffer.getTexture().bind(0);

                setUniformi("u_noise", 1);
            }
        }
    }


    /** SurfaceShader but uses a mod fragment asset. */
    public static class PitShader extends ModSurfaceShader{
        TextureRegion toplayer,bottomlayer,truss;
        String toplayerName,bottomlayerName,trussName;

        public PitShader(String name,String toplayer,String bottomlayer,String truss){
            super(name);
            toplayerName = toplayer;
            bottomlayerName = bottomlayer;
            trussName = truss;
        }

        @Override
        public void apply(){
            var texture = Core.atlas.find("grass1").texture;
            if(toplayer == null){
                toplayer = Core.atlas.find(toplayerName);
            }
            if(bottomlayer == null){
                bottomlayer = Core.atlas.find(bottomlayerName);
            }
            if(truss == null){
                truss = Core.atlas.find(trussName);
            }
            if(noiseTex == null){
                noiseTex = Core.assets.get("sprites/" + textureName() + ".png", Texture.class);
            }
            setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2, Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_resolution", Core.camera.width, Core.camera.height);
            setUniformf("u_time", Time.time);
//tvariants
            setUniformf("u_toplayer", toplayer.u, toplayer.v, toplayer.u2, toplayer.v2);
            setUniformf("u_bottomlayer", bottomlayer.u, bottomlayer.v, bottomlayer.u2, bottomlayer.v2);
            setUniformf("bvariants", bottomlayer.width/32f);
            setUniformf("u_truss", truss.u, truss.v, truss.u2, truss.v2);

            texture.bind(2);
            noiseTex.bind(1);
            renderer.effectBuffer.getTexture().bind(0);
            setUniformi("u_noise", 1);
            setUniformi("u_texture2", 2);
        }
    }
}
