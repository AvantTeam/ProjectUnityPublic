package unity.world.graph.nodes;

import arc.graphics.*;
import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;

@GraphNodeDef
public interface HeatNodeTypeI<T extends HeatGraphI<T>> extends GraphNodeTypeI<T>{
    @Override
    <E extends Building & GraphBuild> HeatNodeI<T> create(E build);

    public interface HeatNodeI<T extends HeatGraphI<T>> extends GraphNodeI<T>{
        Color heatColor();
        void heatColor(Color input);

        float generateHeat(float targetTemp, float eff);
        void generateHeat();

        float getTemp();
        void setTemp(float temp);

        void affectUnit(Unit unit, float intensityScl);
    }
}
