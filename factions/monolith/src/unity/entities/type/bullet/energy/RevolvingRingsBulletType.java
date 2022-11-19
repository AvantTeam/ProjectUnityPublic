package unity.entities.type.bullet.energy;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import unity.util.*;

/**
 * A basic bullet type with additional rings revolving around it. No additional logic purposes.
 * @author GlennFolker
 */
public class RevolvingRingsBulletType extends BasicBulletType{
    public float[] radius = {}, thickness = {};
    public Color[] colors = {}, glows = {};

    public float
    rotateSpeed = 6f,
    layerLow = Layer.flyingUnitLow - 0.01f, layerHigh = Layer.flyingUnit;

    public RevolvingRingsBulletType(float speed, float damage){
        this(speed, damage, "bullet");
    }

    public RevolvingRingsBulletType(float speed, float damage, String name){
        super(speed, damage, name);
    }

    @Override
    public void draw(Bullet b){
        super.draw(b);

        long seed = Mathf.rand.getState(0);
        Mathf.rand.setSeed(b.id);

        TextureRegion reg = Core.atlas.white(), light = Core.atlas.find("unity-line-shade");
        for(int i = 0; i < radius.length; i++){
            Tmp.v31.set(Vec3.X).setToRandomDirection();

            float r = b.id * 20f + Time.time * rotateSpeed * Mathf.sign(b.id % 2 == 0);
            MathUtils.q1.set(Tmp.v31, r).mul(MathUtils.q2.set(Tmp.v31.crs(Tmp.v32.set(Vec3.X).setToRandomDirection()), r * Mathf.signs[i % 2]));

            Draw.color(colors[i]);
            Lines.stroke(thickness[i]);

            DrawUtils.panningCircle(reg,
            b.x, b.y, 1f, 1f,
            radius[i], 360f, 0f,
            MathUtils.q1, true, layerLow, layerHigh
            );

            Draw.color(glows[i]);
            Draw.blend(Blending.additive);

            DrawUtils.panningCircle(light,
            b.x, b.y, 5f, 5f,
            radius[i], 360f, 0f,
            MathUtils.q1, true, layerLow, layerHigh
            );

            Draw.blend();
        }

        Draw.reset();
        Mathf.rand.setSeed(seed);
    }
}
