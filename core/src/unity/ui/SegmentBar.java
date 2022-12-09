package unity.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

/** @author GlennFolker */
public class SegmentBar extends Bar{
    protected static Rect scissor = new Rect();

    protected CharSequence name;
    protected Floatp fraction;
    protected Segment[] segments;

    protected float value, lastValue, blink, outlineRadius, separatorWidth = 4f;
    protected Color outlineColor = new Color(), separatorColor = Pal.gray.cpy();

    public SegmentBar(Prov<CharSequence> name, Floatp fraction, Segment... segments){
        this.fraction = fraction;
        this.segments = Seq.with(segments).sort(s -> s.fraction).toArray(Segment.class);

        value = lastValue = fraction.get();
        update(() -> {
            this.name = name.get();
            for(var segment : segments){
                segment.color.set(segment.colorProv.get());
                segment.blinkColor.set(segment.blinkColorProv.get());
            }
        });
    }

    @Override
    public void reset(float value){
        this.value = lastValue = blink = value;
    }

    @Override
    public void set(Prov<String> name, Floatp fraction, Color color){
        this.fraction = fraction;
        this.lastValue = fraction.get();

        for(var segment : segments){
            segment.color.set(color);
            segment.blinkColor.set(color);
        }

        update(() -> this.name = name.get());
    }

    @Override
    public void snap(){
        lastValue = value = fraction.get();
    }

    @Override
    public SegmentBar outline(Color color, float stroke){
        outlineColor.set(color);
        outlineRadius = Scl.scl(stroke);
        return this;
    }

    public SegmentBar separator(Color color, float stroke){
        separatorColor.set(color);
        separatorWidth = Scl.scl(stroke);
        return this;
    }

    @Override
    public void flash(){
        blink = 1f;
    }

    @Override
    public Bar blink(Color color){
        Color f = color.cpy();
        for(var segment : segments){
            segment.blinkColor.set(f);
            segment.blinkColorProv = () -> f;
        }

        return this;
    }

    @Override
    public void draw(){
        if(fraction == null) return;
        float computed = Mathf.clamp(fraction.get());

        if(lastValue > computed){
            blink = 1f;
            lastValue = computed;
        }

        if(Float.isNaN(lastValue)) lastValue = 0;
        if(Float.isInfinite(lastValue)) lastValue = 1f;
        if(Float.isNaN(value)) value = 0;
        if(Float.isInfinite(value)) value = 1f;
        if(Float.isNaN(computed)) computed = 0;
        if(Float.isInfinite(computed)) computed = 1f;

        blink = Mathf.lerpDelta(blink, 0f, 0.2f);
        value = Mathf.lerpDelta(value, computed, 0.15f);

        Drawable bar = Tex.bar;
        if(outlineRadius > 0f){
            Draw.color(outlineColor);
            bar.draw(x - outlineRadius, y - outlineRadius, width + outlineRadius * 2f, height + outlineRadius * 2f);
        }

        Draw.colorl(0.1f);
        Draw.alpha(parentAlpha);
        bar.draw(x, y, width, height);

        Drawable top = Tex.barTop;
        float topWidth = width * value;
        float minWidth = Core.atlas.find("bar-top").width;

        for(int i = 1; i < segments.length; i++){
            float fraction = segments[i].fraction;

            Lines.stroke(separatorWidth, separatorColor);
            Draw.alpha(parentAlpha);

            Lines.line(
                x + width * fraction, y,
                x + width * fraction, y + height,
                false
            );
        }

        Draw.reset();
        if(topWidth > minWidth){
            drawTop(top, width, topWidth);
        }else if(ScissorStack.push(scissor.set(x, y, topWidth, height))){
            drawTop(top, width, minWidth);
            ScissorStack.pop();
        }

        Draw.reset();

        Font font = Fonts.outline;
        GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        lay.setText(font, name);

        font.setColor(1f, 1f, 1f, 1f);
        font.getCache().clear();
        font.getCache().addText(name, x + width / 2f - lay.width / 2f, y + height / 2f + lay.height / 2f + 1);
        font.getCache().draw(parentAlpha);

        Pools.free(lay);
    }

    protected void drawTop(Drawable top, float width, float topWidth){
        for(int i = 0; i < segments.length; i++){
            var segment = segments[i];
            if(value > segment.fraction && ScissorStack.push(scissor.set(
                x + width * segment.fraction, y,
                x + width * (i == segments.length - 1 ? 1f : segments[i + 1].fraction), height
            ))){
                Draw.color(segment.color, segment.blinkColor, blink);
                Draw.alpha(parentAlpha);

                top.draw(x, y, topWidth, height);
                ScissorStack.pop();
            }
        }
    }

    public static class Segment{
        public Prov<Color> colorProv, blinkColorProv;
        public Color color = new Color(), blinkColor = new Color();
        public float fraction;

        public Segment(Prov<Color> colorProv, Prov<Color> blinkColorProv, float fraction){
            this.colorProv = colorProv;
            this.blinkColorProv = blinkColorProv;
            this.fraction = fraction;

            color.set(colorProv.get());
            blinkColor.set(blinkColorProv.get());
        }

        public Segment(Prov<Color> colorProv, float fraction){
            this(colorProv, colorProv, fraction);
        }
    }
}
