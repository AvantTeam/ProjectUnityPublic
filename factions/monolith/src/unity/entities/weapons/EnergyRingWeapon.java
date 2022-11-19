package unity.entities.weapons;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static unity.graphics.MonolithPal.monolithLighter;

/**
 * Sprite-less weapon composed of revolving energy rings.
 * @author GlennFolker
 */
public class EnergyRingWeapon extends Weapon{
    public final Seq<Ring> rings = new Seq<>(4);

    public float aggressionScale = 3f;
    public float aggressionSpeed = 0.2f;
    public float cooldownSpeed = 0.08f;

    public Color eyeColor = monolithLighter;
    public float eyeRadius = 2.5f;

    public EnergyRingWeapon(){
        super("");
        mountType = weapon -> new EnergyRingMount((EnergyRingWeapon)weapon);
    }

    @Override
    public void update(Unit unit, WeaponMount mount){
        super.update(unit, mount);

        EnergyRingMount m = (EnergyRingMount)mount;
        m.aggression = (m.target != null || m.shoot)
        ? Mathf.lerpDelta(m.aggression, 1f, aggressionSpeed)
        : Mathf.lerpDelta(m.aggression, 0f, cooldownSpeed);
        m.time += Time.delta + m.aggression * Time.delta * aggressionScale;
    }

    @Override
    public void draw(Unit unit, WeaponMount mount){
        float z = Draw.z();
        Draw.z(z + layerOffset);

        EnergyRingMount m = (EnergyRingMount)mount;

        float rot = unit.rotation - 90f;
        Tmp.v1.trns(rot, x, y).add(unit);

        for(Ring ring : rings){
            int sign = Mathf.sign(ring.flip ^ unit.id % 2 == 0);
            float rotation = ring.angleOffset * sign + (ring.rotate ? (m.time * sign) : (rot + mount.rotation));

            Lines.stroke(ring.thickness, ring.color);
            for(int i = 0; i < ring.divisions; i++){
                float angleStep = 360f / ring.divisions, sect = angleStep - ring.divisionSeparation;
                Lines.arc(Tmp.v1.x, Tmp.v1.y,
                ring.radius, ring.divisions == 1 ? 1f : (sect / 360f),
                rotation - sect / 2f + angleStep * i
                );
            }

            for(int i = 0; i < ring.spikes; i++){
                float spikeRotation = rotation + ring.spikeRotOffset + 360f / ring.spikes * i;

                Tmp.v2.trns(spikeRotation, 0f, ring.radius + ring.spikeOffset).add(Tmp.v1);
                Drawf.tri(Tmp.v2.x, Tmp.v2.y, ring.spikeWidth, ring.spikeLength, spikeRotation + 90f);
            }
        }

        rot += m.rotation;
        Tmp.v1.add(Tmp.v2.trns(rot, shootX, shootY));

        Draw.color(eyeColor);
        Fill.circle(Tmp.v1.x, Tmp.v1.y, eyeRadius);

        Draw.z(z);
    }

    @Override
    public void drawOutline(Unit unit, WeaponMount mount){}

    public static class Ring{
        public Color color = monolithLighter;

        public float thickness = 1.5f;
        public float radius = 4.5f;
        /** If {@code false}, the ring uses the weapon mount's rotation instead. */
        public boolean rotate = true;
        public float rotateSpeed = 2f;
        public float angleOffset = 0f;
        public boolean flip;

        public int divisions = 1;
        public float divisionSeparation = 12f;

        public int spikes = 0;
        public float spikeOffset = 1f;
        public float spikeRotOffset;
        public float spikeWidth = 1.5f;
        public float spikeLength = 3f;

        @Override
        public String toString(){
            return "Ring{" +
            "color=" + color + ", thickness=" + thickness + ", radius=" + radius + ", rotate=" + rotate +
            ", rotateSpeed=" + rotateSpeed + ", angleOffset=" + angleOffset + ", flip=" + flip + ", divisions=" + divisions +
            ", divisionSeparation=" + divisionSeparation + ", spikes=" + spikes + ", spikeOffset=" + spikeOffset +
            ", spikeRotOffset=" + spikeRotOffset + ", spikeWidth=" + spikeWidth + ", spikeLength=" + spikeLength +
            '}';
        }
    }

    public static class EnergyRingMount extends WeaponMount{
        public float time;
        public float aggression;

        public EnergyRingMount(EnergyRingWeapon weapon){
            super(weapon);
        }
    }
}
