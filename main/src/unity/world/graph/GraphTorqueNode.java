package unity.world.graph;

import unity.world.blocks.*;

public class GraphTorqueNode extends GraphNode<TorqueGraph>{
    public float baseFriction, baseInertia, baseForce;

    public GraphTorqueNode(float friction, float inertia, GraphBlock.IGraphBuild build){
        super(build);
        baseFriction = friction;
        baseInertia = inertia;
    }

    public GraphTorqueNode(GraphBlock.IGraphBuild build){
        this(0.1f, 10f,build);
    }


    /*
    table.row().left();
    table.add("Torque system").color(Pal.accent).fillX();
    table.row().left();
    table.add("[lightgray]" + bundle.get("stat.unity.friction") + ":[] ").left();
    table.row().left();
    table.add("[lightgray]" + bundle.get("stat.unity.inertia") + ":[] ").left();
    table.add(baseInertia + "t m^2");
    setStatsExt(table);*/

    /*
    void drawPlace(int x, int y, int size, int rotation, boolean valid){
        for(int i = 0; i < connector.length; i++){
            for(int j = 0; j < connector[i].connectionPoints.length; j++){
                var cp = connector[i].connectionPoints[j];
                Lines.stroke(3.5f, Color.white);
                GraphData outPos = GraphData.getConnectSidePos(i, size, rotation);
                int dx = (outPos.toPos.x + x) * tilesize;
                int dy = (outPos.toPos.y + y) * tilesize;
                Point2 dir = Geometry.d4(outPos.dir);
                Lines.line(dx - dir.x, dy - dir.y, dx - dir.x * 2, dy - dir.y * 2);
            }
        }
    }*/
    public float getFriction(){
        return baseFriction;
    }

    public float getInertia(){
       return baseInertia;
    }

    public float getForce(){
       return baseForce;
    }
}
