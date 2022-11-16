package unity.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import unity.graphics.*;
import unity.world.blocks.*;
import unity.world.graph.*;

import static arc.Core.atlas;

public class SimpleTransmission extends GenericGraphBlock{
    final TextureRegion[] topRegions = new TextureRegion[2], overlayRegions = new TextureRegion[2], movingRegions = new TextureRegion[3];//topsprite,overlaysprite,moving
    TextureRegion bottomRegion, mbaseRegion;//base,mbase

    public SimpleTransmission(String name){
        super(name);
        rotate = solid = true;
    }

    @Override
    public void load(){
        super.load();
        for(int i = 0; i < 2; i++){
            topRegions[i] = atlas.find(name + "-top" + (i + 1));
            overlayRegions[i] = atlas.find(name + "-overlay" + (i + 1));
        }
        for(int i = 0; i < 3; i++) movingRegions[i] = atlas.find(name + "-moving" + (i + 1));
        bottomRegion = atlas.find(name + "-bottom");
        mbaseRegion = atlas.find(name + "-mbase");
    }

    public static class TransmissionTorqueGraphNode extends TorqueGraphNode{
        float ratio = 2;
        public TransmissionTorqueGraphNode(float friction, float inertia, float ratio,GraphBuild build){
            super(friction, inertia, build);
            this.ratio=ratio;
        }

        public TransmissionTorqueGraphNode(GraphBuild build){
            super(build);
        }

        @Override
        public void update(){
            if(connector.size!=2){return;}
            TorqueGraph t1 = connector.get(0).getGraph();
            TorqueGraph t2 = connector.get(1).getGraph();
            if(t1.authoritative || t2.authoritative){
                //in middle of save flood
                return;
            }
            if(t1==t2){
                var build = t1.randomVertex().getNode().build();
                build.damage(Mathf.random(t1.lastVelocity*50));
                this.build().damage(Mathf.random(t1.lastVelocity*20));
                t1.lastVelocity*=0.8;
                return;
            }
            float totalmomentum = t1.lastInertia*t1.lastVelocity +t2.lastInertia*t2.lastVelocity;
            float totalinertia = t1.lastInertia+ ratio*t2.lastInertia;
            float v1 = totalmomentum/totalinertia;
            t1.lastVelocity = v1;
            t2.lastVelocity = v1*ratio;


        }
    }

    public class SimpleTransmissionBuildGeneric extends GenericGraphBuild{
        //oh god :(
        @Override
        public void draw(){
            TorqueGraphNode torqueNode = (TorqueGraphNode)getGraphNode(TorqueGraph.class);
            float graphRot0 = torqueNode.connector.get(0).getGraph().rotation;
            float graphRot1 = torqueNode.connector.get(1).getGraph().rotation;

            int rectifiedRotation = rotation+1; // haha i dont understand my draw code either
            float fixedRot = (rectifiedRotation*90 + 90f) % 180f - 90f;
            int variant = (rectifiedRotation + 1) % 4 >= 2 ? 1 : 0;
            Draw.rect(bottomRegion, x, y);
            Draw.rect(mbaseRegion, x, y, rectifiedRotation*90);

            Point2 offset = Geometry.d4(rectifiedRotation + 1);
            float ox = offset.x * 4f;
            float oy = offset.y * 4f;
            //xelo..
            UnityDrawf.drawRotRect(movingRegions[0], x + ox, y + oy, 16f, 4.5f, 4.5f, fixedRot, graphRot0, graphRot0 + 180f);
            UnityDrawf.drawRotRect(movingRegions[0], x + ox, y + oy, 16f, 4.5f, 4.5f, fixedRot, graphRot0 + 180f, graphRot0 + 360f);

            UnityDrawf.drawRotRect(movingRegions[1], x + ox * -0.125f, y + oy * -0.125f, 16f, 4.5f, 4.5f, fixedRot, 360f - graphRot0, 180f - graphRot0);//360-(a+180)
            UnityDrawf.drawRotRect(movingRegions[1], x + ox * -0.125f, y + oy * -0.125f, 16f, 4.5f, 4.5f, fixedRot, 540f - graphRot0, 360f - graphRot0);//720-(a+180),720-(a+360)

            UnityDrawf.drawRotRect(movingRegions[2], x - ox, y - oy, 16f, 2.5f, 2.5f, fixedRot, graphRot1, graphRot1 + 180f);
            UnityDrawf.drawRotRect(movingRegions[2], x - ox, y - oy, 16f, 2.5f, 2.5f, fixedRot, graphRot1 + 180f, graphRot1 + 360f);

            Draw.rect(overlayRegions[variant], x, y, rectifiedRotation*90);

            Draw.rect(topRegions[rectifiedRotation % 2], x, y);
            drawTeamTop();
        }
    }
}
