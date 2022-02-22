package unity.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;
import static mindustry.graphics.CacheLayer.all;

public class UnityShaders {
    public static @Nullable ModSurfaceShader lava;

    public static CacheLayer.ShaderLayer lavaLayer;
    protected static boolean loaded;

    public static void load(){
        if(!headless){
            lava = new ModSurfaceShader("lava");
            loaded = true;
        }
        Log.info("[accent]<FTE + POST (CACHELAYER)>[]");
        lavaLayer = new CacheLayer.ShaderLayer(lava);
        CacheLayer.add(lavaLayer);
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
            return "noise";
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
}
