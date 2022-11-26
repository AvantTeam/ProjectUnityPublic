package unity.world.graph;

import unity.annotations.Annotations.*;

/**
 * Only used to be known by the annotation processor. Methods defined here are only necessary to be known by the
 * generated classes. Define other abstract methods in the base implementation in the {@code :core} module.
 * @author GlennFolker
 */
@GraphBase
public interface GraphI<T extends GraphI<T>>{
    void update();
}
