package unity.net;

import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.net.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import unity.world.blocks.payloads.PayloadArm.*;
import unity.world.blocks.payloads.PayloadArm.PayloadArmBuild.*;

public class ArmDroppedBlockCallPacket extends Packet{
    private byte[] DATA;
    public BuildPayload buildPayload;
    public Building arm;
    public Tile tile;

    public ArmDroppedBlockCallPacket() {
        this.DATA = NODATA;
    }

    public void write(Writes WRITE) {
        TypeIO.writePayload(WRITE, this.buildPayload);
        TypeIO.writeBuilding(WRITE, arm);
        TypeIO.writeTile(WRITE,tile);
    }

    public void read(Reads READ, int LENGTH) {
        this.DATA = READ.b(LENGTH);
    }

    public void handled() {
        BAIS.setBytes(this.DATA);
        this.buildPayload = (BuildPayload)TypeIO.readPayload(READ);
        this.arm = TypeIO.readBuilding(READ);
        this.tile = TypeIO.readTile(READ);
    }

    public void handleClient() {
        if(arm instanceof PayloadArmBuild pab){
            if(Build.validPlace(buildPayload.block(), buildPayload.build.team, tile.x, tile.y, buildPayload.build.rotation, false)){ // place on the ground
                buildPayload.place(tile, buildPayload.build.rotation);
                Fx.placeBlock.at(tile.drawx(), tile.drawy(), buildPayload.block().size);
                pab.dropBlock();
            }
        }
    }
}
