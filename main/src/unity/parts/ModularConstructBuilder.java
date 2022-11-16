package unity.parts;

import arc.struct.*;
import arc.util.*;
import mindustry.type.*;
import unity.util.*;

import java.util.*;

public class ModularConstructBuilder{
    public long[] parts;
    public boolean[] valid;
    public int w, h;
    public int rootIndex = -1;
    static final int idSize = 1;

    public Runnable onChange = () -> {
    };

    public ModularConstructBuilder(int w, int h){
        this.w = w;
        this.h = h;
        parts = new long[w*h];
        valid = new boolean[w*h];
    }

    public ModularConstructBuilder(byte[] data){
        paste(data);
    }



    public int index(int x,int y){
        return x+y*w;
    }

    public void clear(){
        for(int i = 0; i < parts.length; i++){
            parts[i] = 0;
        }
        rootIndex = -1;
        onChange.run();
        updatedItemReq = false;
    }

    private static final int flag = 0x6942;
    private static final int flagsizeBytes = 2;

    public void paste(byte[] data){
        if(readShort(data,0)!=flag){
            throw new IllegalArgumentException("ModularConstructBuilder.set takes in raw uncompressed, unformatted data.");
        }
        try{
            w = ub(data[flagsizeBytes]);
            h = ub(data[flagsizeBytes+1]);
            rootIndex = -1;
            if(w ==0 || h== 0){
                parts = new long[1];
                w = 1;
                h = 1;
                valid = new boolean[1];
                return;
            }
            parts = new long[w*h];
            int partamount = readShort(data,flagsizeBytes+2);
            int offset = flagsizeBytes+4;
            int blocksize = (3 + idSize);
            for(int i = 0; i < partamount; i++){
                int id = getID(data, offset + i * blocksize);
                int x = ub(data[offset + i * blocksize + idSize]);
                int y = ub(data[offset + i * blocksize + idSize + 1]);
                int o = ub(data[offset + i * blocksize + idSize + 2]);
                long part = PartData.create(id,x,y,o);
                placePartDirect(part,x,y);//todo at some point rotation is needed
            }
        }catch(Exception e){
            Log.err(e.toString());
            var est = e.getStackTrace();
            for(StackTraceElement st:est){
                Log.err(st.toString());
            }
        }
        if(parts == null){
            parts = new long[w*h];
            return;
        }
        valid = new boolean[w*h];
        if(rootIndex==-1){
            findRoot();
        }
        rebuildValid();
        updatedItemReq = false;
        onChange.run();
    }

    //wont change dimensions
    public void paste(ModularConstructBuilder e){
        clear();
        int ox = (w - e.w) / 2;
        int oy = (h - e.h) / 2;
        for(int i = 0; i < w; i++){
            for(int j = 0; j < h; j++){
                if(!e.isIn(i - ox, j - oy)){
                    continue;
                }
                int ind = e.index(i - ox,j - oy);
                if(e.parts[ind] == 0){
                    continue;
                }
                var epart = e.parts[ind];
                if(isHere(epart,i - ox, j - oy)){
                    placePartDirect(epart, i, j);
                }
            }
        }
        if(e.rootIndex!=-1){
            var root = e.parts[e.rootIndex];
            rootIndex = index(PartData.x(root) + ox, PartData.y(root) + oy);
        }
        rebuildValid();
    }


    public static boolean isHere(long data, int x,int y){
        return PartData.x(data) == x && PartData.y(data) == y;
    }
    public boolean isHere(long data, int index){
        return PartData.x(data) + PartData.y(data) * w == index;
    }
    public int indexOf(long data){
        return index(PartData.x(data),PartData.y(data));
    }

    public boolean isIn(int x, int y){
        return !(x < 0 || y < 0 || x >= w || y >= h);
    }

    public boolean canFit(ModularPart p, int ox, int oy){
        return isIn(p.x + ox, p.y + oy) && isIn(p.x + p.type.w - 1 + ox, p.y + p.type.h - 1 + oy);
    }

    public boolean canFit(ModularPartType p, int ox, int oy){
        return isIn(ox, oy) && isIn(p.w - 1 + ox, p.h - 1 + oy);
    }
    public LongSeq getList(boolean includeInvalid){
        LongSeq out = new LongSeq();
        for(int i = 0; i < parts.length; i++){
            if((includeInvalid || valid[i]) && parts[i] != 0 && isHere(parts[i],i)){
                out.add(parts[i]);
            }
        }
        return out;
    }

    public void rebuildValid(){
        Arrays.fill(valid,false);
        if(rootIndex == -1){
            return;
        }

        IntSeq front = new IntSeq();
        front.add(rootIndex);
        valid[rootIndex] = true;
        int ptx,pty;
        while(!front.isEmpty()){
            var pt = front.pop();
            ptx = pt%w;
            pty = pt/w;
            if(ptx > 0 && parts[pt-1] != 0 && !valid[pt-1]){ //left
                valid[pt-1] = true;
                front.add(pt-1);
            }
            if(pty > 0 && parts[pt-w] != 0 && !valid[pt-w]){ //up
                valid[pt-w] = true;
                front.add(pt-w);
            }

            if(ptx < w - 1 && parts[pt+1] != 0 && !valid[pt+1]){ //right
                valid[pt+1] = true;
                front.add(pt+1);
            }
            if(pty < h - 1 && parts[pt+w] != 0 && !valid[pt+w]){ //down
                valid[pt+w] = true;
                front.add(pt+w);
            }
        }
    }

    boolean updatedItemReq = false;
    public ItemSeq itemRequirements;

    public ItemSeq itemRequirements(){
        if(!updatedItemReq){
            itemRequirements = new ItemSeq();
            var list = getList(true);
            for(int i =0;i<list.size;i++){
                itemRequirements.add(PartData.Type(list.get(i)).cost);
            }
            updatedItemReq = true;
        }
        return itemRequirements;
    }
    protected byte[] serialise(LongSeq partseq, int offx, int offy, int w, int h){
        Log.info("serialising construct:"+offx+","+offy+","+w+","+h);
        int headerOffset = 4 + flagsizeBytes;
        byte[] output = new byte[headerOffset + partseq.size * (idSize + 3)];
        writeShort(output,0,flag);
        output[flagsizeBytes] = b(w);
        output[flagsizeBytes+1] = b(h);
        writeShort(output,flagsizeBytes+2,partseq.size);
        int blocksize = (idSize + 3);
        for(int i = 0; i < partseq.size; i++){
            var part = partseq.get(i);
            writeID(output, headerOffset + blocksize * i, PartData.Id(part));
            output[headerOffset + blocksize * i + idSize] = b(PartData.x(part)-offx);
            output[headerOffset + blocksize * i + idSize + 1] = b(PartData.y(part)-offy);
            output[headerOffset + blocksize * i + idSize + 2] = b(PartData.misc(part));
        }
        return output;
    }
    public byte[] exportFull(){
        return serialise(getList(true),0,0,w,h);
    }

    //trims empty tiles and invalid modules
    private int[] findCropped(LongSeq partsList){
        int maxx = 0, minx = 256;
        int maxy = 0, miny = 256;
        int index = 0;
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                if(valid[index] && parts[index] != 0){
                    maxx = Math.max(x, maxx);
                    minx = Math.min(x, minx);
                    maxy = Math.max(y, maxy);
                    miny = Math.min(y, miny);
                    if(isHere(parts[index],x,y)){
                        partsList.add(parts[index]);
                    }
                }
                index++;
            }
        }
        minx = Math.min(minx,maxx);
        miny = Math.min(miny,maxy);
        return new int[]{minx,miny,maxx,maxy};
    }

    private byte[] serialiseCropped(){
        LongSeq partsList = new LongSeq();
        var bounds = findCropped(partsList);
        return serialise(partsList,bounds[0],bounds[1],Math.max(bounds[2] - bounds[0] + 1,0),Math.max(bounds[3] - bounds[1] + 1,0));
    }

    private static byte[] formatForCompression(byte[] unformat){
        byte[] formatted = new byte[unformat.length-flagsizeBytes];
        formatted[0] = unformat[flagsizeBytes+0];
        formatted[1] = unformat[flagsizeBytes+1];
        formatted[2] = unformat[flagsizeBytes+2];
        formatted[3] = unformat[flagsizeBytes+3];
        int parts = readShort(unformat,flagsizeBytes+2);
        int headerOffset = 4+flagsizeBytes;
        int formatHeaderOffset = 4;
        int xoff = parts;
        int yoff = parts*2;
        int othoff = parts*3;
        for(int i = headerOffset; i < headerOffset + parts*4; i += 4){
            int partind = (i - headerOffset) / 4;
            formatted[formatHeaderOffset + partind] = unformat[i];//id
            formatted[formatHeaderOffset + partind + xoff] = unformat[i + 1];//x
            if(partind > 0){
                formatted[formatHeaderOffset + partind + yoff] = (byte)(unformat[i + 2] - unformat[i - 2]);//y
            }else{
                formatted[formatHeaderOffset + partind + yoff] = unformat[i + 2];//y
            }
            formatted[formatHeaderOffset + partind + othoff] = unformat[i + 3];//misc
        }
        return formatted;
    }

    private static byte[] unformatFromCompression(byte[] format){
        byte[] unformatted = new byte[format.length+flagsizeBytes];
        writeShort(unformatted,0,flag);
        unformatted[flagsizeBytes] = format[0];
        unformatted[flagsizeBytes+1] = format[1];
        unformatted[flagsizeBytes+2] = format[2];
        unformatted[flagsizeBytes+3] = format[3];
        int parts = readShort(format,2);
        int headerOffset = flagsizeBytes+4;
        int formatHeaderOffset = 4;
        int idOffset = formatHeaderOffset;
        int xOffset = idOffset+parts;
        int yOffset = xOffset+parts;
        int miscOffset = yOffset+parts;
        for(int i = 0; i < parts; i ++){
            int partind = i*4;
            unformatted[headerOffset + partind] = format[idOffset + i];//id
            unformatted[headerOffset + partind + 1] = format[xOffset + i];//x
            if(i > 0){
                unformatted[headerOffset + partind + 2] = (byte)(format[yOffset + i] + unformatted[headerOffset + partind - 2]);//y
            }else{
                unformatted[headerOffset + partind + 2] = format[yOffset + i];//y
            }
            unformatted[headerOffset + partind + 3] = format[miscOffset + i];//rmm
        }
        return unformatted;
    }

    public byte[] exportAndCompress(){
        byte[] data = formatForCompression(serialiseCropped());
        return Utils.compress(data);
    }

    public static ModularConstructBuilder decompressAndParse(byte[] compress){
        byte[] data = unformatFromCompression(Utils.decompress(compress));
        return new ModularConstructBuilder(data);
    }

    //todo replace with cheaper function
    protected void findRoot(){
        for(int i = 0; i < parts.length; i++){
            if(parts[i] != 0 && PartData.Type(parts[i]).root){
                rootIndex = i;
                return;
            }
        }
    }

    public static Seq<ModularPart> rawDataToPart(LongSeq data){
        Seq<ModularPart> parts = new Seq<>(data.size);
        for(int i = 0;i < data.size;i++){
            parts.add(PartData.get(data.get(i)));
        }
        return parts;
    }

    public static void getStats(ModularConstruct stuff, ModularPartStatMap mstat){
        getStats(stuff.parts,stuff.partlist,mstat);
    }
    public static void getStats(ModularConstructBuilder builder, ModularPartStatMap mstat){
        ///temp
        var partseqraw = builder.getList(false);
        getStats(new ModularPart[builder.w][builder.h], rawDataToPart(partseqraw), mstat);
    }
    public static void getStats(ModularPart[][] partgrid,  Seq<ModularPart> partseq, ModularPartStatMap mstat){
        mstat.initStat(partgrid,partseq);
    }
    public enum PlaceProblem{
        //todo: move to bundles
        CANT_FIT("Module cannot fit"),
        BLOCKED("Module is blocked by a placed module"),
        ROOT_EXISTS("There is already a root"),
        NO_PROBLEM("All good");
        public String text;

        PlaceProblem(String text){
            this.text = text;
        }
    }
    public PlaceProblem canPlaceDebug(ModularPartType selected, int x, int y){
        if(!canFit(selected, x, y)){
            return PlaceProblem.CANT_FIT;
        }
        if(!isEmpty(x,y,selected.w,selected.h)){
            return PlaceProblem.BLOCKED;
        }
        if(selected.root && rootIndex != -1){
            return PlaceProblem.ROOT_EXISTS;
        }
        return PlaceProblem.NO_PROBLEM;
    }
    public boolean canPlace(ModularPartType selected, int x, int y){
        return canPlaceDebug(selected, x, y) == PlaceProblem.NO_PROBLEM;
    }

    public void fill(long value, int x,int y, int w, int h){
        int to;
        for(int j = 0;j<h;j++){
            to = index(x,y+j)+w;
            for(int i = to-w;i<to;i++){
                parts[i] = value;
            }
        }
    }

    public boolean isEmpty(int x,int y, int w, int h){
        int to;
        for(int j = 0;j<h;j++){
            to = index(x,y+j)+w;
            for(int i = to-w;i<to;i++){
                if(parts[i]!=0){
                    return false;
                }
            }
        }
        return true;
    }

    public boolean placePartDirect(ModularPartType selected, int x, int y){
        if(!canPlace(selected, x, y)){
            return false;
        }
        if(selected.root){
            rootIndex = index(x,y);
        }
        long part = PartData.create(selected.id,x,y);
        fill(part,x,y,selected.w,selected.h);
        updatedItemReq = false;
        return true;
    }

    public boolean placePartDirect(long data, int x, int y){
        return placePartDirect(PartData.Type(data),x,y);
    }


    public boolean placePart(ModularPartType selected, int x, int y){
        if(!placePartDirect(selected, x, y)){
            return false;
        }
        if(selected.root){
            rootIndex = index(x,y);
        }
        rebuildValid();
        onChange.run();
        updatedItemReq = false;
        return true;
    }

    public void deletePartAt(int x, int y){
        int ind = index(x,y);
        if(parts[ind] == 0){
            return;
        }
        int px = PartData.x(parts[ind]);
        int py = PartData.y(parts[ind]);
        if(index(px,py) == rootIndex){
            rootIndex = -1;
        }
        var type = PartData.Type(parts[ind]);
        fill(0,px,py,type.w,type.h);

        rebuildValid();
        onChange.run();
        updatedItemReq = false;
    }

    public ModularPartType partTypeAt(int x, int y){
        int ind = index(x,y);
        if(parts[ind] == 0){
            return null;
        }
        return PartData.Type(parts[ind]);
    }

    ///todo: have the top decal be relatively light, the panel decal be made of a collage of different things pf various sizes seeded by the 1st 4 bits of the array.

    public static int getID(byte[] data, int s){
        if(idSize==1){
            return ub(data[s]);
        }
        int id = 0;
        for(int i = 0;i<idSize;i++){
            id += ub(data[s+i])<<(i*8); //ignore warning, its for if there is somehow more then 255 parts
        }
        return id;
    }
    public static void writeID(byte[] data, int s, int id){
        if(idSize==1){
            data[s]=b(id);
            return;
        }
        for(int i = 0;i<idSize;i++){
            data[s+i]= (byte)(id & (0xFF<<(i*8))); //ignore warning, its for if there is somehow more then 255 parts
        }
    }

    public static void writeShort(byte[] data, int index, int value){
        data[index] = (byte)((value>>8)&0xFF);
        data[index+1] = (byte)((value)&0xFF);
    }
    public static int readShort(byte[] data, int index){
        return (ub(data[index])<<8) + ub(data[index+1]);
    }


    public static byte b(int b){
        return (byte)b;
    }

    public static int ub(byte b){
        return ((int)b)&0xFF;
    }

    public ModularConstruct createConstruct(boolean compress){
        LongSeq part  = new LongSeq();
        var crop = findCropped(part);
        long data;
        for(int i = 0;i<part.size;i++){
            data = part.items[i];
            data = PartData.x(data,PartData.x(data)-crop[0]);
            data = PartData.y(data,PartData.y(data)-crop[1]);
            part.items[i] = data;
        }
        var cons =  ModularConstruct.get(crop[2]-crop[0]+1,crop[3]-crop[1]+1,part.toArray());
        if(compress && cons.compressedData==null){
            cons.compressedData = exportAndCompress();
        }
        return cons;
        //do other stuff to it if necessary
    }

    public static final class PartData{
        public static final long bitMaskID =        0xFFFF000000000000L;
        public static final long bitMaskX =         0x0000FF0000000000L;
        public static final long bitMaskY =         0x000000FF00000000L;
        public static final long bitMaskRotation =  0b11000000L << 24;
        public static final long bitMaskMode =      0b00110000L << 24;
        public static final long bitMaskMisc =      0b00001111L << 24;

        public static ModularPartType Type(long g){ return ModularPartType.getPartFromId(Id(g));}

        public static int Id(long g){ return (int)((bitMaskID&g)>>>48) - 1;}
        public static long  Id(long g,int id){ return (g&~bitMaskID) | (((long)id+1)<<48);}

        public static int x(long g){ return (int)((bitMaskX&g)>>>40);}
        public static long  x(long g,int x){ return (g&~bitMaskX) | ((long)x<<40);}

        public static int y(long g){ return (int)((bitMaskY&g)>>>32);}
        public static long y(long g,int y){ return (g&~bitMaskY) | ((long)y<<32);}

        //used in saving
        public static int other(long g){ return (int)(((bitMaskRotation|bitMaskMode|bitMaskMisc)&g)>>>24);}

        public static int rotation(long g){ return (int)((bitMaskRotation&g)>>>30);}
        public static long rotation(long g,int r){ return (g&~bitMaskRotation) | ((long)(r&0b11)<<30);}

        public static int mode(long g){ return (int)((bitMaskMode&g)>>>28);}
        public static long mode(long g,int mode){ return (g&~bitMaskMode) | ((long)(mode&0b11)<<28);}

        public static int misc(long g){ return (int)((bitMaskMisc&g)>>>24);}
        public static long misc(long g,int mode){ return (g&~bitMaskMisc) | ((long)mode<<24);}

        public static long create(ModularPartType id, int x, int y){
            return create(id.id,x,y);
        }
        public static long create(int id, int x, int y){
            return y(x(Id(0,id),x),y);
        }

        public static long create(int id, int x, int y,int o){
            return y(x(Id(0,id),x),y) + ((long)o << 24);
        }


        public static ModularPart get(long data){
            ModularPart p = new ModularPart(Type(data),x(data),y(data));
            p.rotation = rotation(data);
            return p;
        }
    }

}
