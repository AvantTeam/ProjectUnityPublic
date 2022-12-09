package unity.world.graph;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.func.*;
import unity.gen.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.connectors.*;
import unity.world.graph.connectors.GraphConnectorTypeI.*;
import unity.world.graph.nodes.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

import static arc.math.geom.Geometry.*;
import static mindustry.Vars.*;

/**
 * Common definition of the graph blocks. Automatically implemented in the graph annotation processor. Non-default methods must
 * define their statements in the annotation processor.
 * 
 * If you want to extend a generated graph block class and add your own graph binding, override these and add-on to your
 * specific graph fields:
 * <ul>
 *     <li>{@link GraphBlock#eachNodeType(IntObjc<GraphNodeTypeI<?>>)}</li>
 *     <li>{@link GraphBlock#eachConnectorType(IntObjc<GraphConnectorTypeI<?>>)}</li>
 * 
 *     <li>{@link GraphBuild#initGraphNodes()}</li>
 *     <li>{@link GraphBuild#eachNode(IntObjc<GraphNodeI<?>>)}</li>
 *     <li>{@link GraphBuild#graphNode(int)}</li>
 * </ul>
 * @author GlennFolker
 * @author Xelo
 */
@GraphBlockBase
@SuppressWarnings("unchecked")
public interface GraphBlock{
    void eachNodeType(IntObjc<GraphNodeTypeI<?>> cons);
    void eachConnectorType(IntObjc<GraphConnectorTypeI<?>> cons);

    default void setGraphStats(Stats stats){
        eachNodeType((flag, node) -> node.setStats(stats));
    }

    default void drawConnectionPoints(BuildPlan req, Eachable<BuildPlan> list){
        eachConnectorType((flag, conn) -> conn.drawConnectionPoint(req, list));
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
        void initGraphNodes();
        void onGraphInit();

        boolean graphInitialized();

        void onConnectionChanged(GraphConnectorI<?> connector);

        void eachNode(IntObjc<GraphNodeI<?>> cons);

        <N extends GraphNodeI<? extends GraphI<?>>> N graphNode(int type);

        default <C extends GraphConnectorI<? extends GraphI<?>>> C graphConnector(int type, Class<? super C> connType){
            var node = graphNode(type);
            return node == null ? null : (C)node.connectors().find(c -> connType.isInstance(c));
        }

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
