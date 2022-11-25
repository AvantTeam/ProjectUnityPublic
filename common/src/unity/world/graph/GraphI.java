package unity.world.graph;

import unity.annotations.Annotations.*;

public interface GraphI<T extends GraphI>{
    void update();
    void onUpdate();
}
