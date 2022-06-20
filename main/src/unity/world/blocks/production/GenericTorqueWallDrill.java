package unity.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.world.blocks.*;
import unity.world.graph.*;

import static mindustry.Vars.*;

public class GenericTorqueWallDrill extends GenericGraphBlock{
    public float drillTime = 200f;
    public int range = 3;
    public int tier = 3;
    public Effect updateEffect = Fx.mineSmall;

    public GenericTorqueWallDrill(String name){
        super(name);
        hasItems = true;
        rotate = true;
        update = true;
        solid = true;
        drawArrow = false;
    }

    @Override
    public void init(){
        clipSize = Math.max(clipSize, size * tilesize + (range + 1) * tilesize);
        super.init();
    }
    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    ////AAAAAAAAAaaaaaaaaaa
    public class GenericTorqueWallDrillBuild extends GenericGraphBuild{
        public float warmup = 0;
        public float time = 0;
        public Item lastItem = null;
        public Point2[] checkPoints = new Point2[size];
        public OreDistance[] oredist = new OreDistance[size];

        float targetDrillAngle = 0,targetDrillExtend = 0;
        float drillAngle = 0,drillExtend = 0;
        int tilesDrilling = 0;

        @Override
        public float efficiency(){
            return super.efficiency() * Mathf.clamp(Mathf.map(getGraph(TorqueGraph.class).lastVelocity,0,torqueNode().maxSpeed,0,1.0f),0,1);
        }


        @Override
        public void updateTile(){
            super.updateTile();
            if(checkPoints[0] == null){
                for(int i = 0; i < size; i++){
                    if(checkPoints[i] == null) checkPoints[i] = new Point2();
                    getCheckPos(tileX(), tileY(), rotation, i, checkPoints[i]);
                    oredist[i] = new OreDistance(null,0);
                }
            }
            int mindist = range+1;
            tilesDrilling = 0;
            for(int p = 0; p < size; p++){
                Point2 l = checkPoints[p];
                Tile dest = null;
                for(int i = 0; i < range; i++){
                    int rx = l.x + Geometry.d4x(rotation) * i, ry = l.y + Geometry.d4y(rotation) * i;
                    Tile other = world.tile(rx, ry);
                    if(other != null && other.solid()){
                        Item drop = other.wallDrop();
                        if(drop != null && drop.hardness <= tier){
                            oredist[p].tiledis = i;
                            mindist = Math.min(mindist,i);
                            oredist[p].ore = drop;
                            oredist[p].tile = dest = other;
                            tilesDrilling++;
                        }
                        break;
                    }
                }
                if(dest==null){
                    oredist[p].ore = null;
                }
            }
            //linear regression :p
            float n = 0, sy = 0, sx = 0, sxy = 0,sx2 = 0,tx =0;
            float s2 = size*0.5f;
            for(int p = 0; p < size; p++){
                if(oredist[p].ore!=null){
                    n++;
                    tx = p-s2+0.5f;
                    sx += tx;
                    sx2 += tx*tx;
                    sxy += tx*oredist[p].tiledis;
                    sy+= oredist[p].tiledis;
                }
            }
            if(n>1){
                float a = (sy * sx2 - sx * sxy) / (n * sx2 - sx * sx);
                float b = (n * sxy - sx * sy) / (n * sx2 - sx * sx);
                targetDrillAngle = Mathf.atan2(1, -b);
                targetDrillExtend = a;
            }else{
                targetDrillAngle = 0;
                targetDrillExtend = sy;
            }
            targetDrillAngle = Mathf.radiansToDegrees * targetDrillAngle;
            if(rotation==1 || rotation==2){
                targetDrillAngle = -targetDrillAngle;
            }

            drillAngle += (targetDrillAngle - drillAngle)*efficiency()*0.05;
            drillExtend += (targetDrillExtend - drillExtend)*efficiency()*0.1;


            time += edelta();
            if(time >= drillTime){
                for(int p = 0; p < size; p++){
                    if(items.total() >= itemCapacity){
                        break;
                    }
                    if(oredist[p].ore!=null ){
                        items.add(oredist[p].ore, 1);
                        lastItem = oredist[p].ore;
                    }
                }
                time %= drillTime;
            }
            if(timer(timerDump, dumpTime)){
                dump();
            }
        }

        public BlockStatus status() {
            if(items.total()>=itemCapacity){
                return BlockStatus.noOutput;
            }
            return BlockStatus.active;
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            float s2 = size*0.5f;
            float rx = x + Geometry.d4x(rotation) * (s2+targetDrillExtend) * tilesize, ry = y + Geometry.d4y(rotation) * (s2+targetDrillExtend) * tilesize;
            Lines.line(x,y,rx,ry);
            Lines.lineAngle(rx,ry,targetDrillAngle+rotdeg()+90,s2 * tilesize);
            Lines.lineAngle(rx,ry,targetDrillAngle+rotdeg()+270,s2 * tilesize);

        }

        void getCheckPos(int tx, int ty, int rotation, int i, Point2 out){
            int cornerX = tx - (size - 1) / 2, cornerY = ty - (size - 1) / 2, s = size;
            switch(rotation){
                case 0 -> out.set(cornerX + s, cornerY + i);
                case 1 -> out.set(cornerX + i, cornerY + s);
                case 2 -> out.set(cornerX - 1, cornerY + i);
                case 3 -> out.set(cornerX + i, cornerY - 1);
            }
        }

        class OreDistance{
            Item ore;
            int tiledis;
            Tile tile;

            public OreDistance(Item ore, int tiledis){
                this.ore = ore;
                this.tiledis = tiledis;
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(time);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            time = read.f();
        }
    }
}
