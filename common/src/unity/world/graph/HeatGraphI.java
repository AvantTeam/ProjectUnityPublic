package unity.world.graph;

import unity.annotations.Annotations.*;
import unity.world.graph.nodes.*;

/**
 * Only used to create a valid type-reference in the generated classes.
 * @author GlennFolker
 */
@GraphDef(HeatNodeTypeI.class)
public interface HeatGraphI<T extends HeatGraphI<T>> extends GraphI<T>{}
