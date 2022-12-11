package unity.world.blocks.power;

import arc.scene.ui.layout.*;
import arc.util.io.*;

public class SoulSource extends SoulTransmitter{
    public SoulSource(String name){
        super(name);
        config(Float.class, (SoulSourceBuild build, Float value) -> build.prodScale = value);
    }

    public class SoulSourceBuild extends SoulTransmitterBuild{
        public float prodScale = 0f;

        @Override
        public void updateTile(){
            soulNode.prodEfficiency(prodScale);
            super.updateTile();
        }

        @Override
        public void buildConfiguration(Table table){
            super.buildConfiguration(table);
            table.slider(0f, 1f, 0.01f, prodScale, true, this::configure);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(prodScale);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            prodScale = read.f();
        }
    }
}
