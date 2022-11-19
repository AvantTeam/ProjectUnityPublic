package unity.content;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import unity.graphics.*;
import unity.graphics.BaseTrail.*;
import unity.graphics.MultiTrail.*;
import unity.graphics.trail.*;
import unity.mod.*;
import unity.util.*;

import static unity.graphics.MonolithPal.*;

/**
 * Provides various types of trails used by {@linkplain Faction#monolith monolith} entities.
 * @author GlennFolker
 */
public final class MonolithTrails{
    private MonolithTrails(){
        throw new AssertionError();
    }

    public static TexturedTrail singlePhantasmal(int length, TrailAttrib... attributes){
        return new TexturedTrail(Core.atlas.find("unity-phantasmal-trail"), length, attributes){{
            blend = Blending.additive;
            fadeInterp = Interp.pow2In;
            sideFadeInterp = Interp.pow3In;
            mixInterp = Interp.pow10In;
            gradientInterp = Interp.pow10Out;
            fadeColor = new Color(0.3f, 0.5f, 1f);
            shrink = 0f;
            fadeAlpha = 1f;
            mixAlpha = 1f;
            trailChance = 0.4f;
            trailWidth = 1.6f;
            trailColor = monolithLight;
        }};
    }

    public static TexturedTrail phantasmalExhaust(int length, TrailAttrib... attributes){
        TexturedTrail t = singlePhantasmal(length, attributes);
        t.capRegion = Core.atlas.find("clear");
        t.minDst = 0.4f;
        t.fadeInterp = e -> (1f - MathUtils.pow25In.apply(e)) * Interp.pow3In.apply(e);
        t.sideFadeInterp = Interp.pow5In;
        t.mixAlpha = 0f;
        t.trailChance = 0f;
        t.shrink = -3.6f;

        return t;
    }

    public static MultiTrail phantasmal(int length, int strandsAmount){
        return phantasmal(length, strandsAmount, 3.6f, 3.5f, -1f, 0f);
    }

    public static MultiTrail phantasmal(int length, int strandsAmount, VelAttrib vel){
        return phantasmal(BaseTrail::rot, length, strandsAmount, 3.6f, 3.5f, -1f, 0f, vel);
    }

    public static MultiTrail phantasmal(RotationHandler rot, int length, int strandsAmount){
        return phantasmal(rot, length, strandsAmount, 3.6f, 3.5f, -1f, 0f, null);
    }

    public static MultiTrail phantasmal(int length, int strandsAmount, float scale, float magnitude, float speedThreshold, float offsetY){
        return phantasmal(BaseTrail::rot, length, strandsAmount, scale, magnitude, speedThreshold, offsetY, null);
    }

    public static MultiTrail phantasmal(RotationHandler rot, int length, int strandsAmount, float scale, float magnitude, float speedThreshold, float offsetY){
        return phantasmal(rot, length, strandsAmount, scale, magnitude, speedThreshold, offsetY, null);
    }

    public static MultiTrail phantasmal(RotationHandler rot, int length, int strandsAmount, float scale, float magnitude, float speedThreshold, float offsetY, VelAttrib vel){
        TrailHold[] trails = new TrailHold[strandsAmount + 2];
        for(int i = 0; i < strandsAmount; i++){
            TexturedTrail t = singlePhantasmal(Mathf.round(length * 1.5f));
            t.trailWidth = 4.8f;

            trails[i] = new TrailHold(t, 0f, 0f, 0.16f);
        }

        trails[strandsAmount] = new TrailHold(singlePhantasmal(length));
        trails[strandsAmount + 1] = new TrailHold(phantasmalExhaust(Mathf.round(length * 0.5f)), 0f, 1.6f);

        float offset = Mathf.random(Mathf.PI2 * scale);
        return new MultiTrail(rot, vel, trails){
            @Override
            public void defUpdate(float x, float y, float width, float angle, float speed, float delta){
                float scl = Interp.pow2Out.apply(speedThreshold == -1f ? 1f : Mathf.clamp(speed / speedThreshold));

                angle = unconvRot(angle) - 90f;
                for(int i = 0; i < strandsAmount; i++){
                    Tmp.v1.trns(angle, Mathf.sin(Time.time + offset + (Mathf.PI2 * scale) * ((float)i / strandsAmount), scale, magnitude * width * scl), offsetY);

                    TrailHold trail = trails[i];
                    trail.trail.update(x + Tmp.v1.x, y + Tmp.v1.y, width * trail.width);
                }

                for(int i = strandsAmount; i < trails.length; i++){
                    TrailHold trail = trails[i];
                    Tmp.v1.trns(angle, trail.x, trail.y);

                    trail.trail.update(x + Tmp.v1.x, y + Tmp.v1.y, width * trail.width);
                }
            }
        };
    }

    public static TexturedTrail singleSoul(int length, TrailAttrib... attributes){
        return new TexturedTrail(Core.atlas.find("unity-soul-trail"), length, attributes){{
            blend = Blending.additive;
            fadeInterp = Interp.pow5In;
            sideFadeInterp = Interp.pow10In;
            mixInterp = Interp.pow5In;
            gradientInterp = Interp.pow5Out;
            fadeColor = new Color(0.1f, 0.2f, 1f);
            shrink = 1f;
            mixAlpha = 0.8f;
            fadeAlpha = 0.5f;
            trailChance = 0f;
            trailColor = monolithMid;
        }};
    }

    public static MultiTrail soul(int length){
        return soul(length, 6f, 2.2f, -1f);
    }

    public static MultiTrail soul(RotationHandler rot, int length){
        return soul(rot, length, 6f, 2.2f, -1f, null);
    }

    public static MultiTrail soul(int length, float speedThreshold){
        return soul(length, 6f, 2.2f, speedThreshold);
    }

    public static MultiTrail soul(RotationHandler rot, int length, float speedThreshold){
        return soul(rot, length, 6f, 2.2f, speedThreshold, null);
    }

    public static MultiTrail soul(RotationHandler rot, int length, float speedThreshold, VelAttrib vel){
        return soul(rot, length, 6f, 2.2f, speedThreshold, vel);
    }

    public static MultiTrail soul(int length, float scale, float magnitude, float speedThreshold){
        return soul(BaseTrail::rot, length, scale, magnitude, speedThreshold, null);
    }

    public static MultiTrail soul(RotationHandler rot, int length, float scale, float magnitude, float speedThreshold){
        return soul(rot, length, scale, magnitude, speedThreshold, null);
    }

    public static MultiTrail soul(RotationHandler rot, int length, float scale, float magnitude, float speedThreshold, VelAttrib vel){
        int strandsAmount = 3;

        TrailHold[] trails = new TrailHold[strandsAmount + 1];
        for(int i = 0; i < strandsAmount; i++){
            TexturedTrail t = singleSoul(Mathf.round(length * 1.5f));
            t.mixAlpha = 0f;

            trails[i] = new TrailHold(t, 0f, 0f, 0.56f);
        }

        trails[strandsAmount] = new TrailHold(singlePhantasmal(length), monolithLight);

        float dir = Mathf.sign(Mathf.chance(0.5f));
        return new MultiTrail(rot, vel, trails){
            float time = Time.time + Mathf.random(Mathf.PI2 * scale);

            @Override
            public void defUpdate(float x, float y, float width, float angle, float speed, float delta){
                angle = unconvRot(angle) - 90f;

                time += (speedThreshold == -1f ? 1f : Mathf.clamp(Mathf.dst(x, y, lastX, lastY) / Time.delta / speedThreshold)) * Time.delta;
                for(int i = 0; i < strandsAmount; i++){
                    float rad = (time + (Mathf.PI2 * scale) * ((float)i / strandsAmount)) * dir;
                    float scl = Mathf.map(Mathf.sin(rad, scale, 1f), -1f, 1f, 0.2f, 1f);
                    Tmp.v1.trns(angle, Mathf.cos(rad, scale, magnitude * width));

                    TrailHold trail = trails[i];
                    trail.trail.update(x + Tmp.v1.x, y + Tmp.v1.y, width * trail.width * scl);
                }

                TrailHold main = trails[strandsAmount];
                Tmp.v1.trns(angle, main.x, main.y);

                main.trail.update(x + Tmp.v1.x, y + Tmp.v1.y, width * main.width);
            }
        };
    }
}
