package unity.entities;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.io.*;

/**
 * Defines a rotor type.
 * @author younggam
 * @author GlennFolker
 */
public class Rotor{
    public final String name;

    public TextureRegion bladeRegion, bladeOutlineRegion, bladeGhostRegion, bladeShadeRegion, topRegion;

    public boolean mirror;
    public float x;
    public float y;

    public float rotOffset = 0f;
    public float speed = 29f;
    public float shadeSpeed = 3f;
    public float ghostAlpha = 0.6f;
    public float shadowAlpha = 0.4f;
    public float bladeFade = 1f;

    public int bladeCount = 4;

    public Rotor(String name){
        this.name = name;
    }

    public void load(){
        bladeRegion = Core.atlas.find(name + "-blade");
        bladeOutlineRegion = Core.atlas.find(name + "-blade-outline");
        bladeGhostRegion = Core.atlas.find(name + "-blade-ghost");
        bladeShadeRegion = Core.atlas.find(name + "-blade-shade");
        topRegion = Core.atlas.find(name + "-top");
    }

    public Rotor copy(){
        return JsonIO.copy(this, new Rotor(name));
    }

    /** Rotor entities that are mounted in units or other stuff. */
    public static class RotorMount{
        public final Rotor rotor;
        public float rotorRot;
        public float rotorShadeRot;

        public RotorMount(Rotor rotor){
            this.rotor = rotor;
        }
    }
}
