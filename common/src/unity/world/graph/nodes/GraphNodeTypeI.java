package unity.world.graph.nodes;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.connectors.GraphConnectorTypeI.*;

/**
 * Define public methods for graph nodes here.
 * @author GlennFolker
 */
@GraphNodeBase
@SuppressWarnings("unchecked")
public interface GraphNodeTypeI<T extends GraphI<T>>{
    <E extends Building & GraphBuild> GraphNodeI<T> create(E build);

    void setStats(Stats stats);

    default <N extends GraphNodeTypeI<? extends T>> N as(){
        return (N)this;
    }

    interface GraphNodeI<T extends GraphI<T>>{
        void update();

        void addSelf();
        void removeSelf();

        void onConnect();
        void onDisconnect();

        void onRotate();

        void displayBars(Table table);

        Seq<? extends GraphConnectorI<T>> connectors();
        void addConnector(GraphConnectorI<T> connector);

        void removeEdge(GraphNodeI<T> g);

        void write(Writes write);
        void read(Reads read);

        Block block();
        <E extends Building & GraphBuild> E build();
        T graph();

        default <N extends GraphNodeI<? extends T>> N as(){
            return (N)this;
        }
    }
}
