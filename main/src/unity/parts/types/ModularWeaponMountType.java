package unity.parts.types;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.type.*;
import unity.parts.*;
import unity.parts.stat.*;
import unity.parts.stat.AdditiveStat.*;
import unity.ui.*;
import unity.util.*;

public class ModularWeaponMountType extends ModularPartType{
    public ModularWeaponMountType(String name){
        super(name);
    }
    Weapon weapon;
    public void weapon(int slots, Weapon weapon){
        stats.add(new WeaponSlotUseStat(slots));
        stats.add(new WeaponMountStat(weapon));
        this.weapon = weapon;
        drawsTop = true;
        drawsOverlay = true;
    }

    @Override
    public void load(){
        super.load();
        weapon.load();
    }

    @Override
    public void appendStats(ModularPartStatMap statmap, ModularPart part, ModularPart[][] grid){
        super.appendStats(statmap, part, grid);
    }

    @Override
    public void drawTop(DrawTransform transform, ModularPart part){
        super.drawTop(transform, part);
    }

    @Override
    public void drawEditor(PartsEditorElement editor, int x, int y, boolean valid){
        super.drawEditor(editor, x, y, valid);
    }

    @Override
    public void drawEditorTop(PartsEditorElement editor, int x, int y, boolean valid){


        PartParams p = new PartParams();
        p.x = (x+w*0.5f)*32;
        p.y = (y+h*0.5f)*32;
        p.rotation = 0;
        p.life = Time.time * (editor.statmap != null? ((ModularUnitStatMap)editor.statmap).rps :0);
        for(var part : weapon.parts){
            if(part.under)
                part.draw(p);
        }
        if(weapon instanceof EditorDrawable ed){
            ed.drawEditor(editor,x,y,w,h,valid);
        }else{
            editor.rect(weapon.region, (x + w * 0.5f) * 32, (y + h * 0.5f) * 32, 2);
        }
        for(var part : weapon.parts){
            if(!part.under)
                part.draw(p);
        }
        Lines.stroke(2 * editor.scl, new Color(1,1,1,0.7f));
    }

    @Override
    public void drawEditorOverlay(PartsEditorElement editor, int x, int y){
        var point = editor.gridToUi((x+w*0.5f)*32 ,(y+h*0.5f)*32);
        Lines.circle(point.x,point.y,weapon.range() * editor.scl * 8);
    }

    @Override
    public void drawEditorSelect(PartsEditorElement editor, int x, int y, boolean placed){
        super.drawEditorSelect(editor,x,y,placed);
        if(!placed){
            drawEditorTop(editor, x, y, true);
            drawEditorOverlay(editor,x,y);
        }
    }
}
