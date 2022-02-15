package unity.world.blocks.units;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.logic.LogicBlock.*;
import mindustry.world.blocks.payloads.*;
import unity.*;
import unity.content.*;
import unity.parts.*;
import unity.type.*;
import unity.ui.*;

public class ModularUnitAssembler extends PayloadBlock{
    public int unitModuleWidth = 255;
    public int unitModuleHeight = 255;
    public boolean sandbox = false;
    public ModularUnitAssembler(String name){
        super(name);
        solid = false;
        configurable = true;
        config(byte[].class, (ModularUnitAssemblerBuild build, byte[] data) -> build.blueprint.set(data));
        config(Boolean.class, (ModularUnitAssemblerBuild build, Boolean data) -> {if(data){build.spawnUnit();}});
    }

    public class ModularUnitAssemblerBuild extends PayloadBlockBuild<UnitPayload>{
        public ModularConstructBuilder blueprint;

        public ModularUnitAssemblerBuild(){
            blueprint = new ModularConstructBuilder(unitModuleWidth,unitModuleHeight);
        }

        @Override
        public void buildConfiguration(Table table){
            //ui lamdba soup time
            var configureButtonCell = table.button(Tex.whiteui, Styles.clearTransi, 50,
            (() -> {
                Unity.ui.partsEditor.show(blueprint.export(), this::configure, PartsEditorDialog.unitInfoViewer,part->part.visible);
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
           Events.fire(new UnitCreateEvent(t, this));
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
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            byte[] data = new byte[read.i()];
            read.b(data);
            blueprint.set(data);
        }
    }
}
