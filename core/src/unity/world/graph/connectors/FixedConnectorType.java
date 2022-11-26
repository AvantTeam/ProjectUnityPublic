package unity.world.graph.connectors;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.GraphNodeType.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

import static arc.math.geom.Geometry.*;

@SuppressWarnings("unchecked")
public class FixedConnectorType<T extends Graph<T>> extends GraphConnectorType<T> implements FixedConnectorTypeI<T>{
    public int[] connectionIndices;

    public FixedConnectorType(Prov<T> newGraph, int... connectionIndices){
        super(newGraph);
        this.connectionIndices = connectionIndices;
    }

    @Override
    public FixedConnector<T> create(GraphNodeI<T> node){
        return new FixedConnector<>((GraphNode<T>)node, newGraph.get(), connectionIndices);
    }

    @Override
    public int[] connectionIndices(){
        return connectionIndices;
    }

    public static class FixedConnector<T extends Graph<T>> extends GraphConnector<T> implements FixedConnectorI<T>{
        int[] connectionIndices;
        public ConnectionPort<T>[] connectionPoints;
        public Boolf2<ConnectionPort<T>, ConnectionPort<T>> portCompatibility;

        public FixedConnector(GraphNode<T> node, T graph, int... connections){
            super(node, graph);
            connectionIndices = connections;
        }

        @Override
        public int[] connectionIndices(){
            return connectionIndices;
        }

        @Override
        public void recalcPorts(){
            if(connections.size > 0) throw new IllegalStateException("graph connector must have no connections before port recalc");
            connectionPoints = surfaceConnectionsOf(this, connectionIndices);
        }

        @Override
        public void recalcNeighbours(){
            if(connectionPoints == null) recalcPorts();

            // Xelo: disconnect?
            if(connections.size > 0) disconnect();
            for(GraphEdge<T> edge : connections){
                if(edge.valid){
                    edge.valid = false;
                    Log.warn("Deleted valid edge, this may cause issues.");
                }
            }

            connections.clear();

            // Xelo: clear edges from graph as well?

            Tile intrl = node.build().tile;
            Point2 temp = new Point2();
            for(ConnectionPort<T> port : connectionPoints){
                //for each connection point get the relevant tile it connects to. If its a connection point, then attempt a connection.
                temp.set(intrl.x, intrl.y).add(port.relpos).add(port.dir);
                Building building = Vars.world.build(temp.x, temp.y);
                if(building != null && building instanceof GraphBuild igraph){
                    var extnode = (GraphNode<T>)igraph.graphNode(graph.type());
                    if(extnode == null) continue;
                    for(var extconnector : extnode.connectors){
                        if(!(extconnector instanceof FixedConnector fixed)) continue;
                        var edge = fixed.tryConnect(port.relpos.cpy().add(port.dir), (GraphConnector<T>)this);
                        if(edge != null){
                            port.edge = edge;
                            if(!connections.contains(edge)) connections.add(edge);
                        }

                    }
                }

                if(port.edge != null && !port.edge.valid) port.edge = null;
            }

            triggerConnectionChanged();
        }

        public ConnectionPort<T>[] surfaceConnectionsOf(GraphConnector<T> gc, int[] connectids){
            Seq<ConnectionPort<T>> ports = new Seq<>(connectids.length);
            for(int i = 0; i < connectids.length; i++){
                if(connectids[i] == 0){
                    continue;
                }
                var port = getConnectSidePos(i, gc.node.block().size, gc.node.build().rotation);
                port.ordinal = ports.size;
                ports.add(port);
            }

            return ports.toArray(ConnectionPort.class);
        }

        // Xelo: this came from js, but im not sure if it relative to the center or the bl corner of the building.
        //gets positions along the sides.
        public ConnectionPort<T> getConnectSidePos(int index, int size, int rotation){
            int side = index / size;
            side = (side + rotation) % 4;
            Point2 normal = d4((side + 3) % 4);
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
            var c = new ConnectionPort<T>(this, new Point2(originX, originY), new Point2(d4x(side), d4y(side)));

            c.index = index;
            return c;
        }

        public ConnectionPort<T> isConnectionPortHere(Point2 worldpos){
            if(connectionPoints == null){
                recalcPorts();
            }
            Tile intrl = node.build().tile;
            Point2 pt = (worldpos).cpy();
            pt.sub(intrl.x, intrl.y);
            for(ConnectionPort<T> cp : connectionPoints){
                if(pt.equals(cp.relpos.x, cp.relpos.y)){
                    return cp;
                }
            }
            return null;
        }

        public ConnectionPort<T> areConnectionPortsConnectedTo(Point2 worldPortPos, Building building){
            if(connectionPoints == null) recalcPorts();

            Tile intrl = node.build().tile;
            Point2 pt = (worldPortPos).cpy();
            pt.sub(intrl.x, intrl.y);
            for(ConnectionPort<T> cp : connectionPoints){
                if(pt.equals(cp.relpos.x, cp.relpos.y) && cp.connectedToTile().build == building){
                    return cp;
                }
            }

            return null;
        }

        public boolean canConnect(Point2 pt, GraphConnector<T> conn){
            // Xelo: Point2 pt =(external.relpos.cpy()).add(external.dir);
            Tile ext = conn.node.build().tile;
            pt.add(ext.x, ext.y);
            return areConnectionPortsConnectedTo(pt, conn.node.build()) != null;
        }

        public GraphEdge<T> tryConnectPorts(ConnectionPort<T> port, GraphConnector<T> extconn){
            Tile ext = extconn.node.build().tile;
            var pos = port.relpos.cpy().add(port.dir).add(ext.x, ext.y);
            var extport = areConnectionPortsConnectedTo(pos, extconn.node.build());
            if(extport == null || portCompatibility.get(port,extport)) return null;

            GraphEdge<T> edge = addEdge(extconn);
            if(edge != null) extport.edge = edge;

            return edge;
        }

        @Override
        public GraphEdge<T> tryConnect(Point2 pt, GraphConnector<T> extconn){
            Tile ext = extconn.node.build().tile;
            pt.add(ext.x, ext.y);
            var port = areConnectionPortsConnectedTo(pt, extconn.node.build());
            if(port == null) return null;

            GraphEdge<T> edge = addEdge(extconn);
            if(edge != null) port.edge = edge;

            return edge;
        }

        @Override
        public void removeEdge(GraphEdge<T> ge){
            super.removeEdge(ge);
            if(connectionPoints == null){
                return; //how this occurs i dont know
            }

            for(ConnectionPort<T> cp : connectionPoints){
                if(cp.edge != null && !cp.edge.valid) cp.edge = null;
            }
        }

        public void eachConnected(Cons2<GraphConnector<T>, ConnectionPort<T>> cons){
            if(connectionPoints == null) return;
            for(ConnectionPort<T> port : connectionPoints){
                if(port.edge != null){
                    cons.get(port.edge.other(this), port);
                }
            }
        }

        public static class ConnectionPort<T extends Graph<T>>{
            Point2 relpos;// Xelo: position of attachment within the block
            Point2 dir;
            boolean occupied = false;
            GraphConnector<T> connector;
            int index = -1;
            int ordinal = 0;
            public GraphEdge<T> edge = null;

            public ConnectionPort(GraphConnector<T> connector, Point2 relpos, Point2 dir){
                this.relpos = relpos;
                this.dir = dir;
                this.connector = connector;
            }

            public int getIndex(){
                return index;
            }

            public Point2 getDir(){
                return dir;
            }

            public Point2 getRelpos(){
                return relpos;
            }

            public Tile connectedToTile(){
                return Vars.world.tile(connector.node.build().tile.x + relpos.x + dir.x, connector.node.build().tile.y + relpos.y + dir.y);
            }

            public int getOrdinal(){
                return ordinal;
            }
        }
    }
}
