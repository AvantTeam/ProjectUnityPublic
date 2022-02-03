package unity.util;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;

//force textures into the atlas, can be used dynamically at any point, slow
//UNFINiSHED.
public class TempAtlasAdder{
    public float[][] minmax;
    Pixmap pixmap;
    Texture texture;
    TempAtlasAdder(Texture texture){
        this.texture=texture;
        minmax = new float[texture.width][2];
        for(int i  = 0;i<minmax.length;i++){
            minmax[i][0] = texture.height;
        }
        Seq<TextureRegion> regions = new Seq<>();
        for(var region:Core.atlas.getRegions()){
            if(region.texture==texture){
                regions.add(region);
            }
        }
        for(var region:regions){
            int x1 = region.getX();
            int x2 = Math.round(region.u2 * texture.width);
            int y1 = region.getY();
            int y2 = Math.round(region.v2 * texture.height);
            int mny = Math.min(y1,y2);
            int mxy = Math.max(y1,y2);
            for(int x = x1;x<=x2;x++){
                if(x<0 || x>=texture.width){continue;}
                minmax[x][0] = Math.min(minmax[x][0],mny);
                minmax[x][1] = Math.min(minmax[x][1],mxy);
            }
        }
        pixmap = texture.getTextureData().getPixmap();
    }
    public void addRegion(TextureRegion region){

    }
    private void drawRegion(TextureRegion replacement, int x, int y){
        texture.getTextureData().getPixmap().draw(
            replacement.texture.getTextureData().getPixmap(),
            x, y,
            Math.round(replacement.u*replacement.texture.width), Math.round(replacement.v*replacement.texture.height),
            replacement.width, replacement.height
        );
    }

    public void apply(){
        texture.load(texture.getTextureData());
    }
}
