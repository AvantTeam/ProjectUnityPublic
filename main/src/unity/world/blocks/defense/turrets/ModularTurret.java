package unity.world.blocks.defense.turrets;

import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import unity.world.graph.*;

public class ModularTurret extends Turret implements GraphBlock{
    public ModularTurret(String name){
        super(name);
    }

    @Override
    public Block getBuild(){
        return null;
    }

    @Override
    public GraphBlockConfig getConfig(){
        return null;
    }
    /*
    todo: implement uh the stats b4 this or work on units
    public class ModularTurretBuild extends TurretBuild implements GraphBuild{


    }*/
}
