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
        public Point2[] connection; // Xelo: connection?
        int validConnections = 0;

        public DistanceConnector(GraphNode<T> node, T graph, DistanceConnectorType<T> type){
            super(node, graph, type);
            maxConnections = type.connections;
            connection = new Point2[maxConnections];
            disconnectWhenRotate = false;
        }

        public Point2 first(){
            for(Point2 p2 : connection){
                if(p2 == null || (p2.x == 0 && p2.y == 0)) continue;
                return p2;
            }

            return null;
        }

        public void refreshValidConnections(){
            validConnections = 0;
            for(Point2 p2 : connection){
                if(p2 == null || (p2.x == 0 && p2.y == 0)) continue;
                validConnections++;
            }
        }

        public int validConnections(){
            return validConnections;
        }

        public void resize(int size){
            maxConnections = size;
            Point2[] newconnection = new Point2[maxConnections];
            System.arraycopy(connection, 0, newconnection, 0, Math.min(connection.length, size));
            connection = newconnection;
            refreshValidConnections();
        }

        public void connectTo(DistanceConnector<T> other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;

            var edge = other.tryConnect(new Point2(cur.x - ext.x, cur.y - ext.y), this);
            if(edge != null){
                if(!connections.contains(edge)) connections.add(edge);
                addConnection(other);
            }
        }

        @Override
        public void recalcPorts(){
            // Xelo: doesnt need to
        }

        @Override
        public void recalcNeighbors(){
            connections.clear();
            for(Point2 p2 : connection){
                if(p2 == null || (p2.x == 0 && p2.y == 0)) continue;
                connectTo(p2.x, p2.y);
            }

            refreshValidConnections();
        }

        protected boolean addConnection(DistanceConnector<T> other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;
            Point2 relpos = new Point2(ext.x - cur.x, ext.y - cur.y);
            for(Point2 point2 : connection){
                if(point2 != null && (point2.x == relpos.x && point2.y == relpos.y)){
                    return true; // Xelo: it exists already!
                }
            }

            for(int i = 0; i < connection.length; i++){
                if(connection[i] == null || (connection[i].x == 0 && connection[i].y == 0)){
                    connection[i] = relpos;
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
            for(int i = 0; i < connection.length; i++){
                Point2 point2 = connection[i];
                if(point2 != null && (point2.x == relpos.x && point2.y == relpos.y)){
                    connection[i] = null;
                    refreshValidConnections();
                    Log.info("disconnected from:" + point2);
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
                        addConnection(extdist);
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

            Log.info("disconnecting edge." + toRemove);
            removeConnection(other);
            other.removeConnection(this);
            graph.removeEdge(toRemove);
        }

        @Override
        public void disconnect(){
            Log.info("disconnecting.");

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
            if(addConnection(extconn.as())) return addEdge(extconn);
            return null;
        }

        @Override
        public void write(Writes write){
            for(int i = 0; i < maxConnections; i++){
                write.i(connection[i] == null ? 0 : connection[i].pack());
            }
        }

        @Override
        public void read(Reads read){
            for(int i = 0; i < maxConnections; i++){
                connection[i] = Point2.unpack(read.i());
            }

            refreshValidConnections();
        }
    }
}
