package unity.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.graphics.*;

import java.util.*;

import static arc.math.geom.Mat3D.*;
import static mindustry.Vars.tilesize;

public final class UnityDrawf{
    private final static float[] v = new float[6];
    private final static TextureRegion nRegion = new TextureRegion();
    private final static Vec2 vector = new Vec2();
    private static final Vec2 vec1 = new Vec2(), vec2 = new Vec2(), vec3 = new Vec2(), vec4 = new Vec2();

    private UnityDrawf(){
        throw new AssertionError();
    }

    /** @author EyeOfDarkness */
    public static void shiningCircle(int seed, float time, float x, float y, float radius, int spikes, float spikeDuration, float spikeWidth, float spikeHeight){
        shiningCircle(seed, time, x, y, radius, spikes, spikeDuration, spikeWidth, spikeHeight, 0f);
    }

    /** @author EyeOfDarkness */
    public static void shiningCircle(int seed, float time, float x, float y, float radius, int spikes, float spikeDuration, float spikeWidth, float spikeHeight, float angleDrift){
        shiningCircle(seed, time, x, y, radius, spikes, spikeDuration, 0f, spikeWidth, spikeHeight, angleDrift);
    }

    /** @author EyeOfDarkness */
    public static void shiningCircle(int seed, float time, float x, float y, float radius, int spikes, float spikeDuration, float durationRange, float spikeWidth, float spikeHeight, float angleDrift){
        Fill.circle(x, y, radius);
        spikeWidth = Math.min(spikeWidth, 90f);
        int idx;

        for(int i = 0; i < spikes; i++){
            float d = spikeDuration * (durationRange > 0f ? Mathf.randomSeed((seed + i) * 41L, 1f - durationRange, 1f + durationRange) : 1f);
            float timeOffset = Mathf.randomSeed((seed + i) * 314L, 0f, d);
            int timeSeed = Mathf.floor((time + timeOffset) / d);

            float fin = ((time + timeOffset) % d) / d;
            float fslope = (0.5f - Math.abs(fin - 0.5f)) * 2f;
            float angle = Mathf.randomSeed(Math.max(timeSeed, 1) + ((i + seed) * 245L), 360f);

            if(fslope > 0.0001f){
                idx = 0;
                float drift = angleDrift > 0 ? Mathf.randomSeed(Math.max(timeSeed, 1) + ((i + seed) * 162L), -angleDrift, angleDrift) * fin : 0f;
                for(int j = 0; j < 3; j++){
                    float angB = (j * spikeWidth - (2f) * spikeWidth / 2f) + angle;
                    Tmp.v1.trns(angB + drift, radius + (j == 1 ? (spikeHeight * fslope) : 0f)).add(x, y);
                    v[idx++] = Tmp.v1.x;
                    v[idx++] = Tmp.v1.y;
                }

                Fill.tri(v[0], v[1], v[2], v[3], v[4], v[5]);
            }
        }
    }

    public static void snowFlake(float x, float y, float r, float s){
        for(int i = 0; i < 3; i++){
            Lines.lineAngleCenter(x, y, r + 60 * i, s);
        }
    }

    public static void spark(float x, float y, float w, float h, float r){
        for(int i = 0; i < 4; i++){
            Drawf.tri(x, y, w, h, r + 90 * i);
        }
    }

    public static void drawHeat(TextureRegion reg, float x, float y, float rot, float temp){
        float a;
        if(temp > 273.15f){
            a = Math.max(0f, (temp - 498f) * 0.001f);
            if(a < 0.01f) return;
            if(a > 1f){
                Color fCol = Pal.turretHeat.cpy().add(0, 0, 0.01f * a);
                fCol.mul(a);
                Draw.color(fCol, a);
            }else{
                Draw.color(Pal.turretHeat, a);
            }
        }else{
            a = 1f - Mathf.clamp(temp / 273.15f);
            if(a < 0.01f) return;
            Draw.color(UnityPal.coldcolor, a);
        }
        Draw.blend(Blending.additive);
        Draw.rect(reg, x, y, rot);
        Draw.blend();
        Draw.color();
    }

    public static void drawSlideRect(TextureRegion region, float x, float y, float w, float h, float tw, float th, float rot, int step, float offset){
        if(region == null) return;
        nRegion.set(region);

        float scaleX = w / tw;
        float texW = nRegion.u2 - nRegion.u;

        nRegion.u += Mathf.map(offset % 1, 0f, 1f, 0f, texW * step / tw);
        nRegion.u2 = nRegion.u + scaleX * texW;
        Draw.rect(nRegion, x, y, w, h, w * 0.5f, h * 0.5f, rot);
    }

    static float getypos(float d,float r, float h){
        float c1 = Mathf.pi*r;
        if(d<c1){
            return r*(1f-Mathf.sinDeg(180f*d/c1));
        }else if(d>c1+h-r*2){
            return (h-r) + r*(Mathf.sinDeg(180*(d-(c1+h-r*2))/c1));
        }else{
            return d-c1+r;
        }
    }

    public static void drawTread(TextureRegion region, float x, float y, float w, float h, float r, float rot, float d1, float d2){
        float c1 = Mathf.pi*r;
        float cut1 = c1*0.5f;
        float cut2 = c1*1.5f + h-r*2;
        if(d1<cut1 && d2< cut1){ return;}//cant be seen
        if(d1>cut2 && d2> cut2){ return;}//cant be seen

        float y1 = getypos(d1,r,h) - h*0.5f;
        float y2 = getypos(d2,r,h) - h*0.5f;
        TextureRegion reg = region;
        if(d1<cut1){
            y1 =  - h*0.5f;
            nRegion.set(region);
            nRegion.v2 = Mathf.map(cut1,d1,d2,nRegion.v2,nRegion.v);
            reg = nRegion;
        }

        if(d2>cut2){
            y2 = h*0.5f;
            nRegion.set(region);
            nRegion.v = Mathf.map(cut2,d1,d2,nRegion.v2,nRegion.v);
            reg = nRegion;
        }

        Draw.rect(reg, x, y + (y1 + y2) * 0.5f, w, y2 - y1, w * 0.5f,  - y1, rot);

    }

    public static void drawRotRect(TextureRegion region, float x, float y, float w, float rot_h, float true_h, float rot, float ang1, float ang2){
        if(region == null || !Core.settings.getBool("effects")) return;
        float amod1 = Mathf.mod(ang1, 360f);
        float amod2 = Mathf.mod(ang2, 360f);
        if(amod1 >= 180f && amod2 >= 180f) return;

        nRegion.set(region);
        float uy1 = nRegion.v;
        float uy2 = nRegion.v2;
        float uCenter = (uy1 + uy2) / 2f;
        float uSize = (uy2 - uy1) * rot_h / true_h * 0.5f;
        uy1 = uCenter - uSize;
        uy2 = uCenter + uSize;
        nRegion.v = uy1;
        nRegion.v2 = uy2;

        float s1 = -Mathf.cos(ang1 * Mathf.degreesToRadians);
        float s2 = -Mathf.cos(ang2 * Mathf.degreesToRadians);
        if(amod1 > 180f){
            nRegion.v2 = Mathf.map(0f, amod1 - 360f, amod2, uy2, uy1);
            s1 = -1f;
        }else if(amod2 > 180f){
            nRegion.v = Mathf.map(180f, amod1, amod2, uy2, uy1);
            s2 = 1f;
        }
        s1 = Mathf.map(s1, -1f, 1f, y - rot_h / 2f, y + rot_h / 2f);
        s2 = Mathf.map(s2, -1f, 1f, y - rot_h / 2f, y + rot_h / 2f);
        Draw.rect(nRegion, x, (s1 + s2) * 0.5f, w, s2 - s1, w * 0.5f, y - s1, rot);
    }

    public static void line(Color color, float x, float y, float x2, float y2){
        Lines.stroke(3f, Pal.gray);
        Lines.line(x, y, x2, y2);
        Lines.stroke(1f, color);
        Lines.line(x, y, x2, y2);
        Draw.reset();
    }
    public static void arc(float x, float y, float r, float fromRadian,float toRadian){
        int seg = (int)Math.max(1,Lines.circleVertices(r)*Math.abs(toRadian-fromRadian)/(2*Mathf.pi));
        Vec2 ptop = new Vec2(), pbottom = new Vec2(); Vec2 ctop = new Vec2(), cbottom = new Vec2();
        float c = Mathf.cos(fromRadian);
        float s = Mathf.sin(fromRadian);
        float thick = Lines.getStroke()*0.5f;
        ptop.set(c*(r+thick)+x,s*(r+thick)+y);
        pbottom.set(c*(r-thick)+x,s*(r-thick)+y);
        for(int i = 0;i<seg;i++){
            float t = Mathf.lerp(fromRadian,toRadian,(i+1f)/seg);
            c = Mathf.cos(t);
            s = Mathf.sin(t);
            ctop.set(c*(r+thick)+x,s*(r+thick)+y);
            cbottom.set(c*(r-thick)+x,s*(r-thick)+y);
            Fill.quad(Core.atlas.white(), ptop.x,ptop.y,ctop.x,ctop.y,cbottom.x,cbottom.y,pbottom.x,pbottom.y);
            ptop.set(ctop);
            pbottom.set(cbottom);
        }

    }
    
    public static void mulVec(float[] mat, Vec3 vec){
        float x = vec.x * mat[M00] + vec.y * mat[M01] + vec.z * mat[M02] + mat[M03];
        float y = vec.x * mat[M10] + vec.y * mat[M11] + vec.z * mat[M12] + mat[M13];
        float z = vec.x * mat[M20] + vec.y * mat[M21] + vec.z * mat[M22] + mat[M23];
        vec.x = x;
        vec.y = y;
        vec.z = z;
    }
    
    static Vec3[] tmpV = new Vec3[4];
    public static void drawRectOrtho(TextureRegion region, float x, float y, float z, float w, float h, float rotY, float rotZ){
        drawRectOrtho(region,x,y,0,0,z,w,h,rotY,rotZ);
    }
    static Mat3D matT = new Mat3D();
    static Vec3 tAxis = new Vec3();
    static Vec3 tmpV2 = new Vec3();
    
    public static void drawRectOrtho(TextureRegion region, float x, float y, float ox, float oy, float z, float w, float h, float rotY, float rotZ){
        drawRectOrtho(region, x, y, ox, oy, z, w, h, rotY, rotZ, (w!=h)?rotZ:0);
    }
    public static void drawRectOrtho(TextureRegion region, float x, float y, float ox, float oy, float z, float w, float h, float rotY, float rotZ, float sprrotZ){
        tmpV[3].set(+w*0.5f, +h*0.5f, 0);
        tmpV[0].set(-w*0.5f, +h*0.5f, 0);
        tmpV[1].set(-w*0.5f, -h*0.5f, 0);
        tmpV[2].set(+w*0.5f, -h*0.5f, 0);

        tmpV2.set(ox,oy,z);
        matT.idt();
        for(int i = 0;i<4;i++){
            tmpV[i].rotate(Vec3.Z,sprrotZ);
        }
        tAxis.set(Vec3.Y).rotate(Vec3.Z,-rotZ);
        tmpV2.rotate(Vec3.Z,-rotZ);
        matT.rotate(tAxis,-rotY);
        matT.translate(tmpV2);

        for(int i = 0;i<4;i++){
            mulVec(matT.val,tmpV[i]);
            tmpV[i].add(x,y,0);
        }

        Fill.quad(region,tmpV[0].x,tmpV[0].y, tmpV[1].x,tmpV[1].y, tmpV[2].x,tmpV[2].y, tmpV[3].x,tmpV[3].y);
    }
    static TextureRegion t1=new TextureRegion(),t2=new TextureRegion();
    public static void drawRectOffsetHorz(TextureRegion region, float x, float y,float w,float h,float rotation,float o){
        t1.set(region);
        t2.set(region);
        float cx = x+w*0.5f;
        float dx = x-w*0.5f + w*o;
        float t1w = w*(1f-o);
        float t2w = w*o;
        t1.u2 = Mathf.lerp(region.u,region.u2,1-o);
        t2.u = Mathf.lerp(region.u2,region.u,o);
        Draw.rect(t1, dx+t1w*0.5f, y, t1w, h, x-dx, h*0.5f, rotation);
        Draw.rect(t2, dx-t2w*0.5f, y, t2w, h, x-(dx-t2w), h*0.5f, rotation);
    }



    static{
        for(int i = 0; i < 4; i++){
            tmpV[i] = new Vec3();
        }
    }
    /** @author sunny */
    public static void ring(float bx, float by, int sides, float rad, float hScl, float rot, float thickness, float layerUnder, float layerOver){
        float wScl = 1f;

        float l = Lines.getStroke();

        float sign = Mathf.sign(hScl);
        hScl = Math.abs(hScl);
        Tmp.v1.trns(rot + 90, sign * thickness * (1 - hScl));
        hScl = Math.abs(hScl);

        float space = 360 / (float)sides;
        float r1 = rad - l / 2, r2 = rad + l / 2;

        for(int i = 0; i < sides; i++){
            float a = space * i;
            boolean over = i >= sides / 2 == sign > 0;

            Draw.z(!over ? layerUnder : layerOver);
            vec1.trns(rot,
                    r1 * wScl * Mathf.cosDeg(a),
                    r1 * hScl * Mathf.sinDeg(a)
            );
            vec2.trns(rot,
                    r1 * wScl * Mathf.cosDeg(a + space),
                    r1 * hScl * Mathf.sinDeg(a + space)
            );
            vec3.trns(rot,
                    r2 * wScl * Mathf.cosDeg(a + space),
                    r2 * hScl * Mathf.sinDeg(a + space)
            );
            vec4.trns(rot,
                    r2 * wScl * Mathf.cosDeg(a),
                    r2 * hScl * Mathf.sinDeg(a)
            );

            float x = bx + Tmp.v1.x;
            float y = by + Tmp.v1.y;

            if(over){
            //over, use 12
                Draw.color(Color.red);
                Fill.quad(
                        bx - Tmp.v1.x + vec4.x, by - Tmp.v1.y + vec4.y,
                        bx - Tmp.v1.x + vec3.x, by - Tmp.v1.y + vec3.y,
                        x + vec3.x, y + vec3.y,
                        x + vec4.x, y + vec4.y
                );
            }
            else{
                //under, use 34
                Draw.color(Color.orange);
                Fill.quad(
                        bx - Tmp.v1.x + vec2.x, by - Tmp.v1.y + vec2.y,
                        bx - Tmp.v1.x + vec1.x, by - Tmp.v1.y + vec1.y,
                        x + vec1.x, y + vec1.y,
                        x + vec2.x, y + vec2.y
                );

            }

            Draw.z(!over ? layerUnder : layerOver);
            Draw.color(Color.white);
            Fill.quad(
                    x + vec1.x, y + vec1.y,
                    x + vec2.x, y + vec2.y,
                    x + vec3.x, y + vec3.y,
                    x + vec4.x, y + vec4.y
            );
        }
        Draw.reset();
    }
}
