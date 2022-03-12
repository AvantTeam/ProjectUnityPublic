package unity.net;

import mindustry.*;
import mindustry.net.*;
import mindustry.world.blocks.payloads.*;
import unity.world.blocks.payloads.PayloadArm.*;

public class UnityCalls{
    public static void unitGrabbedByArm(UnitPayload unit, PayloadArmBuild pab) {
        if (Vars.net.server() || !Vars.net.active()) {
            unit.unit.remove();
        }
        if (Vars.net.server()) {
            ArmGrabbedUnitCallPacket packet = new ArmGrabbedUnitCallPacket();
            packet.unitPayload = unit;
            packet.arm = pab;
            Vars.net.send(packet, true);
        }
    }


    public static void registerPackets(){
        Net.registerPacket(ArmGrabbedUnitCallPacket::new);
    }
}
