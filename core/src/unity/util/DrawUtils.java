package unity.util;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.graphics.*;

import static arc.Core.atlas;
import static arc.math.geom.Mat3D.*;

/** Intermediately-shared utility access for rendering operations. */
public final class DrawUtils{
    public static final float perspectiveDistance = 150f;

    public static final TextureAtlas emptyAtlas = new TextureAtlas(){{
        white = error = new AtlasRegion(){{
            u = v = 0f;
            u2 = v2 = 1f;
        }};
    }};

    private final static TextureRegion tr1 = new TextureRegion(), tr2 = new TextureRegion();
    private static final Color col1 = new Color();
    private static final Vec2 vec1 = new Vec2(), vec2 = new Vec2(), vec3 = new Vec2(), vec4 = new Vec2();
    private static final Vec2
    a = new Vec2(),
    b = new Vec2(),
    c = new Vec2(),
    left = new Vec2(), leftInit = new Vec2(),
    right = new Vec2(), rightInit = new Vec2();

    private static final Vec3 vert1 = new Vec3(), vert2 = new Vec3(), vert3 = new Vec3(), vert4 = new Vec3();
    private static final Vec3[] vec3s = new Vec3[]{new Vec3(), new Vec3(), new Vec3(), new Vec3()};

    private static boolean building;
    private static final int linestr = 5;
    private static final FloatSeq floatBuilder = new FloatSeq(linestr * 20);

    private static final Vec3 v31 = new Vec3();
    private static final Mat3D m41 = new Mat3D();

    private DrawUtils(){
        throw new AssertionError();
    }

    public static void panningCircle(TextureRegion region,
                                     float x, float y, float w, float h, float radius,
                                     float arcCone, float arcRotation, Quat rotation,
                                     float layerLow, float layerHigh){
        panningCircle(region, x, y, w, h, radius, arcCone, arcRotation, rotation, false, layerLow, layerHigh, perspectiveDistance);
    }

    public static void panningCircle(TextureRegion region,
                                     float x, float y, float w, float h, float radius,
                                     float arcCone, float arcRotation, Quat rotation,
                                     boolean useLinePrecision, float layerLow, float layerHigh){
        panningCircle(region, x, y, w, h, radius, arcCone, arcRotation, rotation, useLinePrecision, layerLow, layerHigh, 150f);
    }

    /**
     * 3D-rotated circle.
     * @author GlennFolker
     */
    public static void panningCircle(TextureRegion region,
                                     float x, float y, float w, float h, float radius,
                                     float arcCone, float arcRotation, Quat rotation,
                                     boolean useLinePrecision, float layerLow, float layerHigh, float perspectiveDst){
        float z = Draw.z();

        float arc = arcCone / 360f;
        int sides = useLinePrecision ? (int)(Lines.circleVertices(radius * 3f) * arc) : (int)((Mathf.PI2 * radius * arc) / w);
        float space = arcCone / sides;
        float hstep = (Lines.getStroke() * h / 2f) / Mathf.cosDeg(space / 2f);
        float r1 = radius - hstep, r2 = radius + hstep;

        for(int i = 0; i < sides; i++){
            float a = arcRotation - arcCone / 2f + space * i,
            cos = Mathf.cosDeg(a), sin = Mathf.sinDeg(a),
            cos2 = Mathf.cosDeg(a + space), sin2 = Mathf.sinDeg(a + space);
            m41.idt().rotate(rotation);

            Mat3D.rot(v31.set(r1 * cos, r1 * sin, 0f), m41).scl(Math.max((perspectiveDst + v31.z) / perspectiveDst, 0f));
            float x1 = x + v31.x;
            float y1 = y + v31.y;
            float sumZ = v31.z;

            Mat3D.rot(v31.set(r1 * cos2, r1 * sin2, 0f), m41).scl(Math.max((perspectiveDst + v31.z) / perspectiveDst, 0f));
            float x2 = x + v31.x;
            float y2 = y + v31.y;
            sumZ += v31.z;

            Mat3D.rot(v31.set(r2 * cos2, r2 * sin2, 0f), m41).scl(Math.max((perspectiveDst + v31.z) / perspectiveDst, 0f));
            float x3 = x + v31.x;
            float y3 = y + v31.y;
            sumZ += v31.z;

            Mat3D.rot(v31.set(r2 * cos, r2 * sin, 0f), m41).scl(Math.max((perspectiveDst + v31.z) / perspectiveDst, 0f));
            float x4 = x + v31.x;
            float y4 = y + v31.y;
            sumZ = (sumZ + v31.z) / 4f;

            Draw.z(sumZ >= 0f ? layerHigh : layerLow);
            Fill.quad(region, x3, y3, x2, y2, x1, y1, x4, y4);
        }

        Draw.z(z);
    }

    public static void line(float x1, float y1, float x2, float y2){
        TextureRegion end = atlas.find("hcircle");
        line(atlas.white(), end, end, x1, y1, x2, y2);
    }

    public static void line(TextureRegion line, TextureRegion end, float x1, float y1, float x2, float y2){
        line(line, end, end, x1, y1, x2, y2);
    }

    /**
     * Alternative to {@link Lines#line(float, float, float, float, boolean)} that uses textured ends.
     * @author GlennFolker
     */
    public static void line(TextureRegion line, TextureRegion start, TextureRegion end, float x1, float y1, float x2, float y2){
        float angle = Mathf.angleExact(x2 - x1, y2 - y1), s = Lines.getStroke();

        Draw.rect(start, x1, y1, s, s, angle + 180f);
        Draw.rect(end, x2, y2, s, s, angle);
        Lines.line(line, x1, y1, x2, y2, false);
    }

    public static void lineFalloff(float x1, float y1, float x2, float y2, Color outer, Color inner, int iterations, float falloff){
        TextureRegion end = atlas.find("hcircle");
        lineFalloff(atlas.white(), end, end, x1, y1, x2, y2, outer, inner, iterations, falloff);
    }

    /** @author GlennFolker */
    public static void lineFalloff(TextureRegion line, TextureRegion start, TextureRegion end,
                                   float x1, float y1, float x2, float y2,
                                   Color outer, Color inner, int iterations, float falloff){
        float s = Lines.getStroke();
        for(int i = 0; i < iterations; i++){
            Lines.stroke(s, col1.set(outer).lerp(inner, i / (iterations - 1f)));

            line(line, start, end, x1, y1, x2, y2);
            s *= falloff;
        }
    }

    public static void line(Color color, float x, float y, float x2, float y2){
        Lines.stroke(3f, Pal.gray);
        Lines.line(x, y, x2, y2);
        Lines.stroke(1f, color);
        Lines.line(x, y, x2, y2);
        Draw.reset();
    }

    public static void fillSector(float x, float y, float radius, float rotation, float fraction){
        fillSector(x, y, radius, rotation, fraction, Lines.circleVertices(radius * 3f));
    }

    /** @author GlennFolker */
    public static void fillSector(float x, float y, float radius, float rotation, float fraction, int sides){
        int max = Math.max(Mathf.round(sides * fraction), 1);
        for(int i = 0; i < max; i++){
            vec1.trns((float)i / max * fraction * 360f + rotation, radius);
            vec2.trns((i + 1f) / max * fraction * 360f + rotation, radius);

            Fill.tri(x, y, x + vec1.x, y + vec1.y, x + vec2.x, y + vec2.y);
        }
    }

    public static void hollowPoly(float x, float y, int amount, float rad1, float rad2){
        for(int i = 0; i < amount; i++){
            float r1 = (360f / amount) * i;
            float r2 = (360f / amount) * (i + 1f);
            float s1 = Mathf.sinDeg(r1), c1 = Mathf.cosDeg(r1);
            float s2 = Mathf.sinDeg(r2), c2 = Mathf.cosDeg(r2);

            Fill.quad(
                    x + s1 * rad1, y + c1 * rad1,
                    x + s1 * rad2, y + c1 * rad2,
                    x + s2 * rad2, y + c2 * rad2,
                    x + s2 * rad1, y + c2 * rad1
            );
        }
    }

    public static void linePoint(float x, float y, Color col){
        linePoint(x, y, col.toFloatBits(), Draw.z());
    }

    public static void linePoint(float x, float y, float col, float z){
        linePoint(x, y, col, Lines.getStroke(), z);
    }

    public static void linePoint(float x, float y, float col, float w, float z){
        if(!building){
            throw new IllegalStateException("Not building.");
        }else{
            floatBuilder.add(x, y, col, w);
            floatBuilder.add(z);
        }
    }

    public static void beginLine(){
        if(building){
            throw new IllegalStateException("Already building.");
        }else{
            floatBuilder.clear();
            building = true;
        }
    }

    public static void endLine(){
        endLine(false);
    }

    public static void endLine(boolean wrap){
        if(!building){
            throw new IllegalStateException("Not building.");
        }else{
            polyLine(floatBuilder.items, 0, floatBuilder.size, wrap);
            building = false;
        }
    }

    /**
     * {@link Lines#polyline(float[], int, boolean)} that supports variable color, width, and Z layer.
     * @author GlennFolker
     */
    public static void polyLine(float[] items, int offset, int length, boolean wrap){
        if(length < linestr * 2) return;
        for(int i = offset + linestr; i < length - linestr; i += linestr){
            float
            widthA = items[i - linestr + 3] / 2f, colA = items[i - linestr + 2],
            widthB = items[i + 3] / 2f, colB = items[i + 2],
            z = items[i + 4];

            a.set(items[i - linestr], items[i - linestr + 1]);
            b.set(items[i], items[i + 1]);
            c.set(items[i + linestr], items[i + linestr + 1]);

            MathUtils.pathJoin(a, b, c, left, right, widthB);
            vert3.set(left, colB);
            vert4.set(right, colB);

            if(i == offset + linestr){
                if(wrap){
                    vec1.set(items[offset + length - linestr], items[offset + length - linestr + 1]);

                    MathUtils.pathJoin(vec1, a, b, leftInit, rightInit, widthA);
                    vert1.set(rightInit, colA);
                    vert2.set(leftInit, colA);
                }else{
                    MathUtils.pathEnd(b.x, b.y, a.x, a.y, left, right, widthA);
                    vert1.set(right, colA);
                    vert2.set(left, colA);
                }
            }

            pushQuad(z);
            vert1.set(vert4.x, vert4.y, colB);
            vert2.set(vert3.x, vert3.y, colB);
        }

        float
        widthEnd = items[offset + length - linestr + 3] / 2f,
        colEnd = items[offset + length - linestr + 2],
        zEnd = items[offset + length - linestr + 4];

        if(wrap){
            float
            colStart = items[offset + 2],
            zStart = items[offset + 4];

            a.set(items[offset], items[offset + 1]);
            MathUtils.pathJoin(b, c, a, left, right, widthEnd);
            vert3.set(left, colEnd);
            vert4.set(right, colEnd);
            pushQuad(zEnd);

            vert1.set(left, colEnd);
            vert2.set(right, colEnd);
            vert3.set(rightInit, colStart);
            vert4.set(leftInit, colStart);
            pushQuad(zStart);
        }else{
            MathUtils.pathEnd(b.x, b.y, c.x, c.y, left, right, widthEnd);
            vert3.set(right, colEnd);
            vert4.set(left, colEnd);
            pushQuad(zEnd);
        }
    }

    private static void pushQuad(float z){
        Draw.z(z);
        Fill.quad(vert1.x, vert1.y, vert1.z, vert2.x, vert2.y, vert2.z, vert3.x, vert3.y, vert3.z, vert4.x, vert4.y, vert4.z);
    }

    public static void mulVec(float[] mat, Vec3 vec){
        float x = vec.x * mat[M00] + vec.y * mat[M01] + vec.z * mat[M02] + mat[M03];
        float y = vec.x * mat[M10] + vec.y * mat[M11] + vec.z * mat[M12] + mat[M13];
        float z = vec.x * mat[M20] + vec.y * mat[M21] + vec.z * mat[M22] + mat[M23];
        vec.x = x;
        vec.y = y;
        vec.z = z;
    }

    static float getYPos(float d, float r, float h){
        float c1 = Mathf.pi * r;
        if(d < c1){
            return r * (1f - Mathf.sinDeg(180 * d / c1));
        }else if(d > c1 + h - r){
            return (h - r) + r * (Mathf.sinDeg(180 * (d - (c1 + h - r)) / c1));
        }else{
            return d - c1 + r;
        }
    }

    public static void drawTread(TextureRegion region, float x, float y, float w, float h, float r, float rot, float d1, float d2){
        float c1 = Mathf.pi * r;
        float cut1 = c1 * 0.5f;
        float cut2 = c1 * 1.5f + h - r * 2;
        if(d1 < cut1 && d2 < cut1){return;}//cant be seen
        if(d1 > cut2 && d2 > cut2){return;}//cant be seen

        float y1 = getYPos(d1, r, h) - h * 0.5f;
        float y2 = getYPos(d2, r, h) - h * 0.5f;
        TextureRegion reg = region;
        if(d1 < cut1){
            y1 = -h * 0.5f;
            tr1.set(region);
            tr1.v = Mathf.map(cut1, d1, d2, tr1.v, tr1.v2);
            reg = tr1;
        }

        if(d2 > cut2){
            y2 = h * 0.5f;
            tr1.set(region);
            tr1.v2 = Mathf.map(cut2, d1, d2, tr1.v, tr1.v2);
            reg = tr1;
        }

        Draw.rect(reg, x, y + (y1 + y2) * 0.5f, w, y2 - y1, w * 0.5f, -y1, rot);

    }

    public static void drawRotRect(TextureRegion region, float x, float y, float w, float h, float th, float rot, float ang1, float ang2){
        if(region == null || !Core.settings.getBool("effects")) return;
        float amod1 = Mathf.mod(ang1, 360f);
        float amod2 = Mathf.mod(ang2, 360f);
        if(amod1 >= 180f && amod2 >= 180f) return;

        tr1.set(region);
        float uy1 = tr1.v;
        float uy2 = tr1.v2;
        float uCenter = (uy1 + uy2) / 2f;
        float uSize = (uy2 - uy1) * h / th * 0.5f;
        uy1 = uCenter - uSize;
        uy2 = uCenter + uSize;
        tr1.v = uy1;
        tr1.v2 = uy2;

        float s1 = -Mathf.cos(ang1 * Mathf.degreesToRadians);
        float s2 = -Mathf.cos(ang2 * Mathf.degreesToRadians);
        if(amod1 > 180f){
            tr1.v2 = Mathf.map(0f, amod1 - 360f, amod2, uy2, uy1);
            s1 = -1f;
        }else if(amod2 > 180f){
            tr1.v = Mathf.map(180f, amod1, amod2, uy2, uy1);
            s2 = 1f;
        }
        s1 = Mathf.map(s1, -1f, 1f, y - h / 2f, y + h / 2f);
        s2 = Mathf.map(s2, -1f, 1f, y - h / 2f, y + h / 2f);
        Draw.rect(tr1, x, (s1 + s2) * 0.5f, w, s2 - s1, w * 0.5f, y - s1, rot);
    }

    public static void drawRectOrtho(TextureRegion region, float x, float y, float z, float w, float h, float rotY, float rotZ){
        drawRectOrtho(region, x, y, 0, 0, z, w, h, rotY, rotZ);
    }

    public static void drawRectOrtho(TextureRegion region, float x, float y, float ox, float oy, float z, float w, float h, float rotY, float rotZ){
        drawRectOrtho(region, x, y, ox, oy, z, w, h, rotY, rotZ, (w != h) ? rotZ : 0);
    }

    public static void drawRectOrtho(TextureRegion region, float x, float y, float ox, float oy, float z, float w, float h, float rotY, float rotZ, float sprrotZ){
        vec3s[3].set(+w * 0.5f, +h * 0.5f, 0);
        vec3s[0].set(-w * 0.5f, +h * 0.5f, 0);
        vec3s[1].set(-w * 0.5f, -h * 0.5f, 0);
        vec3s[2].set(+w * 0.5f, -h * 0.5f, 0);

        vert1.set(ox, oy, z);
        m41.idt();
        for(int i = 0; i < 4; i++){
            vec3s[i].rotate(Vec3.Z, sprrotZ);
        }
        vert2.set(Vec3.Y).rotate(Vec3.Z, -rotZ);
        vert1.rotate(Vec3.Z, -rotZ);
        m41.rotate(vert2, -rotY);
        m41.translate(vert1);

        for(int i = 0; i < 4; i++){
            mulVec(m41.val, vec3s[i]);
            vec3s[i].add(x, y, 0);
        }

        Fill.quad(region, vec3s[0].x, vec3s[0].y, vec3s[1].x, vec3s[1].y, vec3s[2].x, vec3s[2].y, vec3s[3].x, vec3s[3].y);
    }

    public static void drawRectOffsetHorz(TextureRegion region, float x, float y, float w, float h, float rotation, float o){
        tr1.set(region);
        tr2.set(region);
        float cx = x + w * 0.5f;
        float dx = x - w * 0.5f + w * o;
        float t1w = w * (1f - o);
        float t2w = w * o;
        tr1.u2 = Mathf.lerp(region.u, region.u2, 1 - o);
        tr2.u = Mathf.lerp(region.u2, region.u, o);
        Draw.rect(tr1, dx + t1w * 0.5f, y, t1w, h, x - dx, h * 0.5f, rotation);
        Draw.rect(tr2, dx - t2w * 0.5f, y, t2w, h, x - (dx - t2w), h * 0.5f, rotation);
    }

    public static void arc(float x, float y, float r, float fromRadian, float toRadian){
        int seg = (int)Math.max(1, Lines.circleVertices(r) * Math.abs(toRadian - fromRadian) / (2 * Mathf.pi));
        float c = Mathf.cos(fromRadian);
        float s = Mathf.sin(fromRadian);
        float thick = Lines.getStroke() * 0.5f;
        vec1.set(c * (r + thick) + x, s * (r + thick) + y);
        vec2.set(c * (r - thick) + x, s * (r - thick) + y);
        for(int i = 0; i < seg; i++){
            float t = Mathf.lerp(fromRadian, toRadian, (i + 1f) / seg);
            c = Mathf.cos(t);
            s = Mathf.sin(t);
            vec3.set(c * (r + thick) + x, s * (r + thick) + y);
            vec4.set(c * (r - thick) + x, s * (r - thick) + y);
            Fill.quad(Core.atlas.white(), vec1.x, vec1.y, vec3.x, vec3.y, vec4.x, vec4.y, vec2.x, vec2.y);
            vec1.set(vec3);
            vec2.set(vec4);
        }
    }

    public static void selected(float x, float y, float size, Color color){
        Draw.color(color);

        for(int i = 0; i < 4; ++i){
            Point2 p = Geometry.d8edge[i];
            Draw.rect("block-select", x + p.x * size, y + p.y * size, (float)(i * 90));
        }

        Draw.reset();
    }
}
