package unity.world.graph.prop;

import unity.world.blocks.power.*;
import unity.world.graph.GraphBlock.*;

/** @author GlennFolker */
public class SoulTransmitterProps extends SoulProps{
    public final SoulTransmitter block;

    public SoulTransmitterProps(SoulTransmitter block){
        this.block = block;
    }

    @Override
    public SoulTransmitterPropsEntity create(GraphBuild build){
        return new SoulTransmitterPropsEntity(build);
    }

    public class SoulTransmitterPropsEntity extends SoulPropsEntity{
        public SoulTransmitterPropsEntity(GraphBuild build){
            super(build);
        }
    }
}
