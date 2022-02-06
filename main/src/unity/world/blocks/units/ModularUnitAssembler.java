package unity.world.blocks.units;

import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.*;
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
    public ModularUnitAssembler(String name){
        super(name);
        solid = false;
        configurable = true;
        config(byte[].class, (ModularUnitAssemblerBuild build, byte[] data) -> build.blueprint.set(data));
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
                Unity.ui.partsEditor.show(blueprint.export(), this::configure, PartsEditorDialog.unitInfoViewer);
            }));
            configureButtonCell.size(50);
            configureButtonCell.get().getStyle().imageUp = Icon.pencil;

            if(Vars.state.rules.infiniteResources){
                //creative spawn unit.
                var spawnUnitButtonCell = table.button(Tex.whiteui, Styles.clearTransi, 50,
                (() -> {
                    ((UnityUnitType)UnityUnitTypes.modularUnit).spawn(team, x, y,blueprint.exportCompressed());
                })).size(50);
                spawnUnitButtonCell.get().getStyle().imageUp = Icon.add;
            }

            if(this.block.hasItems){
                Vars.control.input.frag.inv.showFor(this);
            }
        }
        @Override
        public byte[] config(){
            return blueprint.export();
        }

        @Override
        public void configured(Unit builder, Object value){
            super.configured(builder, value);
        }
    }
}
