package unity.parts;

import arc.struct.*;

public class ModularConstructBuilder{
    public ModularPart [][] parts;
    public int w, h;

    public ModularConstructBuilder(int w, int h){
        this.w = w;
        this.h = h;
        parts = new ModularPart[w][h];
    }


    public void set(byte[] data){
        ModularConstruct design = new ModularConstruct(data);
        parts = design.parts;
        w = parts.length;
        h = parts[0].length;
    }

    public byte[] export(){
        OrderedSet<ModularPart> partsList = new OrderedSet<>();
        for(int i =0 ;i<w;i++){
            for(int j =0 ;j<h;j++){
                if(parts[i][j]!=null && !partsList.contains(parts[i][j])){
                    partsList.add(parts[i][j]);
                }
            }
        }
        byte[] output = new byte[2+partsList.size*(ModularConstruct.idSize+2)];
        output[0] = ModularConstruct.sb(w);
        output[1] = ModularConstruct.sb(h);
        var partseq = partsList.asArray();
        int blocksize = (ModularConstruct.idSize+2);
        for(int i = 0; i<partseq.size;i++){
            var part = partseq.get(i);
            ModularConstruct.writeID(output,2+blocksize*i,part.type.id);
            output[2+blocksize*i + 1] = ModularConstruct.sb(part.x);
            output[2+blocksize*i + 2] = ModularConstruct.sb(part.y);
        }
        return output;
    }

    public static void getStats(ModularPart [][] parts, ModularPartStatMap mstat){
        //need to find the root;
        ModularPart root = null;
        OrderedSet<ModularPart> partsList = new OrderedSet<>();
        for(int i =0 ;i<parts.length;i++){
            for(int j =0 ;j<parts[0].length;j++){
                if(parts[i][j]!=null && !partsList.contains(parts[i][j])){
                    partsList.add(parts[i][j]);
                    if(parts[i][j].type.root){
                        root = parts[i][j];
                    }
                }
            }
        }
        if(root==null){
            return;
        }
        ///temp
        var partseq = partsList.asArray();
        for(int i = 0; i<partseq.size;i++){
            partseq.get(i).type.appendStats(mstat,partseq.get(i),parts);
        }
        for(int i = 0; i<partseq.size;i++){
            partseq.get(i).type.appendStatsPost(mstat,partseq.get(i),parts);
        }

    }
}
