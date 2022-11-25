package unity.world.graph.nodes;

import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.world.meta.*;
import unity.annotations.Annotations.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.GraphConnectorTypeI.*;

public interface GraphNodeTypeI<T extends GraphI>{
    <E extends Building & GraphBuild> GraphNodeI<T> create(E build);

    void setStats(Stats stats);

    interface GraphNodeI<T extends GraphI>{
        void update();

        void addSelf();
        void removeSelf();

        void onConnect();
        void onDisconnect();

        void onRotate();

        void displayBars(Table table);

        Seq<? extends GraphConnectorI<T>> connectors();

        void write(Writes write);
        void read(Reads read);
    }
}
