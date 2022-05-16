package unity.entities.merge;

import arc.func.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.annotations.Annotations.*;
import unity.gen.Stemc.*;
import unity.world.meta.*;

@SuppressWarnings({"unchecked", "unused"})
@MergeComponent
class StemComp extends Block{
    @ReadOnly Cons<StemBuildc> drawStem = e -> {};
    @ReadOnly Cons<StemBuildc> updateStem = e -> {};

    public StemComp(String name){
        super(name);
    }
    
    public <T extends StemBuildc> void draw(Cons<T> draw){
        drawStem = (Cons<StemBuildc>)draw;
    }

    public <T extends StemBuildc> void update(Cons<T> update){
        updateStem = (Cons<StemBuildc>)update;
    }

    public class StemBuildComp extends Building{
        transient @ReadOnly StemData data = new StemData();

        @Override
        public void draw(){
            drawStem.get(self());
        }

        @Override
        public void updateTile(){
            updateStem.get(self());
        }

        @Override
        public void write(Writes write){
            data.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            data.read(read);
        }
    }
}
