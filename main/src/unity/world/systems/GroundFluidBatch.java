package unity.world.systems;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.*;
import unity.graphics.UnityShaders.*;

//yeh this is a slightly modified spritebatch.

public class GroundFluidBatch extends Batch{
    //xy + color + uv + mix_color
    public static final int VERTEX_SIZE = 2 + 1 + 2 + 1 + 1;
    public static final int SPRITE_SIZE = 4 * VERTEX_SIZE;
    public static final VertexAttribute liquidId =  new VertexAttribute(1, GL20.GL_FLOAT, false, "a_fluidType");

    protected final float[] vertices;

    /** Number of rendering calls, ever. Will not be reset unless set manually. **/
    int totalRenderCalls = 0;
    /** The maximum number of sprites rendered in one batch so far. **/
    int maxSpritesInBatch = 0;

    //the current fluid sprite
    int fluidSetting;

    public GroundFluidBatch(int size, BatchedGroundLiquidShader defaultShader){
        // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
        if(size > 8191) throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);

        if(size > 0){
            projectionMatrix.setOrtho(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());

            mesh = new Mesh(true, false, size * 4, size * 6,
            VertexAttribute.position,
            VertexAttribute.color,
            VertexAttribute.texCoords,
            VertexAttribute.mixColor,
            liquidId
            );

            vertices = new float[size * SPRITE_SIZE];

            int len = size * 6;
            short[] indices = new short[len];
            short j = 0;
            for(int i = 0; i < len; i += 6, j += 4){
                indices[i] = j;
                indices[i + 1] = (short)(j + 1);
                indices[i + 2] = (short)(j + 2);
                indices[i + 3] = (short)(j + 2);
                indices[i + 4] = (short)(j + 3);
                indices[i + 5] = j;
            }
            mesh.setIndices(indices);

            if(defaultShader == null){
                throw new IllegalStateException("GroundFluidBatch must have THE BatchedGroundLiquidShader.");
            }else{
                shader = defaultShader;
            }
        }else{
            vertices = new float[0];
            shader = null;
        }
    }

    @Override
    protected void flush(){
        if(idx == 0) return;

        getShader().bind();
        setupMatrices();

        if(customShader != null && apply){
            customShader.apply();
        }
        if(customShader == null){
            shader.apply();
        }

        Gl.depthMask(false);
        totalRenderCalls++;
        int spritesInBatch = idx / SPRITE_SIZE;
        if(spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
        int count = spritesInBatch * 6;

        blending.apply();

        lastTexture.bind();
        Mesh mesh = this.mesh;
        mesh.setVertices(vertices, 0, idx);
        mesh.getIndicesBuffer().position(0);
        mesh.getIndicesBuffer().limit(count);
        mesh.render(getShader(), Gl.triangles, 0, count);

        idx = 0;
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){

        int verticesLength = vertices.length;
        int remainingVertices = verticesLength;
        if(texture != lastTexture){
            switchTexture(texture);
        }else{
            remainingVertices -= idx;
            if(remainingVertices == 0){
                flush();
                remainingVertices = verticesLength;
            }
        }
        int copyCount = Math.min(remainingVertices, count);

        System.arraycopy(spriteVertices, offset, vertices, idx, copyCount);
        idx += copyCount;
        count -= copyCount;
        while(count > 0){
            offset += copyCount;
            flush();
            copyCount = Math.min(verticesLength, count);
            System.arraycopy(spriteVertices, offset, vertices, 0, copyCount);
            idx += copyCount;
            count -= copyCount;
        }
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){

        Texture texture = region.texture;
        if(texture != lastTexture){
            switchTexture(texture);
        }else if(idx == vertices.length){
            flush();
        }

        float[] vertices = this.vertices;
        int idx = this.idx;
        this.idx += SPRITE_SIZE;

        //yeh theres no rotation
        {
            float fx2 = x + width;
            float fy2 = y + height;
            float u = region.u;
            float v = region.v2;
            float u2 = region.u2;
            float v2 = region.v;

            float color = this.colorPacked;
            float mixColor = this.mixColorPacked;

            vertices[idx] = x;
            vertices[idx + 1] = y;
            vertices[idx + 2] = color;
            vertices[idx + 3] = u;
            vertices[idx + 4] = v;
            vertices[idx + 5] = mixColor;
            vertices[idx + 6] = fluidSetting;

            vertices[idx + 7] = x;
            vertices[idx + 8] = fy2;
            vertices[idx + 9] = color;
            vertices[idx + 10] = u;
            vertices[idx + 11] = v2;
            vertices[idx + 12] = mixColor;
            vertices[idx + 13] = fluidSetting;

            vertices[idx + 14] = fx2;
            vertices[idx + 15] = fy2;
            vertices[idx + 16] = color;
            vertices[idx + 17] = u2;
            vertices[idx + 18] = v2;
            vertices[idx + 19] = mixColor;
            vertices[idx + 20] = fluidSetting;

            vertices[idx + 21] = fx2;
            vertices[idx + 22] = y;
            vertices[idx + 23] = color;
            vertices[idx + 24] = u2;
            vertices[idx + 25] = v;
            vertices[idx + 26] = mixColor;
            vertices[idx + 27] = fluidSetting;
        }
    }


    //shit below, copied from Draw.java is literally only ever used in this liquids context due to the fluid type attribute.
    /** Draws a portion of a world-sized texture. */
    public void drawGroundFluid(Texture texture, int offsetX, int offsetY, int regW, int regH, float x,float y, int fluidID){
        float ww = texture.width, wh = texture.height;
        float u = offsetX/ww,
              v = offsetY/wh,
              u2 = (offsetX + regW)/ww,
              v2 = (offsetY + regH)/wh;

        Tmp.tr1.set(texture);
        Tmp.tr1.set(u, v2, u2, v);

        rect(Tmp.tr1, (x-0.5f)*Vars.tilesize, (y-0.5f)*Vars.tilesize, regW*Vars.tilesize, regH*Vars.tilesize,fluidID);
    }

    public void rect(TextureRegion region, float x, float y, float w, float h,int fid){
        fluidSetting = fid;
        setColor(GroundFluidControl.liquidProperties.get(fid-1).shallowColor);
        setMixColor(GroundFluidControl.liquidProperties.get(fid-1).deepColor);

        draw(region, x , y, 0, 0, w, h, 0);
    }
}
