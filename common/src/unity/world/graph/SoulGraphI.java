package unity.world.graph;

import unity.annotations.Annotations.*;
import unity.world.graph.nodes.*;

/**
 * Only used to create a valid type-reference in the generated classes.
 * @author GlennFolker
 */
@GraphDef(SoulNodeTypeI.class)
public interface SoulGraphI<T extends SoulGraphI<T>> extends GraphI<T>{}
