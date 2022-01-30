package unity.world.blocks.power;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;

import static arc.Core.atlas;

public class WindTurbine extends GenericGraphBlock{


    public final TextureRegion[] overlayRegions = new TextureRegion[2], baseRegions = new TextureRegion[4], rotorRegions = new TextureRegion[2];
    public TextureRegion topRegion, movingRegion, bottomRegion, mbaseRegion;

    public WindTurbine(String name){
        super(name);
        rotate = true;
        update = true;
        solid = true;
    }

    @Override
    public void load(){
        super.load();

        topRegion = atlas.find(name + "-top");
        movingRegion = atlas.find(name + "-moving");
        bottomRegion = atlas.find(name + "-bottom");
        mbaseRegion = atlas.find(name + "-mbase");

        for(int i = 0; i < 4; i++) baseRegions[i] = atlas.find(name + "-base" + (i + 1));
        for(int i = 0; i < 2; i++){
            overlayRegions[i] = atlas.find(name + "-overlay" + (i + 1));
            rotorRegions[i] = atlas.find(name + "-rotor" + (i + 1));
        }
    }
    public static class WindTurbineTorqueGraphNode extends TorqueGraphNode{

        public WindTurbineTorqueGraphNode(float friction, float inertia, float maxTorque, float maxSpeed, GraphBuild build){
            super(friction, inertia, maxTorque, maxSpeed, build);
        }

        @Override
        public void update(){
            //todo weather
            float weather = Groups.weather.contains(w->w.weather== Weathers.sandstorm || w.weather== Weathers.sporestorm)?2:1;
            float x = Time.time * 0.001f;
            float mul = 0.2f * Math.max(
                0f,
                Mathf.sin(x) + 0.5f * Mathf.sin(2f * x + 50f) + 0.2f * Mathf.sin(7f * x + 90f) + 0.1f * Mathf.sin(23f * x + 10f) + 0.55f
            ) + 0.3f;
            baseForce = mul*weather;
        }
    }
    public class WindTurbineBuildGeneric extends GenericGraphBuild{
        GraphConnector<TorqueGraph> torqueConn;
        public GraphConnector<TorqueGraph> getTorqueConn(){
            if(torqueConn ==null){
                torqueConn = getGraphNode(TorqueGraph.class).connector.first();
            }
            return torqueConn;
        }
        @Override
        public void draw(){
            float shaftRotog = getTorqueConn().getGraph().rotation;
            int variant = (rotation + 1) % 4 >= 2 ? 1 : 0;
            float shaftRot = variant == 1 ? 360f - shaftRotog : shaftRotog;

            Draw.rect(bottomRegion, x, y);
            Draw.rect(baseRegions[rotation], x, y);
            Draw.rect(mbaseRegion, x, y, rotdeg());

            UnityDrawf.drawRotRect(movingRegion, x, y, 24f, 3.5f, 24f, rotdeg(), shaftRot, shaftRot + 180f);
            Draw.rect(overlayRegions[variant], x, y, rotdeg());

            Draw.rect(rotorRegions[1], x, y, shaftRotog * 0.4f);
            Draw.rect(rotorRegions[0], x, y, shaftRotog * 0.2f);

            Draw.rect(topRegion, x, y);
            drawTeamTop();
        }
    }
}
