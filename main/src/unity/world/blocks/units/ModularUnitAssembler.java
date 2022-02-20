package unity.world.blocks.units;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.modules.*;
import unity.*;
import unity.content.*;
import unity.gen.*;
import unity.parts.*;
import unity.parts.PanelDoodadType.*;
import unity.parts.types.*;
import unity.type.*;
import unity.ui.*;
import unity.util.*;

import static mindustry.Vars.content;
import static unity.parts.ModularPartType.*;

public class ModularUnitAssembler extends PayloadBlock{
    public int unitModuleWidth = 255;
    public int unitModuleHeight = 255;
    public boolean sandbox = false;
    public NinePatch chasis;

    TextureRegion softShadowRegion;

    public ModularUnitAssembler(String name){
        super(name);
        solid = false;
        configurable = true;
        config(byte[].class, (ModularUnitAssemblerBuild build, byte[] data) -> build.blueprint.set(data));
        config(Boolean.class, (ModularUnitAssemblerBuild build, Boolean data) -> {if(data){build.spawnUnit();}});
        clipSize = Math.max(unitModuleWidth,unitModuleHeight)*partSize;
        outputsPayload = !sandbox;
    }

    @Override
    public void load(){
        super.load();
        chasis = new NinePatch(Core.atlas.find("unity-chasis-panel"),6,6,6,6);
        chasis.scale(0.25f,0.25f);
        softShadowRegion =  Core.atlas.find("circle-shadow");
    }

    public static class ModuleConstructing{
        short x,y;
        ItemStack[] remaining;
        ModularPartType type;
        boolean modulePlaced = false;
        int takenby = -1;

        //moduleblock?
        ModuleConstructing(ModularPart mp){
            this.x = (short)mp.getX();
            this.y = (short)mp.getY();
            remaining = new ItemStack[mp.type.cost.length];
            for(int i = 0; i < remaining.length; i++){
                remaining[i] = new ItemStack(mp.type.cost[i].item,mp.type.cost[i].amount);
            }
            type = mp.type;
            modulePlaced = mp.type.moduleCost==null;
        }

        public void write(Writes write){
            write.s(x);
            write.s(y);
            write.s(type.id);
            write.s(remaining.length); //amount of items

            for(int i = 0; i < remaining.length; i++){
                write.s(remaining[i].item.id); //item ID
                write.i(remaining[i].amount); //item amount
            }
            write.bool(modulePlaced);
            write.i(takenby);
        }

        public ModuleConstructing(Reads read, byte revision){
            x = read.s();
            y = read.s();
            type = ModularPartType.getPartFromId(read.s());
            remaining = new ItemStack[read.s()];
            for(int i = 0; i < remaining.length; i++){
                remaining[i] = new ItemStack(content.item(read.s()),read.i());
            }
            modulePlaced = read.bool();
            takenby = read.i();
        }
        public int total(){
            int t = 0;
            for(int i = 0; i < remaining.length; i++){
                t+=remaining[i].amount;
            }
            return t;
        }
        public boolean submitItem(Item s){
            for(int i = 0; i < remaining.length; i++){
                if(s==remaining[i].item && remaining[i].amount>0){
                    remaining[i].amount--;
                    if(remaining[i].amount==0 && total()==0){
                        remaining = new ItemStack[0];
                    }
                    return true;
                }
            }
            return false;
        }
        public Item any(){
            for(int i = 0; i < remaining.length; i++){
                if(remaining[i].amount>0){
                    return remaining[i].item;
                }
            }
            return null;
        }

    }

    public class ModularUnitAssemblerBuild extends PayloadBlockBuild<UnitPayload>{
        public ModularConstructBuilder blueprint;
        public Seq<ModuleConstructing> currentlyConstructing;
        public Seq<PanelDoodad> doodads;
        IntSet built = new IntSet();
        public ModularConstruct construct;

        public ModularUnitAssemblerBuild(){
            blueprint = new ModularConstructBuilder(unitModuleWidth,unitModuleHeight);
            currentlyConstructing = new Seq<>();
            doodads  = new Seq<>();
        }

        @Override
        public void buildConfiguration(Table table){
            //ui lamdba soup time
            var configureButtonCell = table.button(Tex.whiteui, Styles.clearTransi, 50,
            (() -> {
                Unity.ui.partsEditor.show(blueprint.export(),
                (data)->{this.configure(data); doodads.clear();construct = new ModularConstruct(blueprint.exportCompressed());},
                PartsEditorDialog.unitInfoViewer,
                part->part.visible);
            }));
            configureButtonCell.size(50);
            configureButtonCell.get().getStyle().imageUp = Icon.pencil;

            if(sandbox){
                //creative spawn unit.
                var spawnUnitButtonCell = table.button(Tex.whiteui, Styles.clearTransi, 50,
                (() -> {
                    configure(true);
                })).size(50);
                spawnUnitButtonCell.get().getStyle().imageUp = Icon.add;
            }

            if(this.block.hasItems){
                Vars.control.input.frag.inv.showFor(this);
            }
        }

        //js
        public void spawnUnit(){
            if(Vars.net.client()){
                return;
            }
            var t = ((UnityUnitType)UnityUnitTypes.modularUnit).spawn(team, x, y,blueprint.exportCompressed());
            t.rotation = rotdeg();
            Events.fire(new UnitCreateEvent(t, this));
        }

        public void createUnit(){
            var t = UnityUnitTypes.modularUnit.create(team);
            ModularConstruct.cache.put(t,blueprint.exportCompressed());
            ((ModularUnitUnit) t).constructdata(ModularConstruct.cache.get(t));
            payload = new UnitPayload(t);
            payVector.setZero();
            Events.fire(new UnitCreateEvent(payload.unit, this));

        }

        //todo: TEMP, REMOVE WHEN CONSTRUCTORS IMPLEMENTED.
        public ModuleConstructing currentMod = null;

        @Override
        public void updateTile(){
            super.updateTile();
            if(blueprint.root!=null && !sandbox){
                if(doodads.isEmpty()){
                    UnitDoodadGenerator.initDoodads(blueprint.parts.length, doodads, construct);
                }
                if(construct==null){
                   construct = new ModularConstruct(blueprint.exportCompressed());
                }

                ////////////////====================== testing <
                if(currentMod==null){
                    currentMod = getJob(this,null);
                }
                if(currentMod!=null){
                    if(timer(timerDump, dumpTime)){
                        constructModule(currentMod,currentMod.any());
                    }
                    if(currentMod.remaining.length==0){
                        currentMod = null;
                    }
                }
                ////////////////====================== testing >

                if(built.size>=construct.partlist.size && payload==null){
                    createUnit();
                    built.clear();
                    currentlyConstructing.clear();
                }
            }
            moveOutPayload();
        }
        //arm needs to get an avalible job, then move, then build it
        public ModuleConstructing getJob(Building b,Item e){
            if(construct==null){
                return null;
            }
            if(e==null){
                for(var c:currentlyConstructing){
                    if(c.takenby==b.pos()){
                        return c;
                    }
                    if(c.takenby==-1){
                        c.takenby = b.pos();
                        return c;
                    }
                }
                for(var part:construct.partlist){
                    if(!built.contains(Point2.pack(part.getX(),part.getY()))){
                        var con = new ModuleConstructing(part);
                        con.takenby = b.pos();
                        currentlyConstructing.add(con);
                        return con;
                    }
                }

            }
            return null; //no job for you
        }
        //YoungchaBlocks.monomialHangar.buildVisibility = BuildVisibility.shown

        public boolean constructModule(ModuleConstructing module, Item item){
            boolean b = module.submitItem(item);
            if(module.total()==0){
                currentlyConstructing.remove(module);
                built.add(Point2.pack(module.x,module.y));
            }
            return b;
        }

        @Override
        public void draw(){
            Draw.rect(region,x,y);
            if(!sandbox){
                Draw.rect(outRegion, x, y, rotdeg());
            }
            drawTeamTop();
            DrawTransform dt = new DrawTransform();
            dt.setTranslate(x,y);
            dt.setRotation(rotdeg());
            ModularWheelType.rollDistance = 0;
            if(construct!=null && !sandbox && payload==null){
                //9 slice?

                Draw.color(0, 0, 0, 0.4f);
                float rad = 1.6f;
                float size = (construct.parts.length + construct.parts[0].length) * 0.5f * ModularPartType.partSize;
                Draw.rect(softShadowRegion, this, size * rad * Draw.xscl, size * rad * Draw.yscl, rotdeg() - 90);
                Draw.color();
                for(var part:construct.partlist){
                    if(built.contains(Point2.pack(part.getX(),part.getY()))){
                        part.type.draw(dt,part);
                    }else{
                        float dx = x + (part.ax - 0.5f) * partSize;
                        float dy = y + (part.ay - 0.5f) * partSize;
                        chasis.draw(dx, dy, x-dx,y-dy, part.type.w * partSize, part.type.h * partSize,1,1,rotdeg()-90);
                    }
                }
            }
            Draw.z(Layer.blockOver);
            payRotation = rotdeg();
            drawPayload();
        }

        @Override
        public byte[] config(){
            return blueprint.export();
        }

        @Override
        public void configured(Unit builder, Object value){
            super.configured(builder, value);
        }


        @Override
        public void write(Writes write){
            super.write(write);
            var data  =blueprint.export();
            write.i(data.length);
            write.b(data);
            write.i( built.size);
            built.each(write::i);
            write.i(currentlyConstructing.size);
            for(int i = 0;i<currentlyConstructing.size;i++){
                currentlyConstructing.get(i).write(write);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            byte[] data = new byte[read.i()];
            read.b(data);
            blueprint.set(data);
            int builtlen = read.i();
            for(int i =0;i<builtlen;i++){
                built.add(read.i());
            }
            int clen = read.i();
            for(int i =0;i<clen;i++){
                currentlyConstructing.add(new ModuleConstructing(read,revision));
           }
        }
    }
}
