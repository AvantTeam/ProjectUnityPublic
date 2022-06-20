package unity.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import unity.graphics.*;
import unity.world.graph.*;
import unity.world.meta.*;

public class CrucibleMeltStatElement extends Element{
    Item item;

    public CrucibleMeltStatElement(Item item){
        this.item = item;
    }

    @Override
    public void draw(){
        var font = Fonts.def;
        var lay = Pools.obtain(GlyphLayout.class,(()->{return new GlyphLayout();}));
        var melt = CrucibleRecipes.items.get(item);
        Drawable top = Tex.barTop;
        Draw.color(item.color);
        top.draw(x,y,Math.max(width*0.1f,32),height);

        Draw.color(Pal.darkerGray);
        Draw.rect(item.fullIcon,x+16,y + 14,24,24);
        Draw.color();
        Draw.rect(item.fullIcon,x+16,y + 18,24,24);

        text(Core.bundle.format("stat.unity-itemmeltpoint",melt.meltingpoint-HeatGraphNode.celsiusZero),x+50,y+16,lay,font,Align.left);
        float xpos = x+lay.width+50+8;
        Color col = new Color();
        HeatGraphNode.heatColor(melt.meltingpoint,col);
        Draw.color(col);
        top.draw(xpos,y,32,height);
        Draw.color();
        xpos+=32;



        Pools.free(lay);
    }

    public void text(String text, float x, float y, GlyphLayout lay,Font font,int align ){
        lay.setText(font, text);
        font.setColor(Color.white);
        if(align == Align.center){
            font.draw(text, x - lay.width / 2.0f, y + lay.height / 2.0f);
        }else{
            font.draw(text, x, y + lay.height / 2.0f);
        }

    }

    @Override
    public float getMinHeight(){
       return 32;
    }
}
