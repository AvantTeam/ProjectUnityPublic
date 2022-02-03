package unity.ui;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import unity.parts.*;

import static mindustry.Vars.control;

public class PartsEditorDialog extends BaseDialog{
    public ModularConstructBuilder builder;
    Cons<byte[]> consumer;
    public PartsEditorElement editorElement;
    ModularPartType hoveredPart = null;
    //part select
    Cons<Table> partSelectBuilder = table -> {
        table.clearChildren();
        table.top();
        table.add(Core.bundle.get("ui.parts.select")).growX().left().color(Pal.gray);
        table.row();


        table.add("insert name").growX().left().color(Pal.accent);
        table.row();
        table.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
        table.row();
        table.table(list -> {
            for(var part: ModularPartType.partMap){
                ModularPartType mpt = part.value;
                list.left();
                ImageButton partbutton = list.button(new TextureRegionDrawable(mpt.icon),Styles.selecti,()->{
                    if(editorElement.selected == mpt){
                        editorElement.deselect();
                    }else{
                        editorElement.select(mpt);
                    }
                }).pad(3).size(46f).name("part-" + mpt.name).get();
                //on hover display stats in box below
                partbutton.hovered(()->{
                    hoveredPart = mpt;
                });
                // unselect if another one got selected
                partbutton.update(()->{
                    partbutton.setChecked(editorElement.selected == mpt);
                    //possibly set gray if disallowed.
                });
                //deselect hover
                partbutton.exited(()->{
                    if(hoveredPart == mpt){
                        hoveredPart = null;
                    }
                });
            }
        }).growX().left().padBottom(10);
    };
    //part select & info container
    Cons<Table> leftSideBuilder = table -> {
        table.clearChildren();
        //Middle content
        Table content = new Table();
        //top side, tabs
        Table tabs = new Table();
        ImageButton partsSelectMenuButton = new ImageButton(Icon.box, Styles.clearPartiali);
        partsSelectMenuButton.clicked(()->{
            partSelectBuilder.get(content);
        });
        tabs.add(partsSelectMenuButton).size(64).pad(8);

        ImageButton infoMenuButton = new ImageButton(Icon.info, Styles.clearPartiali);
        tabs.add(infoMenuButton).size(64);

        //middle
        ScrollPane scrollPane = new ScrollPane(content,Styles.defaultPane);
        partSelectBuilder.get(content);

        //bottom info
        Table stats = new Table();
        stats.update(()->{
            stats.clear();
            stats.top().left().margin(5);
            ModularPartType selected = null;
            if(editorElement.selected!=null){
                selected = editorElement.selected;
            }
            if(hoveredPart!=null){
                selected = hoveredPart;
            }
            if(selected!=null){
                selected.display(stats);
            }
        });

        table.align(Align.top);
        table.add(tabs).align(Align.left);
        table.row();
        table.add(scrollPane).
            align(Align.top).growX().left().padBottom(10).growY().minWidth(280).get()
            .setScrollingDisabled(true, false);
        table.row();
        table.add(stats);

    };
    Table selectSide;

    public PartsEditorDialog(){
        super("parts");
        this.builder= new ModularConstructBuilder(3,3);
        editorElement = new PartsEditorElement(this.builder);
        clearChildren();
        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide).name("back");

        ///
        selectSide = new Table();


        Table editorSide = new Table();
        editorSide.add(editorElement).grow().name("editor");

        editorSide.row();

        editorSide.add(buttons).growX().name("canvas");

        add(selectSide).align(Align.top).growY();
        add(editorSide);

        hidden(() -> consumer.get(builder.export()));
    }

    public void show(byte[] data, Cons<byte[]> modified){
        this.builder.set(data);
        leftSideBuilder.get(selectSide);
        editorElement.setBuilder(this.builder);
        this.consumer = result -> {
           if(!new String(result).equals(new String(builder.export()))){
               modified.get(result);
           }
        };
        show();
    }

}
