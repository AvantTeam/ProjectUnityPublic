package unity.net;

import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.world.*;
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
    public static void blockGrabbedByArm(Tile tile, BuildPayload bp, PayloadArmBuild pab) {
        if (Vars.net.server() || !Vars.net.active()) {
            Tile.removeTile(tile);
        }
        if (Vars.net.server()) {
            ArmGrabbedBlockCallPacket packet = new ArmGrabbedBlockCallPacket();
            packet.buildPayload = bp;
            packet.arm = pab;
            packet.tile = tile;
            Vars.net.send(packet, true);
        }
    }
    public static void blockDroppedByArm(Tile tile, BuildPayload bp, PayloadArmBuild pab) {
        if (Vars.net.server() || !Vars.net.active()) {
            if(Build.validPlace(bp.block(), bp.build.team, tile.x, tile.y, bp.build.rotation, false)){ // place on the ground
                bp.place(tile, bp.build.rotation);
                Fx.placeBlock.at(tile.drawx(), tile.drawy(), bp.block().size);
            }
        }
        if (Vars.net.server()) {
            ArmDroppedBlockCallPacket packet = new ArmDroppedBlockCallPacket();
            packet.buildPayload = bp;
            packet.arm = pab;
            packet.tile = tile;
            Vars.net.send(packet, true);
        }
    }


    public static void registerPackets(){
        Net.registerPacket(ArmGrabbedUnitCallPacket::new);
        Net.registerPacket(ArmGrabbedBlockCallPacket::new);
        Net.registerPacket(ArmDroppedBlockCallPacket::new);
    }
}
