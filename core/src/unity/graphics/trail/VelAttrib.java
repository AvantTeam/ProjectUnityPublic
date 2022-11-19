package unity.graphics.trail;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import unity.graphics.*;
import unity.graphics.BaseTrail.*;

/**
 * Velocity trail attribute. Adds an arbitrary vector multiplied by trail statSpeed to the trail position.
 * @author GlennFolker
 */
//TODO doesn't exactly handle Time.delta properly
@SuppressWarnings("unchecked")
public class VelAttrib extends TrailAttrib{
    private static final Vec2 vec = new Vec2();

    public float velX, velY, drag;
    public VelRotationHandler<BaseTrail, VelAttrib> rot;

    public float rotation;

    public VelAttrib(){
        this(0f, 0f, (t, v) -> BaseTrail.unconvRot(t.la()), 0.5f);
    }

    public <T extends BaseTrail, V extends VelAttrib> VelAttrib(float velX, float velY, VelRotationHandler<T, V> rot){
        this(velX, velY, rot, 0.25f);
    }

    public <T extends BaseTrail, V extends VelAttrib> VelAttrib(float velX, float velY, VelRotationHandler<T, V> rot, float drag){
        super(2);

        this.velX = velX;
        this.velY = velY;
        this.drag = drag;
        this.rot = (VelRotationHandler<BaseTrail, VelAttrib>)rot;
    }

    public float velX(float[] vertex){
        return vertex[attribOffset];
    }

    public void velX(float[] vertex, float velX){
        vertex[attribOffset] = velX;
    }

    public float velY(float[] vertex){
        return vertex[attribOffset + 1];
    }

    public void velY(float[] vertex, float velY){
        vertex[attribOffset + 1] = velY;
    }

    @Override
    public void saveLast(BaseTrail trail, float x, float y, float width, float angle, float speed, float pointDelta){
        rotation = rot.get(trail, this) - 90f;
    }

    @Override
    public void point(BaseTrail trail, FloatSeq points, int index, float x, float y, float width, float angle, float speed, float pointDelta){
        vec.trns(rotation, velX * speed, velY * speed);
        points.add(vec.x, vec.y);
    }

    @Override
    public void mutate(BaseTrail trail, float[] vertex, int index, float speed, float pointDelta){
        if(index == trail.size() - 1) return;

        float vx = velX(vertex) * Time.delta, vy = velY(vertex) * Time.delta, d = Math.max(1f - drag * Time.delta, 0f);
        trail.x(vertex, trail.x(vertex) + vx);
        trail.y(vertex, trail.y(vertex) + vy);

        velX(vertex, vx * d);
        velY(vertex, vy * d);
    }

    @Override
    public void endMutate(BaseTrail trail){
        trail.recalculateAngle();
    }

    @Override
    public VelAttrib copy(){
        return new VelAttrib(velX, velY, rot, drag);
    }

    public VelAttrib flip(){
        VelAttrib out = copy();
        out.velX *= -1f;
        return out;
    }

    public interface VelRotationHandler<T extends BaseTrail, V extends VelAttrib>{
        float get(T trail, V Attrib);
    }
}
