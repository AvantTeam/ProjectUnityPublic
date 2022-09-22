package unity.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.meta.*;
import unity.parts.*;

import java.util.*;

import static mindustry.Vars.*;

public class PartsEditorDialog extends BaseDialog{
    public ModularConstructBuilder builder;
    Cons<byte[]> consumer;
    public PartsEditorElement editorElement;
    public Func2<ModularConstructBuilder, Table,ModularPartStatMap> infoViewer;

    public ObjectMap<String,Seq<ModularPartType>> avalibleParts = new ObjectMap<>();
    boolean info = false;

    //part select
    Cons<Table> partSelectBuilder = table -> {
        table.clearChildren();
        table.top();
        table.add(Core.bundle.get("ui.parts.select")).growX().left().color(Pal.gray);

        for(var category: avalibleParts){
            ///Title!
            table.row();
            table.add(Core.bundle.get("ui.parts.category."+category.key)).growX().left().color(Pal.accent);
            table.row();
            table.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
            table.row();
            ///Table of the parts in the category
            table.table(list -> {
                int i = 0;
                for(var part : category.value){
                    if(i!=0 && i%5==0){
                        list.row(); //row size i 5
                    }
                    list.left();
                    ImageButton partbutton = list.button(new TextureRegionDrawable(part.icon), Styles.selecti, () -> {
                        if(editorElement.selected == part){
                            editorElement.deselect();
                        }else{
                            editorElement.select(part);
                        }
                    }).pad(3).size(46f).name("part-" + part.name)
                    .tooltip(part::displayTooltip).get();
                    partbutton.resizeImage(iconMed);

                    // unselect if another one got selected
                    partbutton.update(() -> {
                        partbutton.setChecked(editorElement.selected == part);
                        //possibly set gray if disallowed.
                        partbutton.forEach(elem -> elem.setColor(Color.white));
                        if(builder.rootIndex==-1 && !part.root){
                            partbutton.forEach(elem -> elem.setColor(Color.darkGray));
                        }
                    });
                    i++;
                }
            }).growX().left().padBottom(10);
        }
    };
    //part select & info container
    Cons<Table> leftSideBuilder = table -> {
        table.clearChildren();
        //Middle content
        Table content = new Table();
        //top side, tabs
        Table tabs = new Table();
        ImageButton partsSelectMenuButton = new ImageButton(Icon.box, Styles.clearNonei);
        partsSelectMenuButton.clicked(()->{
            partSelectBuilder.get(content);
        });
        tabs.add(partsSelectMenuButton).size(64).pad(8);

        ImageButton cosmeticMenuButton = new ImageButton(Icon.pick, Styles.clearNonei);
        cosmeticMenuButton.clicked(()->{
            /*
            infoViewer.get(builder,content);
            builder.onChange = ()->{
                infoViewer.get(builder,content);
            };*/
        });
        tabs.add(cosmeticMenuButton).size(64);

        //middle
        ScrollPane scrollPane = new ScrollPane(content,Styles.defaultPane);
        partSelectBuilder.get(content);

        //bottom info
        Table stats = new Table();
        stats.update(()->{
            stats.clear();
            stats.top().left().margin(5);
            if(editorElement.selected!=null){
                editorElement.selected.display(stats);
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
    boolean openInfo = false;

    public PartsEditorDialog(){
        super("parts");
        this.builder= new ModularConstructBuilder(3,3);
        editorElement = new PartsEditorElement(this.builder);
        clearChildren();
        buttons.defaults().size(160f, 64f);
        buttons.button(Icon.flipX,Styles.clearTogglei,()->{
            editorElement.mirror = !editorElement.mirror;
        }).update(i->{i.setChecked(editorElement.mirror);}).tooltip("mirror").width(64);
        buttons.button(Icon.file,()->{
            builder.clear();
            editorElement.onAction();
        }).tooltip("clear").width(64);
        buttons.button(Icon.copy,()->{
            Core.app.setClipboardText(Base64.getEncoder().encodeToString(builder.exportAndCompress()));
        }).tooltip("copy").width(64);
        buttons.button(Icon.paste,()->{
            try{
                var data=  Base64.getDecoder().decode(Core.app.getClipboardText().trim().replaceAll("[\\t\\n\\r]+", ""));
                ModularConstructBuilder test = ModularConstructBuilder.decompressAndParse(data);
                builder.paste(test);
                editorElement.onAction();
            }catch(Exception e){
                Vars.ui.showOkText("Uh", "Your code is poopoo (perhaps it's outdated, copied wrong, or just submit a bug report on discord)", () -> {}); ///?????
            }
        }).tooltip("paste").width(64);
        buttons.button(Icon.undo, ()->{
            editorElement.undo();
        }).name("undo").width(64);
        buttons.button(Icon.redo, ()->{
            editorElement.redo();
        }).name("redo").width(64);
        buttons.button("@back", Icon.left, this::hide).name("back");

        ///
        selectSide = new Table();


        Table editorSide = new Table();
        Table editorGroup = new Table();
        editorGroup.add(editorElement).grow();
        Table infoContent = new Table();
        editorGroup.add(new Table( t -> {
            t.button(Icon.leftSmall, Styles.emptyTogglei,()->{
                if(openInfo){
                    infoContent.clear();
                    openInfo = false;
                }else{
                    editorElement.statmap = infoViewer.get(builder,infoContent);
                    builder.onChange = ()->{
                        editorElement.statmap = infoViewer.get(builder,infoContent);
                    };
                    openInfo = true;
                }
            }).growY();
            t.add(infoContent);
        }));



        editorSide.add(editorGroup).grow().name("editor");

        editorSide.row();

        editorSide.add(buttons).growX().name("canvas");

        add(selectSide).align(Align.top).growY();
        add(editorSide);

        hidden(() -> consumer.get(builder.exportFull()));

        var cell = editorSide.find("editor");



        //input
        update(()->{
            if(Core.scene != null && Core.scene.getKeyboardFocus() == this){
                if(Core.input.ctrl()){
                    if(Core.input.keyTap(KeyCode.z)){
                        editorElement.undo();
                    }else if(Core.input.keyTap(KeyCode.y)){
                        editorElement.redo();
                    }
                }
            }
        });
    }

    public void show(byte[] data, Cons<byte[]> modified,Func2<ModularConstructBuilder, Table,ModularPartStatMap> viewer, Boolf<ModularPartType> allowed){
        this.builder.paste(data);
        leftSideBuilder.get(selectSide);
        editorElement.setBuilder(this.builder);
        this.consumer = modified::get;
        this.infoViewer = viewer;
        show();

        //todo: temp
        avalibleParts.clear();
        for(var part: ModularPartType.partMap){
            if(!allowed.get(part.value)){
                continue;
            }
            if(!avalibleParts.containsKey(part.value.category)){
                avalibleParts.put(part.value.category, new Seq<>());
            }
            avalibleParts.get(part.value.category).add(part.value);
        }
    }


    public static Func2<ModularConstructBuilder, Table,ModularPartStatMap> unitInfoViewer = (construct,table)->{
        table.clearChildren();
        var statmap = new ModularUnitStatMap();
        var itemcost = construct.itemRequirements();
        ModularConstructBuilder.getStats(construct, statmap);
        table.top();
        table.add(Core.bundle.get("ui.parts.info")).growX().left().color(Pal.gray);

        /// cost
        table.row();
        table.add("[lightgray]" + Stat.buildCost.localized() + ":[] ").left().top();
        table.row();
        table.table(req -> {
            req.top().left();
            int t = 0;
            for(ItemStack stack : itemcost){
                if(t%5==0 && t!=0){
                    req.row();
                }
                req.add(new ItemDisplay(stack.item, stack.amount, false)).padRight(5);
                t++;

            }
        }).growX().left().margin(3);
        table.row();
        table.add("[lightgray]" + Stat.health.localized() + ":[accent] "+ statmap.health).left().top();
        table.row();
        float eff =  Mathf.clamp(statmap.power/statmap.powerUsage);
        String color = "[green]";
        if(eff<0.7){
            color = "[red]";
        }else if(eff<1){
            color = "[yellow]";
        }
        float mass = statmap.mass;

        table.add("[lightgray]" + Stat.powerUse.localized() + ": "+color+Strings.fixed(statmap.powerUsage,1) + "/"+ statmap.power).left().top();
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stat.efficiency") + ": "+color+Strings.fixed(Mathf.clamp(eff)*100,1)+"%").left().top();
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stat.weight") + ":[accent] "+ mass).left().top();


        float wcap = statmap.weightCapacity;
        float speed = eff *  Mathf.pow(Mathf.clamp(wcap/mass),3) * statmap.speed;
        table.row();
        table.add("[lightgray]" + Stat.speed.localized()  + ":[accent] "+ Core.bundle.format("ui.parts.stat.speed",Strings.fixed(speed * 60f / tilesize,1))).left().top();
        table.row();
        table.add("[lightgray]" +  Core.bundle.get("ui.parts.stat.steerspeed")  + ":[accent] "+ Core.bundle.format("ui.parts.stattype.steerspeed",Strings.fixed(statmap.turningspeed * 60f,1))).left().top();
        table.row();
        table.add("[lightgray]" + Core.bundle.get("ui.parts.stat.armour-points") + ":[accent] "+ statmap.armourPoints).left().top();
        table.row();
        table.add("[lightgray]" + Stat.armor.localized() + ":[accent] "+ Strings.fixed(statmap.armour,1)).left().top();

        table.row();
        int weaponslots = statmap.weaponSlots;
        int weaponslotsused = statmap.weaponslotuse;
        table.add("[lightgray]" +  Core.bundle.get("ui.parts.stat.weapon-slots") + ": "+(weaponslotsused>weaponslots?"[red]":"[green]")+ weaponslotsused+"/"+weaponslots).left().top().tooltip(Core.bundle.get("ui.parts.stat.weapon-slots-tooltip"));

        table.row();
        int abilityslots = statmap.abilityslots;
        int abilityslotsused = statmap.abilityslotuse;
        table.add("[lightgray]" +  Core.bundle.get("ui.parts.stat.ability-slots") + ": "+(abilityslotsused>abilityslots?"[red]":"[green]")+ abilityslotsused+"/"+abilityslots).left().top().tooltip(Core.bundle.get("ui.parts.stat.ability-slots-tooltip"));
        return statmap;
    };

    public void updateScrollFocus(){
        boolean[] done = {false};

        Core.app.post(() -> forEach(child -> {
            if(done[0]) return;

            if(child instanceof ScrollPane || child instanceof PartsEditorElement){
                Core.scene.setScrollFocus(child);
                done[0] = true;
            }
        }));
    }

}
