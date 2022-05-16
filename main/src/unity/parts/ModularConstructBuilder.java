package unity.parts;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;

public class ModularConstructBuilder{
    public ModularPart [][] parts;
    public boolean[][] valid;
    public int w, h;
    public ModularPart root = null;

    public Runnable onChange = ()->{};

    public ModularConstructBuilder(int w, int h){
        this.w = w;
        this.h = h;
        parts = new ModularPart[w][h];
        valid = new boolean[w][h];
    }
    public void clear(){
        for(int i =0 ;i<w;i++){
            for(int j =0 ;j<h;j++){
                parts[i][j] = null;
            }
        }
        root=null;
        onChange.run();
        updatedItemReq = false;
    }

    public void set(byte[] data){
        ModularConstruct design = new ModularConstruct(data);
        parts = design.parts;
        if(parts==null){
            parts = new ModularPart[w][h];
            return;
        }
        w = parts.length;
        h = parts[0].length;
        valid = new boolean[w][h];
        root = null;
        findRoot();
        onChange.run();
        rebuildValid();
        updatedItemReq = false;
    }
    public void paste(ModularConstructBuilder e){
        int ox = (w-e.w)/2;
        int oy = (h-e.h)/2;
        for(int i =0 ;i<w;i++){
            for(int j =0 ;j<h;j++){
                if(!e.isIn(i-ox,j-oy)){
                    continue;
                }
                if(e.parts[i-ox][j-oy]==null){
                    continue;
                }
                var epart = e.parts[i-ox][j-oy];
                if(canFit(epart,ox,oy) && epart.isHere(i-ox,j-oy)){
                    placePartDirect(epart.type,i,j);
                }
            }
        }
        findRoot();
        rebuildValid();
    }

    public boolean isIn(int x,int y){
        return !(x<0 || y<0|| x>=w || y>=h);
    }
    public boolean canFit(ModularPart p, int ox,int oy){
        return isIn(p.x+ox,p.y+oy) && isIn(p.x+p.type.w-1+ox,p.y+p.type.h-1+oy);
    }
    public boolean canFit(ModularPartType p, int ox,int oy){
        return isIn(ox,oy) && isIn(p.w-1+ox,p.h-1+oy);
    }

    public Seq<ModularPart> getList(){
        OrderedSet<ModularPart> partsList = new OrderedSet<>();
        for(int i =0 ;i<w;i++){
            for(int j =0 ;j<h;j++){
                if(parts[i][j]!=null && !partsList.contains(parts[i][j])){
                    partsList.add(parts[i][j]);
                }
            }
        }
        return partsList.asArray();
    }

    public void rebuildValid(){
        for(int i =0 ;i<w;i++){
            for(int j =0 ;j<h;j++){
                valid[i][j] = false;
            }
        }
        if(root==null){
            return;
        }

        OrderedSet<Point2> front = new OrderedSet<>();
        front.add(new Point2(root.x,root.y));
        valid[root.x][root.y] = true;
        while(!front.isEmpty()){
            var pt = front.removeIndex(0);

            if(pt.x>0 && parts[pt.x-1][pt.y]!=null && !valid[pt.x-1][pt.y]){
                valid[pt.x-1][pt.y] = true;
                front.add(new Point2(pt.x-1,pt.y));
            }
            if(pt.y>0 && parts[pt.x][pt.y-1]!=null && !valid[pt.x][pt.y-1]){
                valid[pt.x][pt.y-1] = true;
                front.add(new Point2(pt.x,pt.y-1));
            }

            if(pt.x<w-1 && parts[pt.x+1][pt.y]!=null && !valid[pt.x+1][pt.y]){
                valid[pt.x+1][pt.y] = true;
                front.add(new Point2(pt.x+1,pt.y));
            }
            if(pt.y<h-1 && parts[pt.x][pt.y+1]!=null && !valid[pt.x][pt.y+1]){
                valid[pt.x][pt.y+1] = true;
                front.add(new Point2(pt.x,pt.y+1));
            }
        }
    }

    boolean updatedItemReq = false;
    public ItemSeq itemRequirements;

    public ItemSeq itemRequirements(){
        if(!updatedItemReq){
            itemRequirements = new ItemSeq();
            var list = getList();
            for(ModularPart mp : list){
                itemRequirements.add(mp.type.cost);
            }
            updatedItemReq = true;
        }
        return itemRequirements;
    }

    public byte[] export(){
        var partseq = getList();
        byte[] output = new byte[2+partseq.size*(ModularConstruct.idSize+2)];
        output[0] = ModularConstruct.sb(w);
        output[1] = ModularConstruct.sb(h);
        int blocksize = (ModularConstruct.idSize+2);
        for(int i = 0; i<partseq.size;i++){
            var part = partseq.get(i);
            ModularConstruct.writeID(output,2+blocksize*i,part.type.id);
            output[2+blocksize*i + ModularConstruct.idSize] = ModularConstruct.sb(part.x);
            output[2+blocksize*i + ModularConstruct.idSize+1] = ModularConstruct.sb(part.y);
        }
        return output;
    }

    //trims empty tiles.
    public byte[] exportCropped(){
        OrderedSet<ModularPart> partsList = new OrderedSet<>();
        int maxx = 0, minx = 256;
        int maxy = 0, miny = 256;

        for(int j = 0; j < h; j++){
            for(int i = 0; i < w; i++){
                if(valid[i][j] && parts[i][j] != null){
                    maxx = Math.max(i,maxx);
                    minx = Math.min(i,minx);
                    maxy = Math.max(j,maxy);
                    miny = Math.min(j,miny);
                }
            }
        }
        for(int i =minx ;i<=maxx;i++){
            for(int j =miny ;j<=maxy;j++){
                if(valid[i][j] && parts[i][j]!=null && !partsList.contains(parts[i][j])){
                    partsList.add(parts[i][j]);
                }
            }
        }
        byte[] output = new byte[2+partsList.size*(ModularConstruct.idSize+2)];
        output[0] = ModularConstruct.sb(maxx-minx+1);
        output[1] = ModularConstruct.sb(maxy-miny+1);
        var partseq = partsList.asArray();
        int blocksize = (ModularConstruct.idSize+2);
        for(int i = 0; i<partseq.size;i++){
            var part = partseq.get(i);
            ModularConstruct.writeID(output,2+blocksize*i,part.type.id);
            output[2+blocksize*i + ModularConstruct.idSize] = ModularConstruct.sb(part.x-minx);
            output[2+blocksize*i + ModularConstruct.idSize+1] = ModularConstruct.sb(part.y-miny);
        }
        return output;
    }

    private void findRoot(){
        for(int i =0 ;i<parts.length;i++){
            for(int j =0 ;j<parts[0].length;j++){
                if(parts[i][j]!=null && parts[i][j].type.root){
                    root = parts[i][j];
                }
            }
        }
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

    public boolean canPlace(ModularPartType selected, int x, int y){
        if(!canFit(selected,x,y)){
            return false;
        }
        for(int i =x ;i<x+selected.w;i++){
            for(int j =y ;j<y+selected.h;j++){
                if(parts[i][j]!=null){
                    return false;
                }
            }
        }
        if(selected.root && root!=null){
            return false;
        }
        return true;
    }
    public void placePartDirect(ModularPartType selected, int x, int y){
        if(!canPlace(selected,x,y)){
            return;
        }
        var part = selected.create(x,y);
        for(int i =x ;i<x+selected.w;i++){
           for(int j =y ;j<y+selected.h;j++){
               parts[i][j] = part;
           }
        }
        updatedItemReq = false;
    }
    public boolean placePart(ModularPartType selected, int x, int y){
        if(!canPlace(selected,x,y)){
            return false;
        }
        var part = selected.create(x,y);
        for(int i =x ;i<x+selected.w;i++){
           for(int j =y ;j<y+selected.h;j++){
               parts[i][j] = part;
           }
        }
        if(selected.root){
            root = part;
        }
        onChange.run();
        rebuildValid();
        updatedItemReq = false;
        return true;
    }

    public void deletePartAt(int x, int y){
        if(parts[x][y]!=null){
            if(parts[x][y]==root){
                root = null;
            }
            var part =parts[x][y];
            for(int i =part.x ;i<part.x+part.type.w;i++){
               for(int j =part.y ;j<part.y+part.type.h;j++){
                   parts[i][j] = null;
               }
            }
        }
        onChange.run();
        rebuildValid();
        updatedItemReq = false;
    }
}
