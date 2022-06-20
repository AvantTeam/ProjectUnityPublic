package unity.net;

import arc.math.geom.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.world.*;
import unity.world.blocks.units.*;
import unity.world.blocks.units.ModularUnitAssembler.*;
import unity.world.blocks.units.UnitAssemblerArm.*;

public class TankModuleBuiltCallPacket extends Packet{
    private byte[] DATA;
    public Building hangar;
    public Building arm;
    public Point2 completedModule;

    public TankModuleBuiltCallPacket() {
        this.DATA = NODATA;
    }

    public void write(Writes WRITE) {
        TypeIO.writeBuilding(WRITE, hangar);
        TypeIO.writeBuilding(WRITE, arm);
        TypeIO.writeInts(WRITE,new int[]{completedModule.x,completedModule.y});
    }

    public void read(Reads READ, int LENGTH) {
        this.DATA = READ.b(LENGTH);
    }

    public void handled() {
        BAIS.setBytes(this.DATA);
        this.hangar = TypeIO.readBuilding(READ);
        this.arm = TypeIO.readBuilding(READ);
        var pts = TypeIO.readInts(READ);
        this.completedModule = new Point2(pts[0],pts[1]);
    }

    public void handleClient() {
        if(arm instanceof UnitAssemblerArmBuild unitarm && hangar instanceof ModularUnitAssemblerBuild hangar){
            hangar.finishModule(completedModule);
            if(unitarm.currentJob!=null && unitarm.currentJob.x == completedModule.x && unitarm.currentJob.y == completedModule.y){
                unitarm.currentJob.takenby=-1;
                unitarm.currentJob = null;
            }
        }
    }
}
