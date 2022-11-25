package unity.world.graph;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.func.*;
import unity.gen.graph.*;
import unity.world.graph.connectors.*;
import unity.world.graph.connectors.GraphConnectorTypeI.*;
import unity.world.graph.nodes.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

import static arc.math.geom.Geometry.*;

/**
 * Common definition of the graph blocks. Automatically implemented in the graph annotation processor. Non-default methods must
 * define their statements in the annotation processor.
 * @author GlennFolker
 * @author Xelo
 */
@GraphBase
public interface GraphBlock{
    void eachNodeType(LongObjc<GraphNodeTypeI<?>> cons);

    default void setGraphStats(Stats stats){
        eachNodeType((flag, node) -> node.setStats(stats));
    }

    void drawConnectionPoints(BuildPlan req, Eachable<BuildPlan> list);
    default void drawConnectionPoint(GraphConnectorTypeI<?> connector, BuildPlan req, Eachable<BuildPlan> list){
        TextureRegion tr = Graphs.info(connector.graphType()).icon;
        if(!Core.atlas.isFound(tr)) return;

        /*
        if(c instanceof FixedConnectionConfig fcc){
            for(int i = 0;i<fcc.connectionIndexes.length;i++){
                if(fcc.connectionIndexes[i]!=0){
                    Point2 p2 = getConnectSidePos(i,this.block.size,req.rotation);
                    int cx = req.x+p2.x;
                    int cy = req.y+p2.y;
                    boolean[] d = {false};
                    list.each(b->{
                        if(d[0]){return;}
                        if(cx>=b.x && cy>=b.y && b.x+b.block.size>cx && b.y+b.block.size>cy){
                            d[0] = true;
                        }
                    });
                    if(d[0]){
                        continue;
                    }
                    Draw.rect(tr,cx*tilesize,cy*tilesize);
                }
            }
        }
        */
    }

    static Point2 getConnectSidePos(int index, int size, int rotation){
        int side = index / size;
        side = (side + rotation) % 4;

        Point2 tangent = d4((side + 1) % 4);
        int originX = 0, originY = 0;
        if(size > 1){
            originX += size / 2;
            originY += size / 2;
            originY -= size - 1;
            if(side > 0){
                for(int i = 1; i <= side; i++){
                    originX += d4x(i) * (size - 1);
                    originY += d4y(i) * (size - 1);
                }
            }
            originX += tangent.x * (index % size);
            originY += tangent.y * (index % size);
        }

        return new Point2(originX + d4x(side), originY + d4y(side));
    }

    interface GraphBuild extends Buildingc{
        int prevRotation();
        void prevRotation(int prevRotation);

        void initGraph();
        boolean graphInitialized();
        default void onGraphInit(){}

        default void onConnectionChanged(GraphConnectorI<?> connector){}

        void eachNode(LongObjc<GraphNodeI<?>> cons);

        default void connectToGraph(){
            eachNode((flag, node) -> node.addSelf());
        }

        default void disconnectFromGraph(){
            eachNode((flag, node) -> node.removeSelf());
        }

        default void onRotate(){
            eachNode((flag, node) -> node.onRotate());
        }

        default void displayGraphBars(Table table){
            eachNode((flag, node) -> node.displayBars(table));
        }

        default void updateGraphs(){
            if(prevRotation() == -1){
                connectToGraph();
                prevRotation(rotation());
            }

            if(prevRotation() != rotation() && block().rotate){
                onRotate();
                prevRotation(rotation());
            }

            eachNode((flag, node) -> {
                // Xelo: change later.
                node.update();
                for(var conn : node.connectors()){
                    conn.graph().update();
                }
            });
        }

        default void writeGraphs(Writes write){
            eachNode((flag, node) -> node.write(write));
        }

        default void readGraphs(Reads read){
            eachNode((flag, node) -> node.read(read));
        }
    }
}
