package unity.assets.list;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import unity.graphics.*;
import unity.util.*;

import java.lang.reflect.*;

import static arc.graphics.gl.Shader.*;
import static mindustry.Vars.*;
import static mindustry.graphics.Shaders.*;

/** Lists all {@link Shader}s the mod has to load. */
public final class PUShaders{
    public static PlanetObjectShader planet;
    public static PUSurfaceShader eneraphyte;

    private PUShaders(){
        throw new AssertionError();
    }

    public static void load(){
        if(headless) return;

        planet = new PlanetObjectShader();
        eneraphyte = new PUSurfaceShader("eneraphyte", "noise");
    }

    public static <T extends Shader> T preprocess(String vertPreprocess, String fragPreprocess, Prov<T> create){
        prependVertexCode = vertPreprocess;
        prependFragmentCode = fragPreprocess;

        T shader = create.get();

        prependVertexCode = "";
        prependFragmentCode = "";
        return shader;
    }

    /**
     * <p>A surface shader that loads modded textures in {@code shaders/textures/}. Texture uniforms that correspond with the
     * texture list should be named as {@code u_texture[n + 1]} (e.g. {@code u_texture1}, {@code u_texture2}, ...), whereas the
     * frame buffer texture uniform can be of any name (e.g. {@code u_noise}, {@code u_texture}, ...).</p>
     *
     * <p>There are also 4 preset uniforms: {@code u_campos}, {@code u_resolution}, {@code u_viewport} and {@code u_time};
     * corresponds to the camera position, the camera dimension, the screen dimension, and {@link Time#time time}.</p>
     *
     * <p><b>The shader only supports up to 8 textures</b>: 1 frame buffer texture and 7 additional textures.</p>
     * @author GlennFolker
     */
    public static class PUSurfaceShader extends Shader{
        public Texture[] textures;

        public PUSurfaceShader(String frag, String... textures){
            this(getShaderFi("screenspace.vert"), file(frag + ".frag"), textures);
        }

        public PUSurfaceShader(Fi vert, Fi frag, String... textures){
            super(vert.readString(), frag.readString());
            loadTextures(textures);
        }

        protected void loadTextures(String... textureNames){
            if(textureNames.length > 7) throw new IllegalArgumentException("Max custom texture amount is 7.");
            if(textures != null) for(Texture texture : textures) texture.dispose();

            textures = new Texture[textureNames.length];
            for(int i = 0; i < textures.length; i++){
                Texture texture = new Texture(tree.get(tex(textureNames[i])));
                texture.setFilter(TextureFilter.linear);
                texture.setWrap(TextureWrap.repeat);
                textures[i] = texture;
            }
        }

        @Override
        public void apply(){
            setUniformf("u_campos", Core.camera.position.x - Core.camera.width / 2f, Core.camera.position.y - Core.camera.height / 2f);
            setUniformf("u_resolution", Core.camera.width, Core.camera.height);
            setUniformf("u_viewport", Core.graphics.getWidth(), Core.graphics.getHeight());
            setUniformf("u_time", Time.time);

            for(int i = textures.length - 1; i >= 0; i--){
                int unit = i + 1;
                String name = "u_texture" + unit;

                if(hasUniform(name)){
                    textures[i].bind(unit);
                    setUniformi(name, unit);
                }
            }

            renderer.effectBuffer.getTexture().bind(0);
        }

        @Override
        public void dispose(){
            super.dispose();
            if(textures != null){
                for(Texture texture : textures) texture.dispose();
                textures = null;
            }
        }
    }

    /**
     * {@link PlanetShader} but with correct normal transformations and an additional emission color.
     * @author GlennFolker
     */
    public static class PlanetObjectShader extends Shader{
        public Vec3 lightDir = new Vec3(1f, 1f, 1f).nor();
        public Color ambientColor = Color.white.cpy();
        public Color emissionColor = Color.clear.cpy();

        public PlanetObjectShader(){
            super(file("planet.vert"), getShaderFi("planet.frag"));
        }

        @Override
        public void apply(){
            Camera3D cam = renderer.planets.cam;

            setUniformf("u_lightdir", lightDir);
            setUniformf("u_ambientColor", ambientColor);
            setUniformf("u_emissionColor", emissionColor);
            setUniformf("u_camdir", cam.direction);
            setUniformf("u_campos", cam.position);
        }
    }

    public static Fi file(String path){
        return tree.get("shaders/" + path);
    }

    public static String tex(String name){
        return "shaders/textures/" + name + ".png";
    }
}
