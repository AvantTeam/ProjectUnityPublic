package unity.world.graph;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.world.*;
import unity.world.graph.GraphConnector.FixedGraphConnector.*;

import java.lang.reflect.*;

import static arc.math.geom.Geometry.*;
import static mindustry.Vars.tilesize;

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

    public Block getBlock(){
        return block;
    }

    public void drawConnectionPoints(BuildPlan req, Eachable<BuildPlan> list){
        //soon
        for(ConnectionConfig c:connections){
            TextureRegion tr = Graphs.graphIcons.get(c.graphType);
            if(tr == null){
                return;
            }
            if(c instanceof FixedConnectionConfig fcc){
                for(int i = 0;i<fcc.connectionIndexes.length;i++){
                    if(fcc.connectionIndexes[i]!=0){
                        Point2 p2 = getConnectSidePos(i,this.block.size,req.rotation);
                        Draw.rect(tr,(req.x+p2.x)*tilesize,(req.x+p2.y)*tilesize);
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
