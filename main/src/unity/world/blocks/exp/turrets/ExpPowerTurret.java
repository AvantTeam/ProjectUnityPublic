package unity.world.blocks.exp.turrets;

import arc.struct.*;
import mindustry.entities.bullet.*;
import mindustry.logic.*;
import mindustry.world.meta.*;
import unity.world.blocks.exp.*;

public class ExpPowerTurret extends ExpTurret {
    public BulletType shootType;
    public float powerUse = 1f;

    public ExpPowerTurret(String name){
        super(name);
        hasPower = true;
        envEnabled |= Env.space;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.ammo, StatValues.ammo(ObjectMap.of(this, shootType)));
    }

    @Override
    public void init(){
        consumePower(powerUse);
        super.init();
    }

    public class ExpPowerTurretBuild extends ExpTurretBuild{

        @Override
        public void updateTile(){
            unit.ammo(power.status * unit.type().ammoCapacity);

            super.updateTile();
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case ammo -> power.status;
                case ammoCapacity -> 1;
                default -> super.sense(sensor);
            };
        }

        @Override
        public BulletType useAmmo(){
            //nothing used directly
            return shootType;
        }

        @Override
        public boolean hasAmmo(){
            //you can always rotate, but never shoot if there's no power
            return true;
        }

        @Override
        public BulletType peekAmmo(){
            return shootType;
        }
    }
}