package unity.world.graph;

import arc.math.geom.*;
import arc.struct.*;
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
    float minDistance,maxDistance; //only used for distance
    public OrderedSet<GraphEdge> connections = new OrderedSet<>();

    //similar to the above but its all the locations block can connect to,
    // rather then the connections themselves
    // in DISTANCE connection type this holds nothing.


    public GraphConnector(GraphNode node,T graph){
        this.node = node;
        graph.addVertex(this);
    }

    public void update(){}

    public void onProximityUpdate(){

    }

    public abstract void recalcPorts();

    public abstract void recalcNeighbours();

    public abstract boolean canConnect(Point2 pt, GraphConnector<T> conn);

    public boolean isConnected(GraphConnector t){
        for(GraphEdge edge:connections){
            if(edge.other(this)==t){
                return true;
            }
        }
        return false;
    }
    public boolean isConnected(GraphBuild t){
        for(GraphEdge edge:connections){
            if(edge.other(this).node.build==t){
                return true;
            }
        }
        return false;
    }

    public void disconnect(){
        graph.removeVertex(this);
    }

    public void removeEdge(GraphEdge ge){
        if(connections.remove(ge)){
            triggerConnectionChanged();
        }
    }

    public void triggerConnectionChanged(){
        this.node.build.onConnectionChanged(this);
    }

    ///derivative classes
    //connections in fixed locations like at the ends of a block
    public static class FixedGraphConnector<U extends Graph> extends GraphConnector<U>{
        int[] connectionPointIndexes;
        public ConnectionPort connectionPoints[];
        public FixedGraphConnector(GraphNode node, U graph,int... connections){
            super(node,graph);
            connectionPointIndexes = connections;
        }

        @Override public void recalcPorts(){
            if(connections.size>0){
                throw new IllegalStateException("graph connector must have no connections before port recalc");
            }
            connectionPoints = surfaceConnectionsOf(this,connectionPointIndexes);
        }

        @Override public void recalcNeighbours(){
            if(connectionPoints==null){
                recalcPorts();
            }
            connections.clear();

            //clear edges from graph as well?

            Tile intrl = node.build().tile;
            Point2 temp = new Point2();
            for(ConnectionPort cp:connectionPoints){
                //for each connection point get the relevant tile it connects to. If its a connection point, then attempt a connection.
                temp.set(intrl.x,intrl.y).add(cp.relpos).add(cp.dir);
                Building building = Vars.world.build(temp.x,temp.y);
                if(building!=null && building instanceof GraphBuild igraph){
                    var extnode = igraph.getGraphNode(graph.getClass());
                    if(extnode==null){
                        continue;
                    }
                    for(var extconnector: extnode.connector){
                        if(extconnector.canConnect(cp.relpos.cpy().add(cp.dir),(GraphConnector)this)){
                            long edgeid = GraphEdge.getId(this,extconnector);
                            if(graph.edges.containsKey(edgeid)){
                                if(!connections.contains((GraphEdge)graph.edges.get(edgeid))){
                                    var edge = (GraphEdge)graph.edges.get(edgeid);
                                    connections.add(edge);
                                }
                                continue;
                            }
                            var edge = new GraphEdge(this, extconnector);
                            graph.addEdge(edge);
                            connections.add(edge);
                            extconnector.connections.add(edge);
                            extconnector.triggerConnectionChanged();
                        }
                    }
                }
            }
            triggerConnectionChanged();
        }
        public ConnectionPort[] surfaceConnectionsOf(GraphConnector gc, int[] connectids){
            Seq<ConnectionPort> ports = new Seq<>(connectids.length);
            for(int i =0;i<connectids.length;i++){
                if(connectids[i]==0){continue;}
                ports.add(getConnectSidePos(i,gc.node.block().size,gc.node.build().rotation));
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
            var c= new ConnectionPort(this,new Point2(originX, originY), new Point2(d4x(side), d4y(side)));;
            c.index = index;
            return c;
        }
        public ConnectionPort isConnectionPortHere(Point2 worldpos){
            if(connectionPoints==null){
                recalcPorts();
            }
            Tile intrl = node.build().tile;
            Point2 pt = (worldpos).cpy();
            pt.sub(intrl.x,intrl.y);
            for(ConnectionPort cp:connectionPoints){
                if(pt.equals(cp.relpos.x,cp.relpos.y)){
                    return cp;
                }
            }
            return null;
        }

        public boolean areConnectionPortsConnectedTo(Point2 worldPortPos,Building building){
            if(connectionPoints==null){
                recalcPorts();
            }
            Tile intrl = node.build().tile;
            Point2 pt = (worldPortPos).cpy();
            pt.sub(intrl.x,intrl.y);
            for(ConnectionPort cp:connectionPoints){
                if(pt.equals(cp.relpos.x,cp.relpos.y) && cp.connectedToTile().build==building){
                    return true;
                }
            }
            return false;
        }

        public boolean canConnect(Point2 pt, GraphConnector<U> conn){
           // Point2 pt =(external.relpos.cpy()).add(external.dir);
            Tile ext = conn.node.build().tile;
            pt.add(ext.x,ext.y);
            return areConnectionPortsConnectedTo(pt,conn.node.build());
        }
        public static class ConnectionPort{
            Point2 relpos;// position of attachment within the block
            Point2 dir; //if 0,0, is universal direction connector (?? thoh 6 months later idk if i want to do that)
            boolean occupied=false;
            GraphConnector connector;
            int index=-1;

            public ConnectionPort(GraphConnector connector,Point2 relpos, Point2 dir){
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
                return Vars.world.tile(connector.node.build().tile.x+relpos.x+dir.x,connector.node.build().tile.y+relpos.y+dir.y);
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
