package unity.world.graph;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import unity.world.graph.GraphConnector.*;

import static arc.math.geom.Geometry.*;
import static mindustry.Vars.*;

//the uh block settings part of graphs ig
public class GraphBlockConfig{
    public ObjectMap<Class<? extends Graph>,Func<GraphBuild,GraphNode<?>>> nodeConfig = new ObjectMap<>();
    public Seq<ConnectionConfig> connections = new Seq<>();
    Block block;

    public GraphBlockConfig(Block block){
        this.block = block;
    }

    public void addConnectionConfig(ConnectionConfig cfg){
        //this is basically an assert right
        if(cfg.config!=null){
            throw new IllegalArgumentException("GraphBlockConfig cannot accept an already-bound ConnectionConfig");
        }
        connections.add(cfg);
        cfg.setConfig(this);
    }

    public <T extends Graph> void fixedConnection(Class<T> tClass, int... connectionIndexes){
        try{
            var constructor = tClass.getConstructor();
            addConnectionConfig(new FixedConnectionConfig(tClass,()-> {
                try{ return constructor.newInstance(); } catch(Exception e) { e.printStackTrace(); }
                return null;
            },connectionIndexes));
        }catch(Exception e){
            throw new IllegalStateException("Graph doesn't have a empty constructor or constructor is invalid/inaccessible");
        }
    }

    public <T extends Graph> void distanceConnection(Class<T> tClass, int connections){
        try{
            var constructor = tClass.getConstructor();
            addConnectionConfig(new DistanceConnectionConfig(tClass,()-> {
                try{ return constructor.newInstance(); } catch(Exception e) { e.printStackTrace(); }
                return null;
            },connections));
        }catch(Exception e){
            throw new IllegalStateException("Graph doesn't have a empty constructor or constructor is invalid/inaccessible");
        }
    }

    public static abstract class ConnectionConfig<T extends Graph>{
        public GraphBlockConfig config;
        Class<T> graphType;

        public abstract GraphConnector getConnector(GraphNode gn);
        public Prov<T> newGraph;

        public ConnectionConfig( Class<T> tClass,Prov<T> newGraph){
            this.newGraph = newGraph;
            graphType = tClass;
        }

        public void setConfig(GraphBlockConfig config){
            this.config = config;
        }

        public Class<T> getGraphType(){
            return graphType;
        }
    }

    public static class DistanceConnectionConfig<T extends Graph>  extends ConnectionConfig<T>{
        int connections;
        public DistanceConnectionConfig(Class<T> tClass, Prov<T> newGraph, int connections){
            super(tClass, newGraph);
            this.connections=connections;
        }

        @Override
        public GraphConnector getConnector(GraphNode gn){
            return new DistanceGraphConnector(connections,gn, newGraph.get());
        }
    }

    //for connections on the sides, most probably the most common type.
    public static class FixedConnectionConfig<T extends Graph>  extends ConnectionConfig<T>{
        int[] connectionIndexes;
        public FixedConnectionConfig( Class<T> tClass,Prov<T> newGraph, int... connectionIndexes){
            super( tClass,newGraph);
            this.connectionIndexes = connectionIndexes;
        }

        @Override public GraphConnector getConnector(GraphNode gn){
            return new GraphConnector.FixedGraphConnector(gn, newGraph.get(),connectionIndexes);
        }

        @Override public void setConfig(GraphBlockConfig config){
            super.setConfig(config);
            if(connectionIndexes.length != config.block.size*4){
                throw  new IllegalStateException(config.block.name+": Number of connectionIndexes do not match the size of the block x 4, was"+connectionIndexes.length+", expected: "+(config.block.size*4));
            }
        }
    }

    public void setStats(Stats stats){
        for(var gcnfig : nodeConfig){
            GraphNode<?> gn = gcnfig.value.get(null);
            gn.setStats(stats);
        }
    }

    public Block getBlock(){
        return block;
    }

    public void drawConnectionPoints(BuildPlan req, Eachable<BuildPlan> list){
        //soon
        for(ConnectionConfig c:connections){
            TextureRegion tr = Graphs.graphInfo.get(c.graphType).icon;
            if(tr == null){
                return;
            }
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
        }
    }

    //this came from js, but im not sure if it relative to the center or the bl corner of the building.
    //gets positions along the sides.
    public Point2 getConnectSidePos(int index, int size, int rotation){
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
        return new Point2(originX+d4x(side),originY+d4y(side));
    }




}
