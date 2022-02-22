package unity.world.blocks.distribution;

import mindustry.world.blocks.distribution.*;

public class KoruhConveyor extends Conveyor{
    protected float realSpeed, drawMultiplier;

    public KoruhConveyor(String name){
        super(name);
        absorbLasers = true; //an internal flag telling expconveyors apart, also makes lasers less viable against koruh
    }

    @Override
    public void load(){
        super.load();
        realSpeed = speed;
    }

    public class KoruhConveyorBuild extends ConveyorBuild{
        @Override
        public void draw(){
            speed = realSpeed * drawMultiplier;
            super.draw();
            speed = realSpeed;
        }
    }
}
