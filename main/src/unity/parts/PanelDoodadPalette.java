package unity.parts;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;

public class PanelDoodadPalette{
    public boolean center;
    public boolean sides;
    public int w,h;
    public Seq<PanelDoodadType> doodads = new Seq<>();
    int amount = 0;
    String name;

    public PanelDoodadPalette(boolean center, boolean sides, int w, int h,String name, int amount){
        this.center = center;
        this.sides = sides;
        this.w = w;
        this.h = h;
        this.amount=amount;
        this.name=name;
    }

    public void load(){
        Point2[] parray = new Point2[w*h];
        for(int i = 0;i<w*h;i++){
            parray[i] = new Point2(i%w,i/w);
        }
        for(int i = 0;i<amount;i++){
            var d = new PanelDoodadType(parray, Core.atlas.find("unity-doodad-"+name+"-"+(i+1)),Core.atlas.find("unity-doodad-"+name+"-outline-"+(i+1)),w,h);
            doodads.add(d);
        }
    }

    public PanelDoodadType get(float b){
        return doodads.get((int)Mathf.clamp(doodads.size*b,0,doodads.size-1));
    }

}
