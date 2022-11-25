package unity.gensrc;

import unity.annotations.Annotations.*;
import unity.world.graph.*;

final class GraphComposer{
    /** Monolith unit. */
    @GraphCompose(HeatGraphI.class) Object heat;

    private GraphComposer(){
        throw new AssertionError();
    }
}
