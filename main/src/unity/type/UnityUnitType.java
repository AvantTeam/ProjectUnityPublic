package unity.type;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import unity.entities.*;
import unity.entities.Rotor.*;
import unity.gen.*;
import unity.parts.*;
import unity.parts.types.*;
import unity.util.*;

import static mindustry.Vars.headless;

public class UnityUnitType extends UnitType{
    // Common.
    public TextureRegion payloadCellRegion;

    // Copters.
    public final Seq<Rotor> rotors = new Seq<>(2);
    public float rotorDeathSlowdown = 0.01f;
    public float fallRotateSpeed = 2.5f;

    // Modular Units.
    public Seq<String> templates = new Seq<>();

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
            if(unit instanceof ModularUnitc){
                drawModularCell((Unit & ModularUnitc)unit);
                return;
            }
            super.drawCell(unit);
        }else{
            if(unit instanceof ModularUnitc){
                //payloading
                drawModularBody((Unit & ModularUnitc)unit);
                drawModularCell((Unit & ModularUnitc)unit);
                drawWeapons(unit);
                return;
            }
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

    @Override
    public void drawOutline(Unit unit){
        //if(unit instanceof M){
        //
        //};
        super.drawOutline(unit);
    }
    @Override
    public void drawBody(Unit unit){
        if(unit instanceof ModularUnitc){
            drawModularBody((Unit & ModularUnitc)unit);
            return;
        }
        super.drawBody(unit);
    }
    @Override
    public void drawSoftShadow(Unit unit, float alpha){
        if(unit instanceof ModularUnitc){
            drawModularBodySoftShadow((Unit & ModularUnitc)unit,alpha);
            return;
        }
        super.drawSoftShadow(unit,alpha);

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

    public <T extends Unit & ModularUnitc> void drawModularBodySoftShadow(T unit, float alpha){
        Draw.color(0, 0, 0, 0.4f * alpha);
        float rad = 1.6f;
        float size = unit.hitSize;
        Draw.rect(softShadowRegion, unit, size * rad * Draw.xscl, size * rad * Draw.yscl, unit.rotation - 90);
        Draw.color();
    }

    public <T extends Unit & ModularUnitc> void drawModularBody(T unit){
        applyColor(unit);
        DrawTransform dt = new DrawTransform(new Vec2(unit.x,unit.y),unit.rotation);
        var construct = unit.construct();
        if(construct!=null){
            unit.doodadlist().each(d->{
                d.drawOutline(dt);
            });
            construct.hasCustomDraw.each((p) -> {
                p.type.drawOutline(dt, p, unit);
            });
            construct.hasCustomDraw.each((p) -> {
                p.type.draw(dt, p, unit);
            });
            unit.doodadlist().each(d->{
                d.drawTop(dt);
            });
            construct.hasCustomDraw.each((p) -> {
                p.type.drawTop(dt, p);
            });
        }else{
            if(unit.constructdata()!=null && unit.constructdata().length>0){
                unit.construct(ModularConstruct.get(unit.constructdata()));
                UnitDoodadGenerator.initDoodads(unit.construct().parts.length, unit.doodadlist(), unit.construct());
            }
        }
        Draw.reset();
    }
    public <T extends Unit & ModularUnitc> void drawModularCell(T  unit){
        applyColor(unit);
        Draw.color(cellColor(unit));
        DrawTransform dt = new DrawTransform(new Vec2(unit.x,unit.y),unit.rotation);
        var construct = unit.construct();
        if(construct!=null){
            construct.hasCustomDraw.each((p) -> {
                p.type.drawCell(dt, p);
            });
        }
        Draw.reset();
    }


    public Unit spawn(Team t, float x, float y, ModularConstruct data){
        var unit = this.create(t);
        unit.set(x, y);
        ModularConstruct.cache.put(unit,data);
        unit.add();
        return unit;
    }
}
