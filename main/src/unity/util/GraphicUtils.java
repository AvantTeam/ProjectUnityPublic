package unity.util;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.gen.*;

import static arc.Core.*;

public final class GraphicUtils{
    private static final IntIntMap matches = new IntIntMap();

    private GraphicUtils(){
        throw new AssertionError();
    }

    public static Pixmap outline(TextureRegion region, Color color, int width){
        Pixmap out = Pixmaps.outline(atlas.getPixmap(region), color, width);
        if(Core.settings.getBool("linear")) Pixmaps.bleed(out);

        return out;
    }

    public static void drawCenter(Pixmap pix, Pixmap other){
        pix.draw(other, pix.width / 2 - other.width / 2, pix.height / 2 - other.height / 2, true);
    }

    public static void drawCenter(Pixmap pix, PixmapRegion other){
        Pixmap copy = other.crop();
        drawCenter(pix, copy);
        copy.dispose();
    }

    /** @author Drullkus */
    public static int colorLerp(int a, int b, float frac){
        return SColor.construct(
            pythagoreanLerp(SColor.r(a), SColor.r(b), frac),
            pythagoreanLerp(SColor.g(a), SColor.g(b), frac),
            pythagoreanLerp(SColor.b(a), SColor.b(b), frac),
            pythagoreanLerp(SColor.a(a), SColor.a(b), frac)
        );
    }

    /** @author Drullkus */
    public static int averageColor(int a, int b){
        return SColor.construct(
            pythagoreanAverage(SColor.r(a), SColor.r(b)),
            pythagoreanAverage(SColor.g(a), SColor.g(b)),
            pythagoreanAverage(SColor.b(a), SColor.b(b)),
            pythagoreanAverage(SColor.a(a), SColor.a(b))
        );
    }

    /**
     * Pythagorean-style interpolation will result in color transitions that appear more natural than linear interpolation.
     * @author Drullkus
     */
    public static float pythagoreanLerp(float a, float b, float frac){
        if(a == b || frac <= 0) return a;
        if(frac >= 1) return b;

        a *= a * (1 - frac);
        b *= b * frac;

        return Mathf.sqrt(a + b);
    }

    /** @author Drullkus */
    public static float pythagoreanAverage(float a, float b){
        return Mathf.sqrt(a * a + b * b) * Utils.sqrtHalf;
    }

    /**
     * Almost Bilinear Interpolation except the underlying color interpolator uses {@link #pythagoreanLerp(float, float, float)}.
     * @author Drullkus
     */
    public static int getColor(PixmapRegion pix, float x, float y){
        // Cast floats into ints twice instead of casting 20 times.
        int xInt = (int)x;
        int yInt = (int)y;

        if(!Structs.inBounds(xInt, yInt, pix.width, pix.height)) return 0;

        // A lot of these booleans are commonly checked, so let's run each check just once.
        boolean isXInt = x == xInt;
        boolean isYInt = y == yInt;
        boolean xOverflow = x + 1 > pix.width;
        boolean yOverflow = y + 1 > pix.height;

        // Remember: x & y values themselves are already checked if in-bounds.
        if((isXInt && isYInt) || (xOverflow && yOverflow)) return pix.get(xInt, yInt);

        if(isXInt || xOverflow){
            return colorLerp(getAlphaMedianColor(pix, xInt, yInt), getAlphaMedianColor(pix, xInt, yInt + 1), y % 1);
        }else if(isYInt || yOverflow){
            return colorLerp(getAlphaMedianColor(pix, xInt, yInt), getAlphaMedianColor(pix, xInt + 1, yInt), x % 1);
        }

        return colorLerp(
            colorLerp(getAlphaMedianColor(pix, xInt, yInt), getAlphaMedianColor(pix, xInt + 1, yInt), x % 1),
            colorLerp(getAlphaMedianColor(pix, xInt, yInt + 1), getAlphaMedianColor(pix, xInt + 1, yInt + 1), x % 1),
            y % 1
        );
    }

    /** @author Drullkus */
    public static int getAlphaMedianColor(PixmapRegion pix, int x, int y){
        int color = pix.get(x, y);
        float alpha = SColor.a(color);

        if(alpha >= 0.1f) return color;

        return SColor.a(alphaMedian(
            color,
            pix.get(x + 1, y),
            pix.get(x, y + 1),
            pix.get(x - 1, y),
            pix.get(x, y - 1)
        ), alpha);
    }

    /** @author Drullkus */
    public static int alphaMedian(int main, int... colors){
        int c1 = main,
            c2 = main;

        synchronized(matches){
            matches.clear();
            int count, primaryCount = -1, secondaryCount = -1;

            for(int color : colors){
                if(SColor.a(color) < 0.1f) continue;

                count = matches.increment(color) + 1;

                if(count > primaryCount){
                    secondaryCount = primaryCount;
                    c2 = c1;

                    primaryCount = count;
                    c1 = color;
                }else if(count > secondaryCount){
                    secondaryCount = count;
                    c2 = color;
                }
            }

            if(primaryCount > secondaryCount){
                return c1;
            }else if(primaryCount == -1){
                return main;
            }else{
                return averageColor(c1, c2);
            }
        }
    }

    /**
     * Gets multiple regions inside a {@link TextureRegion}. The size for each region has to be 32.
     * @param w The amount of regions horizontally.
     * @param h The amount of regions vertically.
     */
    public static TextureRegion[] getRegions(TextureRegion region, int w, int h, int tilesize){
        int size = w * h;
        TextureRegion[] regions = new TextureRegion[size];

        float tileW = (region.u2 - region.u) / w;
        float tileH = (region.v2 - region.v) / h;

        for(int i = 0; i < size; i++){
            float tileX = ((float)(i % w)) / w;
            float tileY = ((float)(i / w)) / h;
            TextureRegion reg = new TextureRegion(region);

            //start coordinate
            reg.u = Mathf.map(tileX, 0f, 1f, reg.u, reg.u2) + tileW * 0.01f;
            reg.v = Mathf.map(tileY, 0f, 1f, reg.v, reg.v2) + tileH * 0.01f;
            //end coordinate
            reg.u2 = reg.u + tileW * 0.98f;
            reg.v2 = reg.v + tileH * 0.98f;

            reg.width = reg.height = tilesize;

            regions[i] = reg;
        }
        return regions;
    }

    public static TextureRegion[] getRegions(TextureRegion region, int w, int h){
        return getRegions(region,w,h,32);
    }

    public static void selected(float x, float y, float size, Color color) {
        Draw.color(color);

        for(int i = 0; i < 4; ++i) {
            Point2 p = Geometry.d8edge[i];
            Draw.rect("block-select", x + p.x*size, y + p.y*size, (float)(i * 90));
        }

        Draw.reset();
    }

    public static class ZipperArm{
        //go back and forth like a zipper
        public float maxlen;
        public int joints;
        public Vec2 start = new Vec2();
        public Vec2 end = new Vec2();
        public Vec2[] jointPositions;
        Vec2 prevnorm = new Vec2();

        public ZipperArm(float sx,float sy, float tx,float ty,float maxlen, int joints){
            this.maxlen = maxlen;
            this.joints = joints;
            start.set(sx,sy);
            end.set(tx,ty);
            jointPositions = new Vec2[joints];
            for(int i =0;i<joints;i++){
                jointPositions[i] = new Vec2();
            }
        }

        public void update(){
            Tmp.v1.set(end).sub(start);
            float d = Tmp.v1.len();
            Tmp.v1.scl(1f/d);
            float armseg = 0.5f*maxlen/joints;
            float dseg = 0.5f*d/joints;
            float offset = Mathf.sqrt(Math.max(0,armseg*armseg-dseg*dseg));
            float flipnor = prevnorm.dot(Tmp.v1.y,-Tmp.v1.x)>0?1:-1;
            prevnorm.set(Tmp.v1.y*flipnor,-Tmp.v1.x*flipnor);
            for(int i =0;i<joints;i++){
                int flip = i % 2 == 0 ? 1 : -1;
                jointPositions[i].set(
                    start.x + Tmp.v1.x * dseg * (2*i+1) + prevnorm.x * offset * flip,
                    start.y + Tmp.v1.y * dseg * (2*i+1) + prevnorm.y * offset * flip
                );
            }
        }
        public void draw(Cons3<Vec2,Vec2,Integer> con){
            int index = 0;
            for(int i = 0;i<joints;i++){
                if(i==0){
                    Tmp.v1.set(start.x, start.y);
                    Tmp.v2.set(jointPositions[0].x, jointPositions[0].y);
                    con.get(Tmp.v1,Tmp.v2,index++);
                }
                if(i==joints-1){
                    Tmp.v1.set(jointPositions[i].x, jointPositions[i].y);
                    Tmp.v2.set(end.x, end.y);
                    con.get(Tmp.v1,Tmp.v2,index++);
                }else{
                    Tmp.v1.set(jointPositions[i].x, jointPositions[i].y);
                    Tmp.v2.set(jointPositions[i+1].x, jointPositions[i+1].y);
                    con.get(Tmp.v1,Tmp.v2,index++);
                }
            }

        }

    }

    //
}





















