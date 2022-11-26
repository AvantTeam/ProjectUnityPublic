package unity.world.graph;

import unity.annotations.Annotations.*;

@GraphBase
public interface GraphI<T extends GraphI<T>>{
    void update();

    int type();
}
