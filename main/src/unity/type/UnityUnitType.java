package unity.type;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import unity.entities.*;
import unity.entities.Rotor.*;
import unity.gen.*;

public class UnityUnitType extends UnitType{
    // Common.
    public TextureRegion payloadCellRegion;

    // Copters.
    public final Seq<Rotor> rotors = new Seq<>(2);
    public float rotorDeathSlowdown = 0.01f;
    public float fallRotateSpeed = 2.5f;

    public UnityUnitType(String name){
        super(name);
        outlines = false; // We already generated the sprites when compiling, so...
    }

    @Override
    public void load(){
        super.load();

        for(Rotor rotor : rotors) rotor.load();
        payloadCellRegion = Core.atlas.find(name + "-cell-payload", cellRegion);
    }

    @Override
    public void init(){
        super.init();

        Seq<Rotor> mapped = new Seq<>();
        for(Rotor rotor : rotors){
            mapped.add(rotor);
            if(rotor.mirror){
                Rotor copy = rotor.copy();
                copy.x *= -1f;
                copy.speed *= -1f;
                copy.shadeSpeed *= -1f;
                copy.rotOffset += 360f / (copy.bladeCount * 2);

                mapped.add(copy);
            }
        }
        rotors.set(mapped);
    }

    @Override
    public void drawCell(Unit unit){
        if(unit.isAdded()){
            super.drawCell(unit);
        }else{
            applyColor(unit);

            Draw.color(cellColor(unit));
            Draw.rect(payloadCellRegion, unit.x, unit.y, unit.rotation - 90);
            Draw.reset();
        }
    }

    @Override
    public void draw(Unit unit){
        super.draw(unit);

        if(unit instanceof Copterc) drawRotors((Unit & Copterc)unit);
    }

    public <T extends Unit & Copterc> void drawRotors(T unit){
        applyColor(unit);

        RotorMount[] rotors = unit.rotors();
        for(RotorMount mount : rotors){
            Rotor rotor = mount.rotor;
            float x = unit.x + Angles.trnsx(unit.rotation - 90f, rotor.x, rotor.y);
            float y = unit.y + Angles.trnsy(unit.rotation - 90f, rotor.x, rotor.y);

            float alpha = Mathf.curve(unit.rotorSpeedScl(), 0.2f, 1f);
            Draw.color(0f, 0f, 0f, rotor.shadowAlpha);
            float rad = 1.2f;
            float size = Math.max(rotor.bladeRegion.width, rotor.bladeRegion.height) * Draw.scl;

            Draw.rect(softShadowRegion, x, y, size * rad * Draw.xscl, size * rad * Draw.yscl);

            Draw.color();
            Draw.alpha(alpha * rotor.ghostAlpha);

            Draw.rect(rotor.bladeGhostRegion, x, y, mount.rotorRot);
            Draw.rect(rotor.bladeShadeRegion, x, y, mount.rotorShadeRot);

            Draw.alpha(1f - alpha * rotor.bladeFade);
            for(int j = 0; j < rotor.bladeCount; j++){
                Draw.rect(rotor.bladeOutlineRegion, x, y,
                    unit.rotation - 90f
                    + 360f / rotor.bladeCount * j
                    + mount.rotorRot
                );
            }
        }

        for(RotorMount mount : rotors){
            Rotor rotor = mount.rotor;
            float x = unit.x + Angles.trnsx(unit.rotation - 90f, rotor.x, rotor.y);
            float y = unit.y + Angles.trnsy(unit.rotation - 90f, rotor.x, rotor.y);

            Draw.alpha(1f - Mathf.curve(unit.rotorSpeedScl(), 0.2f, 1f) * rotor.bladeFade);
            for(int j = 0; j < rotor.bladeCount; j++){
                Draw.rect(rotor.bladeRegion, x, y,
                    unit.rotation - 90f
                    + 360f / rotor.bladeCount * j
                    + mount.rotorRot
                );
            }

            Draw.color();
            Draw.rect(rotor.topRegion, x, y, unit.rotation - 90f);
        }

        Draw.reset();
    }
}
