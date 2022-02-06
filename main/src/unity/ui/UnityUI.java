package unity.ui;

import arc.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.event.InputEvent.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import unity.gen.*;
import unity.mod.*;
import unity.util.*;

import static mindustry.Vars.iconMed;

//idk
public class UnityUI{
    public PartsEditorDialog partsEditor;
    public int one = 1;
    //blockfrag editings
    ObjectSet<Faction> included = new ObjectSet<>();
    boolean includeVanilla = true;

    public void init(){
        partsEditor = new PartsEditorDialog();

        Events.run(Trigger.update,()->{
            if(Vars.ui.hudfrag.blockfrag==null){
                return;
            }
            Table top = ReflectUtils.getFieldValue(Vars.ui.hudfrag.blockfrag,ReflectUtils.getField(Vars.ui.hudfrag.blockfrag,"topTable"));
            if(top!=null){
                ///needs to be triggered in this manner, otherwise hovered blocks will not display stats.
                var method = ReflectUtils.findMethod(Vars.ui.hudfrag.blockfrag.getClass(),"hasInfoBox",true);
                top.visible(()->ReflectUtils.invokeMethod(Vars.ui.hudfrag.blockfrag,method)==Boolean.TRUE || 1==one);
                if(top.find("faction table")!=null){
                    return;
                }
                top.row();
                top.image().growX().padTop(5).padLeft(0).padRight(0).height(3).color(Pal.gray); //top divider
                top.row();
                var pane = top.add(new ScrollPane(
                    new Table((tbl)->{
                        tbl.left();
                        ///add the vanilla button, with sharded as the icon?
                        var vanillaCheck = tbl.button(new TextureRegionDrawable(Core.atlas.find("team-sharded")), Styles.selecti,()->{
                            includeVanilla = !includeVanilla;
                            reloadBlocks();
                        }).size(46f).get();
                        vanillaCheck.resizeImage(iconMed);
                        vanillaCheck.update(()->{
                            vanillaCheck.setChecked(includeVanilla);
                        });
                        ///add the faction buttons
                        for(Faction f:Faction.all){
                            var factionCheck =tbl.button(new TextureRegionDrawable(f.icon), Styles.selecti,()->{
                                if(included.contains(f)){
                                    included.remove(f);
                                }else{
                                    included.add(f);
                                }
                                reloadBlocks();
                            }).tooltip(f.localizedName).size(46f).get();
                            factionCheck.getStyle().imageDownColor = f.color;
                            factionCheck.resizeImage(iconMed);
                            factionCheck.update(()->{
                                factionCheck.setChecked(included.contains(f));
                            });
                        }
                    })
                )).left().growX().get();
                pane.name = "faction table";
                pane.setScrollingDisabledY(true);
                ///needs to be done so click doesn't remove scroll focus from the game zoom
                pane.update(()->{
                    if(pane.hasScroll()){
                        Element result = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                        if(result == null || !result.isDescendantOf(pane)){
                            Core.scene.setScrollFocus(null);
                        }
                    }
                });
            }
        });
    }

    public void reloadBlocks(){
        Table table = ReflectUtils.getFieldValue(Vars.ui.hudfrag.blockfrag,ReflectUtils.getField(Vars.ui.hudfrag.blockfrag,"toggler"));
        for(Block b:Vars.content.blocks()){
            if(FactionMeta.map(b)==null){
                b.placeablePlayer = includeVanilla;
            }else{
                b.placeablePlayer = included.contains(FactionMeta.map(b));
            }
        }
        //fuckery (updates the blocks)
        var b = (ImageButton)table.find("category-"+ Vars.ui.hudfrag.blockfrag.currentCategory.name());
        b.fireClick();

    }

}
