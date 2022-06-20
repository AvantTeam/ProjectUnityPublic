package unity.world.graph;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.world.*;

//interface bc it can be connected to any building type. esp turrets.
public interface GraphBlock{
    Block getBuild();
    GraphBlockConfig getConfig();
    default TextureRegion loadTex(String n){
           return Core.atlas.find(getBuild().name+"-"+n);
       }
}
