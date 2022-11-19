package unity.graphics.trail;

import arc.struct.*;
import mindustry.gen.*;
import unity.graphics.*;
import unity.graphics.BaseTrail.*;

/**
 * Move trail attribute. Offsets the entire trail evenly, as if it inherits the given transform.
 * @author GlennFolker
 */
//TODO rotation
public class MoveAttrib extends TrailAttrib{
    public Posc parent;

    protected float lastX, lastY, dx, dy;

    public MoveAttrib(Posc parent){
        super(0);
        this.parent = parent;

        lastX = parent.getX();
        lastY = parent.getY();
    }

    @Override
    public void point(BaseTrail trail, FloatSeq points, int index, float x, float y, float width, float angle, float speed, float pointDelta){}

    @Override
    public void saveLast(BaseTrail trail, float x, float y, float width, float angle, float speed, float pointDelta){
        if(!parent.isAdded()){
            dx = dy = 0f;
        }else{
            float nx = parent.getX(), ny = parent.getY();
            dx = nx - lastX;
            dy = ny - lastY;
            lastX = nx;
            lastY = ny;
        }
    }

    @Override
    public void mutate(BaseTrail trail, float[] vertex, int index, float speed, float pointDelta){
        trail.x(vertex, trail.x(vertex) + dx);
        trail.y(vertex, trail.y(vertex) + dy);
    }

    @Override
    public TrailAttrib copy(){
        return new MoveAttrib(parent);
    }
}
