package unity.world.graph;

import unity.annotations.Annotations.*;

@GraphConnectorBase
public interface GraphConnectorTypeI<T extends GraphI>{
    long graphType();

    public interface GraphConnectorI<T extends GraphI>{
        T graph();
    }
}
