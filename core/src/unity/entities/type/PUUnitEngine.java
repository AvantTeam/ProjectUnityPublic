package unity.entities.type;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.type.UnitType.*;

public class PUUnitEngine extends UnitEngine{
    public Color color;

    public PUUnitEngine(){}

    public PUUnitEngine(float x, float y, float radius, float rotation){
        this(x, y, radius, rotation, null);
    }

    public PUUnitEngine(float x, float y, float radius, float rotation, Color color){
        super(x, y, radius, rotation);
        this.color = color;
    }

    @Override
    public void draw(Unit unit){
        UnitType type = unit.type;
        float scale = type.useEngineElevation ? unit.elevation : 1f;

        if(scale <= 0.0001f) return;

        float rot = unit.rotation - 90f;
        Color color = this.color != null ? this.color : type.engineColor != null ? type.engineColor : unit.team.color;

        Tmp.v1.set(x, y).rotate(rot);
        float ex = Tmp.v1.x, ey = Tmp.v1.y;

        Draw.color(color);
        Fill.circle(
        unit.x + ex,
        unit.y + ey,
        (radius + Mathf.absin(Time.time, 2f, radius / 4f)) * scale
        );

        Draw.color(type.engineColorInner);
        Fill.circle(
        unit.x + ex - Angles.trnsx(rot + rotation, 1f),
        unit.y + ey - Angles.trnsy(rot + rotation, 1f),
        (radius + Mathf.absin(Time.time, 2f, radius / 4f)) / 2f * scale
        );
    }
}
