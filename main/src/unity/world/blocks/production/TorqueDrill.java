package unity.world.blocks.production;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import unity.annotations.Annotations.*;
import unity.gen.*;
import unity.gen.GraphDrill.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.blocks.defense.*;
import unity.world.blocks.exp.*;
import unity.world.graph.*;

import static arc.Core.atlas;

@Dupe(base = GenericGraphBlock.class, parent = Drill.class, name = "GraphDrill")
public class TorqueDrill extends GraphDrill{
    public float maxEfficiency = 3.0f;

    public final TextureRegion[] bottomRegions = new TextureRegion[2], topRegions = new TextureRegion[2], oreRegions = new TextureRegion[2];
    public TextureRegion rotorRegion, rotorRotateRegion, mbaseRegion, wormDrive, gearRegion, rotateRegion, overlayRegion;

    public TorqueDrill(String name){
        super(name);
        rotate = true;
        drawArrow = false;
    }
    @Override
    public TextureRegion[] icons(){
            return new TextureRegion[]{atlas.find(name)};
        }

    @Override
    public void load(){
        super.load();

        rotorRegion = atlas.find(name + "-rotor");
        rotorRotateRegion = atlas.find(name + "-rotor-rotate");
        mbaseRegion = atlas.find(name + "-mbase");

        gearRegion = atlas.find(name + "-gear");
        overlayRegion = atlas.find(name + "-overlay");
        rotateRegion = atlas.find(name + "-moving");
        wormDrive = atlas.find(name + "-rotate");

        for(int i = 0; i < 2; i++){
            bottomRegions[i] = atlas.find(name + "-bottom" + (i + 1));
            topRegions[i] = atlas.find(name + "-top" + (i + 1));
            oreRegions[i] = atlas.find(name + "-ore" + (i + 1));
        }
    }

    @Override public boolean outputsItems(){
        return true;
    }

    @Override public boolean rotatedOutput(int x, int y){
        return false;
    }


    public class TorqueDrillBuild extends GraphDrillBuild{

        @Override
        public void updateConsumption(){
            super.updateConsumption();
            efficiency *= Mathf.sqrt(Mathf.clamp(Mathf.map(getGraph(TorqueGraph.class).lastVelocity,0,torqueNode().maxSpeed,0,maxEfficiency),0,maxEfficiency));
        }

        @Override
        public void draw(){
            float rot = getGraph(TorqueGraph.class).rotation;
            float fixedRot = (rotdeg() + 90f) % 180f - 90f;

            int variant = rotation % 2;

            float deg = rotation == 0 || rotation == 3 ? rot : -rot;
            float shaftRot = rot * 0.75f;

            Point2 offset = Geometry.d4(rotation + 1);

            Draw.rect(bottomRegions[variant], x, y);

            //target region
            if(dominantItem != null && drawMineItem){
                Draw.color(dominantItem.color);
                Draw.rect(oreRegions[variant], x, y);
                Draw.color();
            }
            float bottomrotor = 360f - rot * 0.05f;
            //bottom rotor
            Draw.rect(rotorRegion, x, y, bottomrotor);

            UnityDrawf.drawRotRect(rotorRotateRegion, x, y, 24f, 3.5f, 3.5f, 90 + bottomrotor, shaftRot, shaftRot + 180f);
            UnityDrawf.drawRotRect(rotorRotateRegion, x, y, 24f, 3.5f, 3.5f, 90 + bottomrotor, shaftRot + 180f, shaftRot + 360f);

            //shaft
            Draw.rect(mbaseRegion, x, y, fixedRot);

            UnityDrawf.drawRotRect(wormDrive, x, y, 24f, 3.5f, 3.5f, fixedRot, rot, rot + 180f);
            UnityDrawf.drawRotRect(wormDrive, x, y, 24f, 3.5f, 3.5f, fixedRot, rot + 180f, rot + 360f);
            UnityDrawf.drawRotRect(rotateRegion, x, y, 24f, 3.5f, 3.5f, fixedRot, rot, rot + 180f);

            Draw.rect(overlayRegion, x, y, fixedRot);

            //gears
            Draw.rect(gearRegion, x + offset.x * 4f, y + offset.y * 4f, -deg/2);
            Draw.rect(gearRegion, x - offset.x * 4f, y - offset.y * 4f, deg/2);

            Draw.rect(topRegions[variant], x, y);
            drawTeamTop();
        }

    }
}
