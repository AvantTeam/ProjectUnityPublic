package unity.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/**
 * An extension of the trail; capable of texture-mapping, shrink/grow configurations, various color interpolations, custom
 * blends, and trail effects.
 * @author GlennFolker
 */
public class TexturedTrail extends BaseTrail{
    /** The texture region of this trail. */
    public TextureRegion region;
    /** The cap texture region of this trail. */
    public TextureRegion capRegion;
    /** The trail's width shrink as it goes, in percentage. `1f` makes the trail triangle-shaped. */
    public float shrink = 1f;
    /** The trail's alpha as it goes, in percentage. `1f` makes the trail's tail completely invisible. */
    public float fadeAlpha = 0f;
    /** The trail's mix color alpha, used in {@link #draw(Color, float)}. Fades as the trail goes. */
    public float mixAlpha = 0.5f;
    /** The trail's base width, multiplied by {@link #update(float, float, float)}'d width. **/
    public float baseWidth = 1f;
    /** The trail's fade color, multiplied as the trail fades. Typically, used to saturate the trail as it fades. */
    public Color fadeColor = Color.white;
    /** The trail's {@link #fadeColor} interpolation. */
    public Interp gradientInterp = Interp.linear;
    /** The trail's center alpha interpolation. */
    public Interp fadeInterp = Interp.pow2In;
    /** The trail's edge alpha interpolation. */
    public Interp sideFadeInterp = Interp.pow3In;
    /** The trail's mix color interpolation. */
    public Interp mixInterp = Interp.pow5In;
    /** The trail's blending. */
    public Blending blend = Blending.normal;

    /** The trail's particle effect. */
    public Effect trailEffect = Fx.missileTrail;
    /** The particle effect's chance. */
    public float trailChance = 0f;
    /** The particle effect's radius. */
    public float trailWidth = 1f;
    /** The particle effect's color. */
    public Color trailColor = Pal.engine;
    /** The trail statSpeed's bare minimum at which the particle effects start appearing less. */
    public float trailThreshold = 3f;

    private static final float[] vertices = new float[24];
    private static final Color tmp = new Color();

    public TexturedTrail(TextureRegion region, TextureRegion capRegion, int length, TrailAttrib... attributes){
        this(length, attributes);
        this.region = region;
        this.capRegion = capRegion;
    }

    public TexturedTrail(TextureRegion region, int length, TrailAttrib... attributes){
        this(length, attributes);
        this.region = region;
        if(region instanceof AtlasRegion reg) capRegion = Core.atlas.find(reg.name + "-cap", "unity-hcircle");
    }

    public TexturedTrail(int length, TrailAttrib... attributes){
        super(length, attributes);
    }

    @Override
    public TexturedTrail copy(){
        TexturedTrail out = new TexturedTrail(region, capRegion, length, copyAttrib(attributes));
        out.points.addAll(points);
        out.lastX = lastX;
        out.lastY = lastY;
        out.lastAngle = lastAngle;
        out.lastWidth = lastWidth;
        out.rot = rot;
        out.counter = counter;
        out.shrink = shrink;
        out.fadeAlpha = fadeAlpha;
        out.mixAlpha = mixAlpha;
        out.baseWidth = baseWidth;
        out.fadeColor = fadeColor;
        out.gradientInterp = gradientInterp;
        out.fadeInterp = fadeInterp;
        out.sideFadeInterp = sideFadeInterp;
        out.mixInterp = mixInterp;
        out.blend = blend;
        out.forceCap = forceCap;
        out.minDst = minDst;
        out.trailEffect = trailEffect;
        out.trailChance = trailChance;
        out.trailWidth = trailWidth;
        out.trailColor = trailColor;
        out.trailThreshold = trailThreshold;
        return out;
    }

    @Override
    public int baseSize(){
        return 5;
    }

    @Override
    public void draw(Color color, float widthMultiplier){
        if(forceCap) forceDrawCap(color, widthMultiplier);
        float width = baseWidth * widthMultiplier;

        if(region == null) region = Core.atlas.find("white");
        if(points.isEmpty()) return;

        float[] items = points.items;
        int psize = points.size, stride = this.stride;

        float u = region.u2, v = region.v2, u2 = region.u, v2 = region.v, uh = Mathf.lerp(u, u2, 0.5f);

        Draw.blend(blend);
        for(int i = 0, ind = 0; i < psize; i += stride, ind++){
            float
            x1 = items[i], y1 = items[i + 1], w1 = items[i + 2], r1 = items[i + 3], rv1 = Mathf.clamp(items[i + 4]),
            x2, y2, w2, r2, rv2;

            if(i < psize - stride){
                x2 = items[i + stride];
                y2 = items[i + stride + 1];
                w2 = items[i + stride + 2];
                r2 = items[i + stride + 3];
                rv2 = Mathf.clamp(items[i + stride + 4]);
            }else{
                x2 = lastX;
                y2 = lastY;
                w2 = lastWidth;
                r2 = lastAngle;
                rv2 = (float)psize / stride / length;
            }

            float
            fs1 = Mathf.map(rv1, 1f - shrink, 1f) * width * w1,
            fs2 = Mathf.map(rv2, 1f - shrink, 1f) * width * w2,

            cx = Mathf.sin(r1) * fs1, cy = Mathf.cos(r1) * fs1,
            nx = Mathf.sin(r2) * fs2, ny = Mathf.cos(r2) * fs2,

            mv1 = Mathf.lerp(v, v2, rv1), mv2 = Mathf.lerp(v, v2, rv2),
            cv1 = rv1 * fadeAlpha + (1f - fadeAlpha), cv2 = rv2 * fadeAlpha + (1f - fadeAlpha),
            col1 = tmp.set(Draw.getColor()).lerp(fadeColor, gradientInterp.apply(1f - rv1)).a(fadeInterp.apply(cv1)).clamp().toFloatBits(),
            col1h = tmp.set(Draw.getColor()).lerp(fadeColor, gradientInterp.apply(1f - rv1)).a(sideFadeInterp.apply(cv1)).clamp().toFloatBits(),
            col2 = tmp.set(Draw.getColor()).lerp(fadeColor, gradientInterp.apply(1f - rv2)).a(fadeInterp.apply(cv2)).clamp().toFloatBits(),
            col2h = tmp.set(Draw.getColor()).lerp(fadeColor, gradientInterp.apply(1f - rv2)).a(sideFadeInterp.apply(cv2)).clamp().toFloatBits(),
            mix1 = tmp.set(color).a(mixInterp.apply(rv1 * mixAlpha)).clamp().toFloatBits(),
            mix2 = tmp.set(color).a(mixInterp.apply(rv2 * mixAlpha)).clamp().toFloatBits();

            vertices[0] = x1 - cx;
            vertices[1] = y1 - cy;
            vertices[2] = col1h;
            vertices[3] = u;
            vertices[4] = mv1;
            vertices[5] = mix1;

            vertices[6] = x1;
            vertices[7] = y1;
            vertices[8] = col1;
            vertices[9] = uh;
            vertices[10] = mv1;
            vertices[11] = mix1;

            vertices[12] = x2;
            vertices[13] = y2;
            vertices[14] = col2;
            vertices[15] = uh;
            vertices[16] = mv2;
            vertices[17] = mix2;

            vertices[18] = x2 - nx;
            vertices[19] = y2 - ny;
            vertices[20] = col2h;
            vertices[21] = u;
            vertices[22] = mv2;
            vertices[23] = mix2;

            Draw.vert(region.texture, vertices, 0, 24);

            vertices[0] = x1 + cx;
            vertices[1] = y1 + cy;
            vertices[2] = col1h;
            vertices[3] = u2;
            vertices[4] = mv1;
            vertices[5] = mix1;

            vertices[6] = x1;
            vertices[7] = y1;
            vertices[8] = col1;
            vertices[9] = uh;
            vertices[10] = mv1;
            vertices[11] = mix1;

            vertices[12] = x2;
            vertices[13] = y2;
            vertices[14] = col2;
            vertices[15] = uh;
            vertices[16] = mv2;
            vertices[17] = mix2;

            vertices[18] = x2 + nx;
            vertices[19] = y2 + ny;
            vertices[20] = col2h;
            vertices[21] = u2;
            vertices[22] = mv2;
            vertices[23] = mix2;

            Draw.vert(region.texture, vertices, 0, 24);
        }

        Draw.blend();
    }

    @Override
    public void forceDrawCap(Color color, float widthMultiplier){
        if(capRegion == Core.atlas.find("clear")) return;

        float width = baseWidth * widthMultiplier;
        if(capRegion == null) capRegion = Core.atlas.find("unity-hcircle");

        int psize = points.size;
        if(psize > 0){
            float
            rv = (float)psize / stride / length,
            alpha = rv * fadeAlpha + (1f - fadeAlpha),
            w = Mathf.map(rv, 1f - shrink, 1f) * width * lastWidth * 2f,
            h = ((float)capRegion.height / capRegion.width) * w,

            angle = unconvRot(lastAngle) - 90f,
            u = capRegion.u, v = capRegion.v2, u2 = capRegion.u2, v2 = capRegion.v, uh = Mathf.lerp(u, u2, 0.5f),
            cx = Mathf.cosDeg(angle) * w / 2f, cy = Mathf.sinDeg(angle) * w / 2f,
            x1 = lastX, y1 = lastY,
            x2 = lastX + Mathf.cosDeg(angle + 90f) * h, y2 = lastY + Mathf.sinDeg(angle + 90f) * h,

            col1 = tmp.set(Draw.getColor()).lerp(fadeColor, gradientInterp.apply(1f - rv)).a(fadeInterp.apply(alpha)).clamp().toFloatBits(),
            col1h = tmp.set(Draw.getColor()).lerp(fadeColor, gradientInterp.apply(1f - rv)).a(sideFadeInterp.apply(alpha)).clamp().toFloatBits(),
            col2 = tmp.set(Draw.getColor()).lerp(fadeColor, gradientInterp.apply(1f - rv)).a(fadeInterp.apply(alpha)).clamp().toFloatBits(),
            col2h = tmp.set(Draw.getColor()).lerp(fadeColor, gradientInterp.apply(1f - rv)).a(sideFadeInterp.apply(alpha)).clamp().toFloatBits(),
            mix1 = tmp.set(color).a(mixInterp.apply(rv * mixAlpha)).clamp().toFloatBits(),
            mix2 = tmp.set(color).a(mixInterp.apply(rv * mixAlpha)).clamp().toFloatBits();

            Draw.blend(blend);
            vertices[0] = x1 - cx;
            vertices[1] = y1 - cy;
            vertices[2] = col1h;
            vertices[3] = u;
            vertices[4] = v;
            vertices[5] = mix1;

            vertices[6] = x1;
            vertices[7] = y1;
            vertices[8] = col1;
            vertices[9] = uh;
            vertices[10] = v;
            vertices[11] = mix1;

            vertices[12] = x2;
            vertices[13] = y2;
            vertices[14] = col2;
            vertices[15] = uh;
            vertices[16] = v2;
            vertices[17] = mix2;

            vertices[18] = x2 - cx;
            vertices[19] = y2 - cy;
            vertices[20] = col2h;
            vertices[21] = u;
            vertices[22] = v2;
            vertices[23] = mix2;

            Draw.vert(region.texture, vertices, 0, 24);

            vertices[0] = x1 + cx;
            vertices[1] = y1 + cy;
            vertices[2] = col1h;
            vertices[3] = u2;
            vertices[4] = v;
            vertices[5] = mix1;

            vertices[6] = x1;
            vertices[7] = y1;
            vertices[8] = col1;
            vertices[9] = uh;
            vertices[10] = v;
            vertices[11] = mix1;

            vertices[12] = x2;
            vertices[13] = y2;
            vertices[14] = col2;
            vertices[15] = uh;
            vertices[16] = v2;
            vertices[17] = mix2;

            vertices[18] = x2 + cx;
            vertices[19] = y2 + cy;
            vertices[20] = col2h;
            vertices[21] = u2;
            vertices[22] = v2;
            vertices[23] = mix2;

            Draw.vert(region.texture, vertices, 0, 24);
            Draw.blend();
        }
    }

    @Override
    public void shorten(){
        super.shorten();
        calcProgress();
    }

    @Override
    public float update(float x, float y, float width, float angle){
        float speed = super.update(x, y, width, angle);
        if(trailChance > 0f && Mathf.chanceDelta(trailChance * Mathf.clamp(speed / trailThreshold))){
            trailEffect.at(
            x, y, lastWidth * trailWidth,
            tmp.set(trailColor).a(fadeInterp.apply(Mathf.clamp(((float)points.size / stride / length) * fadeAlpha + (1f - fadeAlpha))))
            );
        }

        calcProgress();
        return speed;
    }

    @Override
    protected void basePoint(float x, float y, float width, float angle, float speed, float delta){
        super.basePoint(x, y, width, angle, speed, delta);
        points.add(1f);
    }

    public void calcProgress(){
        int psize = points.size, stride = this.stride;
        if(psize > 0){
            float[] items = points.items;

            float maxDst = 0f;
            for(int i = 0; i < psize; i += stride){
                float
                x = items[i], y = items[i + 1],
                dst = i < psize - stride ? Mathf.dst(x, y, items[i + stride], items[i + stride + 1]) : Mathf.dst(x, y, lastX, lastY);

                items[i + 4] = maxDst;
                maxDst += dst;
            }

            float frac = (float)psize / stride / length;
            for(int i = 0; i < psize; i += stride){
                items[i + 4] = Mathf.clamp((items[i + 4] / maxDst) * frac);
            }
        }
    }

    public float prog(float[] vertex){
        return vertex[4];
    }

    public void prog(float[] vertex, float angle){
        vertex[4] = angle;
    }
}
