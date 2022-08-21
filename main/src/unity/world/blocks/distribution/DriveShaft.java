package unity.world.blocks.distribution;

import arc.graphics.g2d.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;
import unity.world.graph.GraphConnector.*;

import static arc.Core.atlas;

public class DriveShaft extends GenericGraphBlock{
    final TextureRegion[] baseRegions = new TextureRegion[4];
    TextureRegion topRegion, overlayRegion, movingRegion;//topsprite,overlaysprite,moving

    public DriveShaft(String name){
        super(name);
        rotate = true;
        update = true;
        solid = true;
        drawArrow = false;
    }

    @Override
    public void load(){
        super.load();
        topRegion = atlas.find(name + "-top");
        overlayRegion = atlas.find(name + "-overlay");
        movingRegion = atlas.find(name + "-moving");
        for(int i = 0; i < 4; i++) baseRegions[i] = atlas.find(name + "-base" + (i + 1));
    }

    public class DriveShaftBuildGeneric extends GenericGraphBuild{
        public int baseSpriteIndex;
        FixedGraphConnector<TorqueGraph> torqueConn;


        public void onConnectionChanged(GraphConnector g){
            baseSpriteIndex = 0;
            for(int i = 0;i<2;i++) {
                if(nearby((i*2+rotation)%4) instanceof GraphBuild gbuild){
                    if(!g.isConnected(gbuild)){continue;}
                    if(rotation == 1 || rotation == 2) baseSpriteIndex += i==0 ? 2 : 1;
                    else baseSpriteIndex += i==0 ? 1 : 2;
                }
            };
        }

        public FixedGraphConnector<TorqueGraph> getTorqueConn(){
            if(torqueConn ==null){
                torqueConn = (FixedGraphConnector)getGraphNode(TorqueGraph.class).connector.first();
            }
            return torqueConn;
        }

        @Override
        public void draw(){
            float graphRot = getTorqueConn().getGraph().rotation;
            float fixedRot = (rotdeg() + 90f) % 180f - 90f;
            Draw.rect(baseRegions[baseSpriteIndex], x, y, fixedRot);
            UnityDrawf.drawRotRect(movingRegion, x, y, 8f, 3.5f, 6f, fixedRot, graphRot, graphRot + 90f);
            Draw.rect(overlayRegion, x, y, fixedRot);
            Draw.rect(topRegion, x, y, fixedRot);
        }
    }
}
