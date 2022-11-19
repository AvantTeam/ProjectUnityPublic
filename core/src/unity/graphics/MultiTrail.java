package unity.graphics;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import unity.graphics.trail.*;

/**
 * Holds multiple trails with additional offsets, width multiplier, and color override. Currently, it only supports the
 * {@linkplain VelAttrib velocity} attribute.
 * @author GlennFolker
 */
public class MultiTrail extends BaseTrail{
    public @Nullable VelAttrib vel;
    public TrailHold[] trails;

    public MultiTrail(TrailHold... trails){
        this(BaseTrail::rot, trails);
    }

    public MultiTrail(RotationHandler rot, TrailHold... trails){
        this(rot, null, trails);
    }

    public MultiTrail(RotationHandler rot, VelAttrib vel, TrailHold... trails){
        super(maxLen(trails));
        this.rot = rot;
        this.vel = vel;
        this.trails = trails;

        if(vel != null) vel.attribOffset = 2;
    }

    @Override
    public MultiTrail copy(){
        TrailHold[] mapped = new TrailHold[trails.length];
        for(int i = 0; i < mapped.length; i++) mapped[i] = trails[i].copy();

        MultiTrail out = new MultiTrail(rot, vel, mapped);
        out.points.addAll(points);
        out.lastX = lastX;
        out.lastY = lastY;
        out.counter = counter;
        return out;
    }

    @Override
    public int baseSize(){
        return 4;
    }

    @Override
    public void clear(){
        for(TrailHold trail : trails) trail.trail.clear();
    }

    @Override
    public void drawCap(Color color, float width){
        if(forceCap) return;
        forceDrawCap(color, width);
    }

    @Override
    public void forceDrawCap(Color color, float width){
        for(TrailHold trail : trails) trail.trail.drawCap(trail.color == null ? color : trail.color, width);
    }

    @Override
    public void draw(Color color, float width){
        if(forceCap) forceDrawCap(color, width);
        for(TrailHold trail : trails) trail.trail.draw(trail.color == null ? color : trail.color, width);
    }

    @Override
    public void shorten(){
        if((counter += Time.delta) >= 1f) counter %= 1f;
        for(TrailHold trail : trails) trail.trail.shorten();
    }

    @Override
    public float update(float x, float y, float width, float angle){
        if(Float.isNaN(lastX) || Float.isNaN(lastY)){
            lastX = x;
            lastY = y;
            lastWidth = width;
            lastAngle = angle;
            //return 0f;
        }

        float delta = counter, speed = Mathf.dst(lastX, lastY, x, y) / Time.delta;
        defUpdate(x, y, width, angle, speed, delta);

        int psize = points.size, stride = this.stride;
        if((counter += Time.delta) >= 1f){
            if(points.size > length * stride - stride) points.removeRange(0, stride - 1);
            points.add(x, y);

            if(vel != null){
                vel.point(this, points, psize / stride, x, y, width, angle, speed, delta);
            }else{
                points.add(Float.NaN, Float.NaN);
            }

            counter %= 1f;
        }

        if(vel != null){
            vel.saveLast(this, x, y, width, angle, speed, delta);

            float[] items = points.items;
            for(int i = 0, ind = 0; i < psize - stride; i += stride, ind++){
                float x1 = items[i], y1 = items[i + 1];

                float[] baseVert = vert(ind);
                vel.mutate(this, baseVert, ind, speed, delta);
                vert(baseVert, ind);

                float dx = items[i] - x1, dy = items[i + 1] - y1;
                for(TrailHold trail : trails){
                    BaseTrail t = trail.trail;

                    int childInd = ind + (t.size() - size());
                    if(childInd >= 0 && childInd < t.size() - 1){
                        float[] vert = t.vert(childInd);
                        t.x(vert, t.x(vert) + dx);
                        t.y(vert, t.y(vert) + dy);
                        t.vert(vert, childInd);
                    }
                }
            }

            for(TrailHold trail : trails) trail.trail.recalculateAngle();
        }

        lastX = x;
        lastY = y;
        return speed;
    }

    @Override
    public void kickstart(float x, float y){
        clear();
        for(int i = 0; i < length; i++){
            points.add(x, y);
            if(vel != null){
                vel.point(this, points, i, x, y, 0f, 0f, 0f, 0f);
            }else{
                points.add(Float.NaN, Float.NaN);
            }
        }

        for(TrailHold trail : trails) trail.trail.kickstart(x, y);
    }

    public void defUpdate(float x, float y, float width, float angle, float speed, float delta){
        for(TrailHold trail : trails){
            Tmp.v1.trns(unconvRot(angle) - 90f, trail.x, trail.y);
            trail.trail.update(x + Tmp.v1.x, y + Tmp.v1.y, width * trail.width);
        }
    }

    public static int maxLen(TrailHold... trails){
        int length = 0;
        for(TrailHold trail : trails) length = Math.max(length, trail.trail.length);
        return length;
    }

    @Override
    public void recalculateAngle(){}

    public static class TrailHold{
        public BaseTrail trail;
        public float x;
        public float y;
        public float width;
        public Color color;

        public TrailHold(BaseTrail trail){
            this(trail, 0f, 0f, 1f, null);
        }

        public TrailHold(BaseTrail trail, Color color){
            this(trail, 0f, 0f, 1f, color);
        }

        public TrailHold(BaseTrail trail, float x, float y){
            this(trail, x, y, 1f, null);
        }

        public TrailHold(BaseTrail trail, float x, float y, Color color){
            this(trail, x, y, 1f, color);
        }

        public TrailHold(BaseTrail trail, float x, float y, float width){
            this(trail, x, y, width, null);
        }

        public TrailHold(BaseTrail trail, float x, float y, float width, Color color){
            this.trail = trail;
            this.x = x;
            this.y = y;
            this.width = width;
            this.color = color;
        }

        public TrailHold copy(){
            return new TrailHold(trail.copy(), x, y, width, color);
        }
    }
}
