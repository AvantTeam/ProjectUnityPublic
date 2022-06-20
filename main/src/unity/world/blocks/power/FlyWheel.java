package unity.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.graphics.*;
import unity.world.blocks.*;
import unity.world.blocks.power.SteamPiston.*;
import unity.world.graph.*;

public class FlyWheel extends GenericGraphBlock{
    TextureRegion base,shaft,wheel,top;
    public FlyWheel(String name){
        super(name);
    }

    @Override
    public void load(){
        super.load();
        base = Core.atlas.find(name+"-base");
        shaft = Core.atlas.find(name+"-shaft");
        wheel = Core.atlas.find(name+"-wheel");
        top = Core.atlas.find(name+"-top");
    }

    public class FlyWheelBuild extends GenericGraphBuild{
        public OrderedSet<SteamPistonBuild> connected = new OrderedSet<>();
        public float attachX,attachY;

        @Override
        public void created(){
            super.created();
            attachY = y;
            attachX = x;
        }

        public void update(){
            super.update();
            torqueNode().baseForce=0;
            for(SteamPistonBuild spb: connected){
                torqueNode().baseForce += spb.pushForce*0.5f;
            }
            float rot = getGraph(TorqueGraph.class).rotation*0.25f;
            attachX = Mathf.sinDeg(-rot)* Vars.tilesize * 0.5f + x;
            attachY = Mathf.cosDeg(rot)* Vars.tilesize * 0.5f + y;
        }
        @Override
        public void draw(){
            float graphRot = getGraph(TorqueGraph.class).rotation;
            Draw.rect(base,x,y,0);
            Draw.rect(shaft,x,y, get2SpriteRotation());
            Drawf.spinSprite(wheel, x, y, graphRot * 0.25f);
            Draw.rect(top,x,y,0);
            drawTeamTop();
        }
    }
}
