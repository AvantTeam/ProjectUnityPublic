package unity.world.blocks.power;

import unity.gen.graph.*;

public class SoulSource extends SoulBlock{
    public SoulSource(String name){
        super(name);
    }

    public class SoulSourceBuild extends SoulBuild{
        @Override
        public void updateTile(){
            super.updateTile();
            soulNode.prodEfficiency(1f);
        }
    }
}
