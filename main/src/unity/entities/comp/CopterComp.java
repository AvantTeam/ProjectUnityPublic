package unity.entities.comp;

import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import unity.annotations.Annotations.*;
import unity.entities.*;
import unity.entities.Rotor.*;
import unity.type.*;

import static mindustry.Vars.*;

/**
 * @author GlennFolker
 * @author MEEPofFaith
 */
@SuppressWarnings("unused")
@EntityComponent
abstract class CopterComp implements Unitc{
    transient RotorMount[] rotors;
    transient float rotorSpeedScl = 1f;

    @Import UnitType type;
    @Import boolean dead;
    @Import float health, rotation;
    @Import int id;

    @Override
    public void add(){
        UnityUnitType type = (UnityUnitType)this.type;

        rotors = new RotorMount[type.rotors.size];
        for(int i = 0; i < rotors.length; i++){
            Rotor rotor = type.rotors.get(i);
            rotors[i] = new RotorMount(rotor);
            rotors[i].rotorRot = rotor.rotOffset;
            rotors[i].rotorShadeRot = rotor.rotOffset;
        }
    }

    @Override
    public void update(){
        UnityUnitType type = (UnityUnitType)this.type;
        if(dead || health < 0f){
            if(!net.client() || isLocal()) rotation += type.fallRotateSpeed * Mathf.signs[id % 2] * Time.delta;

            rotorSpeedScl = Mathf.lerpDelta(rotorSpeedScl, 0f, type.rotorDeathSlowdown);
        }else{
            rotorSpeedScl = Mathf.lerpDelta(rotorSpeedScl, 1f, type.rotorDeathSlowdown);
        }

        for(RotorMount rotor : rotors){
            rotor.rotorRot += rotor.rotor.speed * rotorSpeedScl * Time.delta;
            rotor.rotorRot %= 360f;

            rotor.rotorShadeRot += rotor.rotor.shadeSpeed * Time.delta;
            rotor.rotorShadeRot %= 360f;
        }
    }
}
