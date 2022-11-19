package unity.graphics;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.util.*;
import mindustry.graphics.*;
import unity.assets.list.*;

import static mindustry.Vars.renderer;
import static mindustry.graphics.CacheLayer.*;

/** Defines all {@link CacheLayer}s this mod has to load. */
public final class PUCacheLayer{
    public static CacheLayer
    eneraphyte;

    private PUCacheLayer(){
        throw new AssertionError();
    }

    public static void load(){
        add(
            eneraphyte = new ApplicableShaderLayer(PUShaders.eneraphyte)
        );
    }

    /**
     * A forcible {@link ShaderLayer} that {@linkplain Shader#apply() applies} additional shader uniforms.
     * @author GlennFolker
     */
    public static class ApplicableShaderLayer extends CacheLayer{
        public @Nullable Shader shader;
        public @Nullable Cons<Shader> apply;
        public boolean force;

        public ApplicableShaderLayer(Shader shader){
            this(shader, false);
        }

        public ApplicableShaderLayer(Shader shader, boolean force){
            this(shader, force, null);
        }

        public ApplicableShaderLayer(Shader shader, Cons<Shader> apply){
            this(shader, false, apply);
        }

        public ApplicableShaderLayer(Shader shader, boolean force, Cons<Shader> apply){
            this.shader = shader;
            this.force = force;
            this.apply = apply;
        }

        @Override
        public void begin(){
            if(!force && !Core.settings.getBool("animatedwater")) return;

            renderer.blocks.floor.endc();
            renderer.effectBuffer.begin();

            Core.graphics.clear(Color.clear);
            renderer.blocks.floor.beginc();
        }

        @Override
        public void end(){
            if(!force && !Core.settings.getBool("animatedwater")) return;

            renderer.blocks.floor.endc();
            renderer.effectBuffer.end();

            if(apply != null) apply.get(shader);
            renderer.effectBuffer.blit(shader);
            renderer.blocks.floor.beginc();
        }
    }
}
