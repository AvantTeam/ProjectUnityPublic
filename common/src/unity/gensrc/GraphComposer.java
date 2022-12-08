package unity.gensrc;

import unity.annotations.Annotations.*;
import unity.world.graph.*;

final class GraphComposer{
    /** Heat block. */
    @GraphCompose(HeatGraphI.class) Object heatBlock;

    /** Soul block. */
    @GraphCompose(SoulGraphI.class) Object soulBlock;

    private GraphComposer(){
        throw new AssertionError();
    }
}
