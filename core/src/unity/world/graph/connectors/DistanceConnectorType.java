package unity.world.graph.connectors;

import arc.func.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.world.*;
import unity.world.graph.*;
import unity.world.graph.GraphBlock.*;
import unity.world.graph.nodes.*;
import unity.world.graph.nodes.GraphNodeType.*;
import unity.world.graph.nodes.GraphNodeTypeI.*;

@SuppressWarnings("unchecked")
public class DistanceConnectorType<T extends Graph<T>> extends GraphConnectorType<T>{
    public int connections = 1;

    public DistanceConnectorType(Prov<T> newGraph, int connections){
        super(newGraph);
        this.connections = connections;
    }

    @Override
    public DistanceConnector<T> create(GraphNodeI<T> node){
        return new DistanceConnector<>(node.as(), newGraph.get(), this);
    }

    public static class DistanceConnector<T extends Graph<T>> extends GraphConnector<T>{
        public int maxConnections;
        public Connection[] distConnections;
        int validConnections = 0;

        public DistanceConnector(GraphNode<T> node, T graph, DistanceConnectorType<T> type){
            super(node, graph, type);
            maxConnections = type.connections;
            distConnections = new Connection[maxConnections];
            disconnectWhenRotate = false;
        }

        public Point2 first(){
            for(var conn : distConnections){
                if(conn == null) continue;

                var p2 = conn.relpos;
                if(p2.x == 0 && p2.y == 0) continue;

                return p2;
            }

            return null;
        }

        public void refreshValidConnections(){
            validConnections = 0;
            for(var conn : distConnections){
                if(conn == null) continue;

                var p2 = conn.relpos;
                if(p2.x == 0 && p2.y == 0) continue;

                validConnections++;
            }
        }

        public int validConnections(){
            return validConnections;
        }

        public void resize(int size){
            maxConnections = size;
            var newconnection = new Connection[maxConnections];
            System.arraycopy(distConnections, 0, newconnection, 0, Math.min(distConnections.length, size));
            distConnections = newconnection;
            refreshValidConnections();
        }

        public void connectTo(DistanceConnector<T> other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;

            var edge = other.tryConnect(new Point2(cur.x - ext.x, cur.y - ext.y), this);
            if(edge != null){
                if(!connections.contains(edge)) connections.add(edge);
                addConnection(edge, other);
            }
        }

        @Override
        public void recalcPorts(){
            // Xelo: doesnt need to
        }

        @Override
        public void recalcNeighbors(){
            connections.clear();
            for(var conn : distConnections){
                if(conn == null) continue;

                var p2 = conn.relpos;
                if(p2.x == 0 && p2.y == 0) continue;

                connectTo(p2.x, p2.y);
            }

            refreshValidConnections();
        }

        @Override
        public void sideChanged(GraphEdge<T> edge, boolean n2){
            DistanceConnector<T> other = edge.other(this);

            var conn = getConnection(other);
            if(conn != null) conn.isN2 = n2;
        }

        protected Connection getConnection(DistanceConnector<T> other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;

            for(var conn : distConnections){
                if(conn == null) continue;

                var p2 = conn.relpos;
                if(p2.x == ext.x - cur.x && p2.y == ext.y - cur.y) return conn;
            }

            return null;
        }

        protected boolean addConnection(@Nullable GraphEdge<T> edge, DistanceConnector<T> other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;
            Point2 relpos = new Point2(ext.x - cur.x, ext.y - cur.y);
            for(var conn : distConnections){
                if(conn == null) continue;

                var p2 = conn.relpos;
                if(p2 != null && (p2.x == relpos.x && p2.y == relpos.y)){
                    if(edge != null) edge.setN2(conn.isN2 ? this : edge.other(this)); // Glenn: just in case
                    return true; // Xelo: it exists already!
                }
            }

            for(int i = 0; i < distConnections.length; i++){
                if(distConnections[i] == null || (distConnections[i].relpos.x == 0 && distConnections[i].relpos.y == 0)){
                    distConnections[i] = new Connection(relpos, edge == null ? false : edge.n2 == this);
                    refreshValidConnections();
                    return true;
                }
            }

            return false;
        }

        protected void removeConnection(DistanceConnector<T> other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;
            Point2 relpos = new Point2(ext.x - cur.x, ext.y - cur.y);
            for(int i = 0; i < distConnections.length; i++){
                var conn = distConnections[i];
                if(conn == null) continue;

                Point2 p2 = conn.relpos;
                if(p2.x == relpos.x && p2.y == relpos.y){
                    distConnections[i] = null;
                    refreshValidConnections();
                    return;
                }
            }
        }

        public void connectTo(int rx, int ry){
            Tile intrl = node.build().tile;
            Building build = Vars.world.build(intrl.x + rx, intrl.y + ry);
            if(build == null) return;

            if(build instanceof GraphBuild graphBuild){
                GraphNode<T> extnode = graphBuild.graphNode(graph.type());
                if(extnode == null) return;

                for(GraphConnector<T> extconnector : extnode.connectors){
                    if(!(extconnector instanceof DistanceConnector<T> extdist)) continue;

                    var edge = extdist.tryConnect(new Point2(-rx, -ry), this);
                    if(edge != null){
                        if(!connections.contains(edge)) connections.add(edge);
                        addConnection(edge, extdist);
                    }
                }
            }
        }

        public void disconnectTo(DistanceConnector<T> other){
            GraphEdge<T> toRemove = null;
            for(GraphEdge<T> edge : connections){
                if(edge.other(this) == other){
                    toRemove = edge;
                    break;
                }
            }

            if(toRemove == null) return;

            removeConnection(other);
            other.removeConnection(this);
            graph.removeEdge(toRemove);
        }

        @Override
        public void disconnect(){
            while(!connections.isEmpty()) disconnectTo(connections.first().other(this));
            super.disconnect();
        }

        @Override
        public boolean canConnect(Point2 pt, GraphConnectorI<T> conn){
            return false;
        }

        @Override
        public GraphEdge<T> tryConnect(Point2 pt, GraphConnectorI<T> extconn){
            if(!priorityCompatible(extconn)) return null;

            var conn = getConnection(extconn.as());
            boolean exists = conn != null, n2 = exists && conn.isN2;

            if(addConnection(null, extconn.as())){
                var edge = addEdge(extconn);
                // Glenn: if the connection already exists it must be from save files, so override no matter what
                if(exists) edge.setN2(n2 ? this : extconn);

                return edge;
            }

            return null;
        }

        @Override
        public void write(Writes write){
            for(int i = 0; i < maxConnections; i++){
                var conn = distConnections[i];
                if(conn == null || (conn.relpos.x == 0 && conn.relpos.y == 0)){
                    write.bool(false);
                }else{
                    write.bool(true);
                    write.i(conn.relpos.pack());
                    write.bool(conn.isN2);
                }
            }
        }

        @Override
        public void read(Reads read){
            for(int i = 0; i < maxConnections; i++){
                boolean exists = read.bool();
                if(!exists){
                    distConnections[i] = null;
                }else{
                    int packed = read.i();
                    boolean n2 = read.bool();
                    distConnections[i] = new Connection(Point2.unpack(packed), n2);
                }
            }

            refreshValidConnections();
        }

        public static class Connection{
            public Point2 relpos;
            public boolean isN2;

            public Connection(Point2 relpos, boolean isN2){
                this.relpos = relpos;
                this.isN2 = isN2;
            }
        }
    }
}
