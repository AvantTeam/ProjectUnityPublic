package unity.parts;

import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import unity.content.*;
import unity.parts.ModularConstructBuilder.*;
import unity.parts.PanelDoodadType.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

// OK this is the plan:
// ModularConstructBuilder -> actual edits the bytes, add Modules, removes them, loads, saves, compresses, formats, etc.
// ModularConstruct -> 'cache', stuff related to a complete modular contruct is stored here, ie. positions, stats, cosmetic vertexes etc.
public class ModularConstruct implements Serializable{
    public static ObjectMap<Unitc, ModularConstruct> cache = new ObjectMap<>();
    public static ModularConstruct placeholder = new ModularConstruct(1, 1, new long[]{
    ModularConstructBuilder.PartData.create(UnityParts.smallRoot.id, 0, 0)
    });

    //for editor -> unit
    private static class ConstructBuildkey{
        long[] data;

        public ConstructBuildkey(long[] data){
            this.data = data;
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(!(o instanceof ConstructBuildkey)) return false;
            ConstructBuildkey that = (ConstructBuildkey)o;
            return Arrays.equals(data, that.data);
        }

        @Override
        public int hashCode(){
            return Arrays.hashCode(data);
        }
    }

    private static ObjectMap<ConstructBuildkey, ModularConstruct> createdCache = new ObjectMap<>();

    //for data -> unit
    private static class ConstructDatakey{
        byte[] data;

        public ConstructDatakey(byte[] data){
            this.data = data;
        }

        @Override
        public boolean equals(Object o){
            if(this == o) return true;
            if(!(o instanceof ConstructDatakey)) return false;
            ConstructDatakey that = (ConstructDatakey)o;
            return Arrays.equals(data, that.data);
        }

        @Override
        public int hashCode(){
            return Arrays.hashCode(data);
        }
    }

    private static ObjectMap<ConstructDatakey, ModularConstruct> dataCache = new ObjectMap<>();

    public ModularPart[][] parts;
    public Seq<ModularPart> partlist = new Seq<>();
    public Seq<ModularPart> hasCustomDraw = new Seq<>();
    public Seq<ModularPart> hasCustomUpdate = new Seq<>();

    public long[] data;
    public byte[] compressedData;

    ModularPartStatMap statMap;

    private ModularConstruct(int w, int h, long[] data){
        try{
            parts = new ModularPart[w][h];
            for(int index = 0; index < data.length; index++){

                ModularPart part = PartData.get(data[index]);
                int pw = (part.rotation & 1) == 0 ? part.type.w : part.type.h;
                int ph = (part.rotation & 1) == 0 ? part.type.h : part.type.w;
                for(int i = 0; i < pw; i++){
                    for(int j = 0; j < ph; j++){
                        parts[part.x + i][part.y + j] = part;
                    }
                }
                part.ax = part.x - w * 0.5f + 0.5f;
                part.ay = part.y - h * 0.5f + 0.5f;
                part.cx = part.x - w * 0.5f + part.type.w * 0.5f;
                part.cy = part.y - h * 0.5f + part.type.h * 0.5f;
                partlist.add(part);
                part.prop_index = partlist.size-1;
                if(part.type.open || part.type.hasCellDecal || part.type.hasExtraDecal){
                    hasCustomDraw.add(part); // replace.
                }
                if(part.type.updates){
                    hasCustomUpdate.add(part);
                }
            }
            //for(ModularPart mp:partlist){
            //    mp.type.setupPanellingIndex(mp,parts);
            //}
            //this.data = data;
            // populate parts.
            // d
        }catch(Exception e){
            Log.err(e.toString());
            var est = e.getStackTrace();
            for(StackTraceElement st : est){
                Log.err(st.toString());
            }
        }
        this.data = new long[data.length];
        System.arraycopy(data,0,this.data,0,data.length);

    }

    public static ModularConstruct get(byte[] data){
        var key = new ConstructDatakey(data);
        if(dataCache.containsKey(key)){
            Log.info("ModularConstruct: created data construct exists!");
            return dataCache.get(key);
        }
        ModularConstructBuilder b = ModularConstructBuilder.decompressAndParse(data);
        var cons = b.createConstruct(false);
        cons.compressedData = data;
        dataCache.put(key, cons);
        return cons;
    }

    public static ModularConstruct get(int w, int h, long[] data){
        var key = new ConstructBuildkey(data);
        if(createdCache.containsKey(key)){
            Log.info("ModularConstruct: created construct exists!");
            return createdCache.get(key);
        }
        var cons = new ModularConstruct(w, h, data);
        createdCache.put(key, cons);
        return cons;
    }

    public <T extends ModularPartStatMap> T getStatMap(Class<T> maptype){

        if(statMap == null){
            try{
                statMap = maptype.getConstructor().newInstance();
            }catch(NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e){
                Log.err(e.toString() + "\n"+ Arrays.toString(e.getStackTrace()));
            }
            ModularConstructBuilder.getStats(this, statMap);
        }
        return (T)statMap;
    }

    public byte[] getCompressedData(){
        if(compressedData == null){
            var c = new ModularConstructBuilder(parts.length, parts[0].length);
            c.parts = this.data;
            c.findRoot();
            c.rebuildValid();
            compressedData = c.exportAndCompress();
            Log.info("Had to compress from scratch..");
        }
        return compressedData;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof ModularConstruct)) return false;
        ModularConstruct that = (ModularConstruct)o;
        return Arrays.equals(parts, that.parts) && Objects.equals(partlist, that.partlist) && Objects.equals(hasCustomDraw, that.hasCustomDraw) && Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode(){
        int result = Objects.hash(partlist, hasCustomDraw);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
