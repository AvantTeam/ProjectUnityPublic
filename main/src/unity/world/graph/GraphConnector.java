package unity.world.graph;

import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.world.*;

import static arc.math.geom.Geometry.*;

/*
 * This class acts as the thing that'll connect to the graph network as a vertex.
 * It handles connecting to adjacent blocks (and thus finding edges) as well.
 * */
public abstract class GraphConnector<T extends Graph>{
    public final int id = idAccum++;
    private static int idAccum = 0;
    GraphNode node;
    T graph;
    float minDistance, maxDistance; //only used for distance
    public OrderedSet<GraphEdge> connections = new OrderedSet<>();
    boolean disconnectWhenRotate = true;


    public GraphConnector(GraphNode node, T graph){
        this.node = node;
        graph.addVertex(this);
    }

    public void update(){
    }

    public void onProximityUpdate(){

    }

    public abstract void recalcPorts();

    public abstract void recalcNeighbours();

    public abstract boolean canConnect(Point2 pt, GraphConnector<T> conn);

    public abstract GraphEdge tryConnect(Point2 pt, GraphConnector<T> conn);

    public boolean isConnected(GraphConnector t){
        for(GraphEdge edge : connections){
            if(edge.other(this) == t){
                return true;
            }
        }
        return false;
    }

    public void eachConnected(Cons<GraphConnector<T>> cons){
        for(GraphEdge edge : connections){
            cons.get(edge.other(this));
        }
    }

    public boolean isConnected(GraphBuild t){
        for(GraphEdge edge : connections){
            if(edge.other(this).node.build == t){
                return true;
            }
        }
        return false;
    }

    public void disconnect(){
        graph.removeVertex(this);
        if(connections.size>0){
            Log.info("[scarlet] disconnected vertex still has edges!");
        }
    }

    public void removeEdge(GraphEdge ge){
        if(connections.remove(ge)){
            ge.valid = false;
            triggerConnectionChanged();
        }
    }

    public void triggerConnectionChanged(){
        this.node.build.onConnectionChanged(this);
    }
    public void write(Writes write){}
    public void read(Reads read){}

    public GraphEdge addEdge(GraphConnector extconn){
        long edgeid = GraphEdge.getId(this, extconn);
        if(graph.edges.containsKey(edgeid)){
           if(!connections.contains((GraphEdge)graph.edges.get(edgeid))){
               var edge = (GraphEdge)graph.edges.get(edgeid);
               connections.add(edge);
               edge.valid = true; // in case.
           }
           return (GraphEdge)graph.edges.get(edgeid);
        }
        var edge = new GraphEdge(this, extconn);
        graph.addEdge(edge);
        connections.add(edge);
        triggerConnectionChanged();
        return edge;
    }



    ///derivative classes
    //single distance connections?
    public static class DistanceGraphConnector<U extends Graph> extends GraphConnector<U>{
        public int maxConnections = 1;
        public Point2[] connection; // connection?
        int validConnections = 0;
        public DistanceGraphConnector(GraphNode node, U graph){
            super(node, graph);
            connection = new Point2[maxConnections];
            disconnectWhenRotate = false;
        }
        public DistanceGraphConnector(int connections, GraphNode node, U graph){
            super(node, graph);
            maxConnections = connections;
            connection = new Point2[maxConnections];
            disconnectWhenRotate = false;
        }
        public Point2 first(){
            for(Point2 p2:connection){
                if(p2 == null || (p2.x == 0 && p2.y == 0)){
                    continue;
                }
                return p2;
            }
            return null;
        }
        public void refreshValidConnections(){
            validConnections = 0;
            for(Point2 p2:connection){
                if(p2 == null || (p2.x == 0 && p2.y == 0)){
                    continue;
                }
                validConnections++;
            }
        }
        public int validConnections(){
            return validConnections;
        }

        public void resize(int size){
            maxConnections = size;
            Point2[] newconnection  = new Point2[maxConnections];
            System.arraycopy(connection, 0, newconnection, 0, Math.min(connection.length, size));
            connection = newconnection;
            refreshValidConnections();
        }
        public void connectTo(DistanceGraphConnector other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;

            var edge = other.tryConnect(new Point2(cur.x-ext.x,cur.y-ext.y), this);
            if(edge != null){
                if(!connections.contains(edge)){
                    connections.add(edge);
                }
                addConnection(other);
            }

        }
        public void connectTo(int rx,int ry){
            Tile intrl = node.build().tile;
            Building build = Vars.world.build(intrl.x + rx, intrl.y + ry);
            if(build == null){
                return;
            }
            if(build instanceof GraphBuild graphBuild){
                var extnode = graphBuild.getGraphNode(graph.getClass());
                if(extnode == null){
                    return;
                }
                for(GraphConnector extconnector: extnode.connector){
                    if(!(extconnector instanceof GraphConnector.DistanceGraphConnector)){
                        continue;
                    }
                    var edge = extconnector.tryConnect(new Point2(-rx,-ry), this);
                    if(edge != null){
                        if(!connections.contains(edge)){
                            connections.add(edge);
                        }
                        addConnection((DistanceGraphConnector)extconnector);
                    }

                }
            }
        }

        @Override
        public void recalcPorts(){
            //doesnt need to
        }

        @Override
        public void recalcNeighbours(){
            connections.clear();
            for(Point2 p2:connection){
                if(p2 == null || (p2.x == 0 && p2.y == 0)){
                    continue;
                }
                connectTo(p2.x,p2.y);
            }
            refreshValidConnections();
        }
        boolean addConnection(DistanceGraphConnector other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;
            Point2 relpos = new Point2(ext.x-cur.x,ext.y-cur.y);
            for(Point2 point2 : connection){
                if(point2 != null && (point2.x == relpos.x && point2.y == relpos.y)){
                    return true; // it exists already!
                }
            }
            for(int i = 0;i<connection.length;i++){
                if(connection[i] == null || (connection[i].x == 0 && connection[i].y == 0)){
                    connection[i] = relpos;
                    refreshValidConnections();
                    return true;
                }
            }
            return false;
        }
        public void disconnectTo(DistanceGraphConnector other){
            GraphEdge toRemove = null;
            for(GraphEdge edge:connections){
                if(edge.other(this)==other){
                    toRemove = edge;
                    break;
                }
            }
            if(toRemove==null){
                return;
            }
            Log.info("disconnecting edge."+toRemove);
            removeConnection(other);
            other.removeConnection(this);
            graph.removeEdge(toRemove);
        }

        @Override
        public void disconnect(){
            Log.info("disconnecting.");
            while(!connections.isEmpty()){
                disconnectTo((DistanceGraphConnector) connections.first().other(this));
            }
            super.disconnect();
        }

        void removeConnection(DistanceGraphConnector other){
            Tile ext = other.node.build().tile;
            Tile cur = node.build().tile;
            Point2 relpos = new Point2(ext.x-cur.x,ext.y-cur.y);
            for(int i = 0;i<connection.length;i++){
                Point2 point2 = connection[i];
                if(point2 != null && (point2.x == relpos.x && point2.y == relpos.y)){
                    connection[i] = null;
                    refreshValidConnections();
                    Log.info("disconnected from:"+point2);
                    return;
                }
            }
        }

        @Override
        public boolean canConnect(Point2 pt, GraphConnector<U> conn){
            return false;
        }

        @Override
        public GraphEdge tryConnect(Point2 pt, GraphConnector<U> extconn){
            if(addConnection((DistanceGraphConnector)extconn)){
                return addEdge(extconn);
            }
            return null;
        }
        public void write(Writes write){
            for(int i = 0;i<maxConnections;i++){
                write.i(connection[i]==null?0:connection[i].pack());
            }
        }
        public void read(Reads read){
            for(int i = 0;i<maxConnections;i++){
                connection[i] = Point2.unpack(read.i());
            }
            refreshValidConnections();
        }
    }

    //connections in fixed locations like at the ends of a block
    public static class FixedGraphConnector<U extends Graph> extends GraphConnector<U>{
        int[] connectionPointIndexes;
        public ConnectionPort connectionPoints[];
        public Boolf2<ConnectionPort,ConnectionPort> portCompatibility;

        public FixedGraphConnector(GraphNode node, U graph, int... connections){
            super(node, graph);
            connectionPointIndexes = connections;
        }

        @Override
        public void recalcPorts(){
            if(connections.size > 0){
                throw new IllegalStateException("graph connector must have no connections before port recalc");
            }
            connectionPoints = surfaceConnectionsOf(this, connectionPointIndexes);
        }

        @Override
        public void recalcNeighbours(){
            if(connectionPoints == null){
                recalcPorts();
            }
            //disconnect?
            if(connections.size>0){
                disconnect();
            }
            for(GraphEdge edge:connections){
                if(edge.valid){
                    edge.valid = false;
                    Log.info("Deleted valid edge, this may cause issues.");
                }
            }
            connections.clear();

            //clear edges from graph as well?

            Tile intrl = node.build().tile;
            Point2 temp = new Point2();
            for(ConnectionPort port : connectionPoints){
                //for each connection point get the relevant tile it connects to. If its a connection point, then attempt a connection.
                temp.set(intrl.x, intrl.y).add(port.relpos).add(port.dir);
                Building building = Vars.world.build(temp.x, temp.y);
                if(building != null && building instanceof GraphBuild igraph){
                    var extnode = igraph.getGraphNode(graph.getClass());
                    if(extnode == null){
                        continue;
                    }
                    for(var extconnector : extnode.connector){
                        if(!(extconnector instanceof FixedGraphConnector)){
                            continue;
                        }
                        var edge = extconnector.tryConnect(port.relpos.cpy().add(port.dir), (GraphConnector)this);
                        if(edge != null){
                            port.edge = edge;
                            if(!connections.contains(edge)){
                                connections.add(edge);
                            }
                        }

                    }
                }
                if(port.edge != null && !port.edge.valid){
                    port.edge = null;
                }
            }
            triggerConnectionChanged();
        }

        public ConnectionPort[] surfaceConnectionsOf(GraphConnector gc, int[] connectids){
            Seq<ConnectionPort> ports = new Seq<>(connectids.length);
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

        //this came from js, but im not sure if it relative to the center or the bl corner of the building.
        //gets positions along the sides.
        public ConnectionPort getConnectSidePos(int index, int size, int rotation){
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
            var c = new ConnectionPort(this, new Point2(originX, originY), new Point2(d4x(side), d4y(side)));
            ;
            c.index = index;
            return c;
        }

        public ConnectionPort isConnectionPortHere(Point2 worldpos){
            if(connectionPoints == null){
                recalcPorts();
            }
            Tile intrl = node.build().tile;
            Point2 pt = (worldpos).cpy();
            pt.sub(intrl.x, intrl.y);
            for(ConnectionPort cp : connectionPoints){
                if(pt.equals(cp.relpos.x, cp.relpos.y)){
                    return cp;
                }
            }
            return null;
        }

        public ConnectionPort areConnectionPortsConnectedTo(Point2 worldPortPos, Building building){
            if(connectionPoints == null){
                recalcPorts();
            }
            Tile intrl = node.build().tile;
            Point2 pt = (worldPortPos).cpy();
            pt.sub(intrl.x, intrl.y);
            for(ConnectionPort cp : connectionPoints){
                if(pt.equals(cp.relpos.x, cp.relpos.y) && cp.connectedToTile().build == building){
                    return cp;
                }
            }
            return null;
        }

        public boolean canConnect(Point2 pt, GraphConnector<U> conn){
            // Point2 pt =(external.relpos.cpy()).add(external.dir);
            Tile ext = conn.node.build().tile;
            pt.add(ext.x, ext.y);
            return areConnectionPortsConnectedTo(pt, conn.node.build()) != null;
        }

        public GraphEdge tryConnectPorts(ConnectionPort port, GraphConnector<U> extconn){
            Tile ext = extconn.node.build().tile;
            var pos = port.relpos.cpy().add(port.dir).add(ext.x, ext.y);
            var extport = areConnectionPortsConnectedTo(pos, extconn.node.build());
            if(extport == null || portCompatibility.get(port,extport)){
               return null;
            }
            GraphEdge edge = addEdge(extconn);
            if(edge!=null){
                extport.edge = edge;
            }
            return edge;
        }

        @Override
        public GraphEdge tryConnect(Point2 pt, GraphConnector<U> extconn){
            Tile ext = extconn.node.build().tile;
            pt.add(ext.x, ext.y);
            var port = areConnectionPortsConnectedTo(pt, extconn.node.build());
            if(port == null){
                return null;
            }
            GraphEdge edge = addEdge(extconn);
            if(edge!=null){
                port.edge = edge;
            }
            return edge;
        }

        @Override
        public void removeEdge(GraphEdge ge){
            super.removeEdge(ge);
            if(connectionPoints == null){
                return; //how this occurs i dont know
            }
            for(ConnectionPort cp : connectionPoints){
                if(cp.edge != null && !cp.edge.valid){
                    cp.edge = null;
                }
            }
        }

        public void eachConnected(Cons2<GraphConnector<U>, ConnectionPort> cons){
            if(connectionPoints == null){
                return;
            }
            for(ConnectionPort port : connectionPoints){
                if(port.edge != null){
                    cons.get(port.edge.other(this), port);
                }
            }
        }

        public static class ConnectionPort{
            Point2 relpos;// position of attachment within the block
            Point2 dir;
            boolean occupied = false;
            GraphConnector connector;
            int index = -1;
            int ordinal = 0;
            public GraphEdge edge = null;

            public ConnectionPort(GraphConnector connector, Point2 relpos, Point2 dir){
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


    @Override
    public String toString(){
        return "GraphConnector{" +
        "id=" + id +
        ", node=" + node.build().block +
        '}';
    }

    public T getGraph(){
        return graph;
    }

    public GraphNode<T> getNode(){
        return node;
    }
}
