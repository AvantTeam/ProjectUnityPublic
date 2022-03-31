package unity.net;

import arc.math.geom.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import unity.world.blocks.payloads.PayloadArm.*;
import unity.world.blocks.units.*;
import unity.world.blocks.units.ModularUnitAssembler.*;
import unity.world.blocks.units.UnitAssemblerArm.*;

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

    public static void moduleComplete(ModularUnitAssemblerBuild hangar, UnitAssemblerArmBuild arm, ModuleConstructing module){
        if (Vars.net.server()) {
            TankModuleBuiltCallPacket packet = new TankModuleBuiltCallPacket();
            packet.hangar = hangar;
            packet.arm = arm;
            packet.completedModule = new Point2(module.x,module.y);
            Vars.net.send(packet, true);
        }
        //TankModuleBuiltCallPacket
    }


    public static void registerPackets(){
        Net.registerPacket(ArmGrabbedUnitCallPacket::new);
        Net.registerPacket(ArmGrabbedBlockCallPacket::new);
        Net.registerPacket(ArmDroppedBlockCallPacket::new);
        Net.registerPacket(TankModuleBuiltCallPacket::new);
    }
}
