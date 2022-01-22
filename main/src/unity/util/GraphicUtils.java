package unity.util;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
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
}
