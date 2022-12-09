package unity.world.graph.nodes;

import arc.graphics.*;
import mindustry.gen.*;
import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;

/**
 * Define public methods for heat graph nodes here.
 * @author GlennFolker
 */
@GraphNodeDef
public interface SoulNodeTypeI<T extends SoulGraphI<T>> extends GraphNodeTypeI<T>{
    @Override
    <E extends Building & GraphBuild> SoulNodeI<T> create(E build);

    public interface SoulNodeI<T extends SoulGraphI<T>> extends GraphNodeI<T>{
        float production();
        float consumption();
        float resistance();

        float safeLimit();
        float absoluteLimit();

        float overloadDump();
        float overloadScale();

        boolean canReceive();
        boolean canTransfer();

        float prodEfficiency();
        void prodEfficiency(float prodEfficiency);

        float amount();
        float transferred();

        float visualAmount();
        float visualTransferred();
    }
}
