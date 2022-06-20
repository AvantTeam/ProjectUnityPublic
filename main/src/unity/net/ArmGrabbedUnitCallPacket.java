package unity.net;

import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.world.blocks.payloads.*;
import unity.world.blocks.payloads.PayloadArm.*;

public class ArmGrabbedUnitCallPacket extends Packet{
    private byte[] DATA;
    public UnitPayload unitPayload;
    public Building arm;
    Unit unit;

    public ArmGrabbedUnitCallPacket() {
        this.DATA = NODATA;
    }

    public void write(Writes WRITE) {
        TypeIO.writePayload(WRITE, this.unitPayload);
        TypeIO.writeUnit(WRITE,this.unitPayload.unit);
        mindustry.io.TypeIO.writeBuilding(WRITE, arm);
    }

    public void read(Reads READ, int LENGTH) {
        this.DATA = READ.b(LENGTH);
    }

    public void handled() {
        BAIS.setBytes(this.DATA);
        this.unitPayload = (UnitPayload)TypeIO.readPayload(READ);
        this.unit = TypeIO.readUnit(READ);
        this.arm = mindustry.io.TypeIO.readBuilding(READ);
    }

    public void handleClient() {
        if(arm instanceof PayloadArmBuild pab){
            unit.remove();
            if(Vars.net.client()){
                Vars.netClient.clearRemovedEntity(unit.id);
            }
            pab.grabUnit(unitPayload);
        }
    }

}
