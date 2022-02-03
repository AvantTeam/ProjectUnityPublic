package unity.world.blocks.units;

import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import unity.*;
import unity.content.*;
import unity.parts.*;
import unity.type.*;

public class ModularUnitAssembler extends PayloadBlock{
    public int unitModuleWidth = 3;
    public int unitModuleHeight = 3;
    public ModularUnitAssembler(String name){
        super(name);
    }

    public class ModularUnitAssemblerBuild extends PayloadBlockBuild<UnitPayload>{
        ModularConstructBuilder blueprint;

        @Override
        public Building create(Block block, Team team){
            ModularUnitAssemblerBuild build = (ModularUnitAssemblerBuild)super.create(block, team);
            build.blueprint = new ModularConstructBuilder(unitModuleWidth,unitModuleHeight);
            return build;
        }

        @Override
        public void buildConfiguration(Table table){
            //ui lamdba soup time
            var configureButtonCell = table.button(Tex.whiteui, Styles.clearTransi, 50,
            (() -> {
                Unity.ui.partsEditor.show(blueprint.export(), this::configure);
            }));
            configureButtonCell.size(50);
            configureButtonCell.get().getStyle().imageUp = Icon.pencil;

            if(Vars.state.rules.infiniteResources){
                //creative spawn unit.
                var spawnUnitButtonCell = table.button(Tex.whiteui, Styles.clearTransi, 50,
                (() -> {
                    ((UnityUnitType)UnityUnitTypes.modularUnit).spawn(team, x, y,blueprint.export());
                })).size(50);
                spawnUnitButtonCell.get().getStyle().imageUp = Icon.add;
            }

            if(this.block.hasItems){
                Vars.control.input.frag.inv.showFor(this);
            }
        }

        @Override
        public void configured(Unit builder, Object value){
            super.configured(builder, value);
        }
    }
}
