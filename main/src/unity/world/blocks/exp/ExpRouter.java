package unity.world.blocks.exp;

import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;
import unity.entities.*;

import static arc.math.geom.Geometry.d4x;
import static arc.math.geom.Geometry.d4y;

/** Items pass through, but exp is distributed */
public class ExpRouter extends Junction {
    public float reloadTime = 15f;

    public ExpRouter(String name){
        super(name);
        noUpdateDisabled = false;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.speed, "stat.unity.exppersec", 60f / reloadTime);
    }

    public class ExpRouterBuild extends JunctionBuild implements ExpHolder{
        public float reload = reloadTime;

        @Override
        public void updateTile(){
            super.updateTile();
            reload += edelta();
        }

        @Override
        public int getExp(){
            return 0;
        }

        @Override
        public int handleExp(int amount){
            return 0;
        }

        @Override
        public boolean acceptOrb(){
            return enabled && reload >= reloadTime;
        }

        @Override
        public boolean handleOrb(int orbExp){
            reload = 0;

            //rotation is the next direction you should try inputting to
            for(int i = 0; i < 4; i++){
                int dir = (rotation + i) % 4;
                if(tryOutput(dir, orbExp)){
                    rotation = (dir + 1) % 4;
                    return true;
                }
            }
            return false;
        }

        @Override
        public int handleTower(int amount, float angle){
            if(!enabled || ExpOrbs.orbs(amount) <= 0) return 0;
            int a = ExpOrbs.oneOrb(amount);

            int dir;
            if(Angles.near(angle % 90f, 45f, 10f)){
                //corner
                dir = (int) (angle / 90f);
                boolean yes = tryOutput((dir + rotation % 2) % 4, a);
                rotation = (rotation + 1) % 4;
                if(yes) return a;
                else if(tryOutput((dir + rotation % 2) % 4, a)) return a; //try the other corner
            }
            else{
                dir = (((int) angle + 45) / 90) % 4;
                if(tryOutput(dir, a)) return a;
            }
            return 0;
        }

        /** Try outputting exp to a tile
         * @return whether the exp was passed successfully
         */
        public boolean tryOutput(int dir, int orbExp){
            Tile t = tile.nearby(dir);
            if(t == null) return false;
            if(t.solid()){
                if(t.build instanceof ExpHolder exp && exp.acceptOrb() && exp.handleOrb(orbExp)) return true;
                else return t.block() instanceof Incinerator;
            }
            else{
                //check conveyor
                if(!(t.build instanceof Conveyor.ConveyorBuild conv) || conv.nearby(conv.rotation) != this){
                    ExpOrbs.dropExp(x + d4x(dir) * 7f, y + d4y(dir) * 7f, dir * 90f, 4f, orbExp);
                    return true;
                }
                return false;
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            reload = read.f();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(reload);
        }
    }
}
