package unity.world.meta;

import arc.util.io.*;

/** @author GlennFolker */
public class StemData{
    public StemData genericValue;
    public boolean boolValue;
    public byte byteValue;
    public short shortValue;
    public int intValue;
    public long longValue;
    public float floatValue;
    public double doubleValue;
    public String stringValue;

    public void write(Writes write){
        write.bool(genericValue != null);
        if(genericValue != null) genericValue.write(write);

        write.bool(boolValue);
        write.b(byteValue);
        write.s(shortValue);
        write.i(intValue);
        write.l(longValue);
        write.f(floatValue);
        write.d(doubleValue);

        write.bool(stringValue != null);
        if(stringValue != null) write.str(stringValue);
    }

    public void read(Reads read){
        boolean hasGeneric = read.bool();
        if(hasGeneric){
            genericValue = new StemData();
            genericValue.read(read);
        }

        boolValue = read.bool();
        byteValue = read.b();
        shortValue = read.s();
        intValue = read.i();
        longValue = read.l();
        floatValue = read.f();
        doubleValue = read.d();

        if(read.bool()) stringValue = read.str();
    }
}
