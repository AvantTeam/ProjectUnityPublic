package unity.parts;

import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

//probably immutable
public class ModularConstruct implements Serializable{
    public static ObjectMap<Unitc,byte[]> cache = new ObjectMap<>();
    public static ModularConstruct test = new ModularConstruct(new byte[]{
        sb(3),sb(3),
        sb(1),sb(1),sb(1),
        sb(0),sb(2),sb(1),
        sb(2),sb(2),sb(0),
        sb(0),sb(0),sb(1),
        sb(2),sb(0),sb(0),
    });

    public ModularPart [][] parts;
    public Seq<ModularPart> partlist = new Seq<>();
    static final int idSize = 1;
    public byte[] data;

    //designed to be synced over the net
    //byte[] structure
    // w , h
    // (type-id,  x ,y )-- repeats
    public ModularConstruct(byte[] data){
        try{
            int w = ub(data[0]);
            int h = ub(data[1]);
            parts = new ModularPart[w][h];
            int partamount = (data.length - 2) / (2 + idSize);
            int blocksize = (2 + idSize);
            for(int i = 0; i < partamount; i++){
                int id = getID(data, 2 + i * blocksize);
                int x = ub(data[2 + i * blocksize + idSize]);
                int y = ub(data[2 + i * blocksize + idSize + 1]);
                var part = ModularPartType.getPartFromId(id).create(x, y);
                partlist.add(part);
                part.ax = x-w*0.5f + 0.5f;
                part.ay = y-h*0.5f + 0.5f;

                for(int px = 0; px < part.type.w; px++){
                    for(int py = 0; py < part.type.h; py++){
                        parts[x + px][y + py] = part;
                    }
                }
            }
            for(ModularPart mp:partlist){
                mp.type.setupPanellingIndex(mp,parts);
            }
            this.data = data;
        }catch(Exception e){
            Log.err(e.toString());
            var est = e.getStackTrace();
            for(StackTraceElement st:est){
                Log.err(st.toString());
            }
        }
    }
    public static int getID(byte[] data, int s){
        if(idSize==1){
            return ub(data[s]);
        }
        int id = 0;
        for(int i = 0;i<idSize;i++){
            id += ub(data[s+i])*(256<<(i*8)); //ignore warning, its for if there is somehow more then 255 parts
        }
        return id;
    }
    public static void writeID(byte[] data, int s, int id){
        if(idSize==1){
            data[s]=sb(id);
        }
        for(int i = 0;i<idSize;i++){
            data[s+i]= sb(id & (0xFF<<(i*8))); //ignore warning, its for if there is somehow more then 255 parts
        }
    }
    public static ModularConstruct read(Reads reads){
        int length = reads.b();
        byte[] bytes = new byte[length];
        return new ModularConstruct(reads.b(bytes));
    }
    public void write(Writes writes){
        int len = partlist.size*3 + 2;
        writes.b(len);
        writes.b(data);
    }
    public static int ub(byte a){
        return (int)(a) + 128;
    }
    public static byte sb(int a){
        return (byte)(a - 128);
    }
}
