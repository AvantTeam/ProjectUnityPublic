package unity.world.systems;

import mindustry.io.*;
import mindustry.io.SaveFileReader.*;

import java.io.*;
import java.util.concurrent.*;

//fluid on the ground :0
public class GroundFluidControl implements CustomChunk{
    int iteration;
    long[][] fluid;

    volatile int w,h;


    CyclicBarrier threadBarrier;

    public GroundFluidControl(){
        SaveVersion.addCustomChunk("ground-fluid-data", this);
    }

    @Override
    public void write(DataOutput stream) throws IOException{

    }

    @Override
    public void read(DataInput stream) throws IOException{

    }

    //well its what the struct annotation does (debateful if its faster single threaded, but will defs carry good multithreaded performance (less data being moved)
    public static final class GroundFluidData{
        public static final long bitMaskAmount = 0x00000000FFFFFFFFL; //the last part is allocated to this float.
        public static final long bitMaskFluid =  0x0000FFFF00000000L; //another part is allocated to the fluid id

        public static float amount(long g){ return Float.intBitsToFloat((int)(bitMaskAmount&g));}
        public static long amountSet(long g,float am){ return (g&~bitMaskAmount) | Float.floatToIntBits(am);}

        public static int liquid(long g){ return (int)(bitMaskFluid&g);}
        public static long liquid(long g, int fluid){ return (g&~bitMaskFluid)|((long)fluid <<32L);}

        public static long setLiquid(int fluid, float amount){return ((long)fluid <<32L)|Float.floatToIntBits(amount);}
    }
}
