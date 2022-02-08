package unity.world.graph;

import mindustry.world.*;

//interface bc it can be connected to any building type. esp turrets.
public interface GraphBlock{
    Block getBuild();
    GraphBlockConfig getConfig();
}
