package unity.world.graph;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

public abstract class Graph<T extends Graph>{
    protected OrderedSet<GraphConnector<T>> vertexes = new OrderedSet<>();
    protected LongMap<GraphEdge> edges = new LongMap<>();
    public final int id;
    private static int lastId;

    public Graph(){}

    long lastFrameUpdated;
    {
        id = lastId++;
    }
    public Graph(GraphConnector<T> gc){
        addVertex(gc);
        onGraphChanged();
    }

    public abstract <U extends Graph<T>> U createFromThis();

    public abstract <U extends Graph<T>> void onMergeBegin(T g);
    public void addEdge(GraphEdge edge){

        edges.put(edge.id,edge);
        Graph<T> g = edge.other(this).graph;
        if(g!=this){
            //CONSUME THE INFERIOR GRAPH
            if(g.vertexes.size<vertexes.size){
                mergeGraph(g);
            }else{
                g.mergeGraph(this);
            }
        }
    }
    public void removeOnlyVertex(GraphConnector<T> vertex){
        if(vertex.getGraph()!=this){ Log.info("tried to remove invalid vertex"); return;}

        vertexes.remove(vertex);
        vertex.graph = null;
        onGraphChanged();
    }

    public void removeVertex(GraphConnector<T> vertex){
        if(vertex.getGraph()!=this){return;}
        if(vertex.connections.size==0){
            //trivial case 1, node has no connections anyway
            return;
        }
        else if(vertex.connections.size==1){
            //trivial case 2, node has 1 connection so we can just detach node into new graph,
            removeEdgeNonSplit(vertex.connections.first());
            Graph<T> ngraph = createFromThis();
            ngraph.addVertex(vertex);
            vertexes.remove(vertex);
            onGraphChanged();
        }else{
            //non trivial case, need to detach edges and check for splits.
            //probably change to do all the edges at once hmm
            int size = vertex.connections.size;
            for(int  i = 0;i<size;i++){
                GraphEdge vconn = vertex.connections.removeIndex(0);
                if(vertex.getGraph()!=this){
                    vertex.getGraph().removeVertex(vertex);
                    if(vertexes.contains(vertex)){
                        throw new IllegalStateException("Graph still contains deleted vertex after splitting? bug.");
                    }
                    return;
                }else{
                    removeEdge(vconn);
                }
            }
            vertexes.remove(vertex);
            onGraphChanged();
        }

    }
    public void removeEdgeNonSplit(GraphEdge edge){
        edges.remove(edge.id);
        edge.n1.connections.remove(edge);
        edge.n2.connections.remove(edge);
    }
    protected OrderedSet<GraphConnector<T>> floodTemp = new OrderedSet<>();
    public void removeEdge(GraphEdge edge){
        removeEdgeNonSplit(edge);
        if(!isConnected(edge.n1,edge.n2,floodTemp)){
            //OHNO
            if(floodTemp.size<= vertexes.size-floodTemp.size){
                //new graph will be the flooded area
                Graph<T> ngraph = createFromThis();
                for(GraphConnector<T> other : floodTemp){
                    this.removeOnlyVertex(other);
                    ngraph.addVertex(other);
                    for(GraphEdge ge: other.connections){
                        if(ge!=edge){
                            ngraph.edges.put(ge.id, ge);
                            edges.remove(ge.id);
                        }
                    }
                }
            }else{
                //this graph will be the flooded area
                Graph<T> ngraph = createFromThis();
                for(GraphConnector<T> other : vertexes){
                    if(!floodTemp.contains(other)){
                        ngraph.addVertex(other);
                        for(GraphEdge ge: other.connections){
                            if(ge!=edge){
                                ngraph.edges.put(ge.id, ge);
                                edges.remove(ge.id);
                            }
                        }
                    }
                }
                for(GraphConnector<T> other : ngraph.vertexes){
                    this.vertexes.remove(other);
                }
            }
        }
    }

    //floods the entire graph (if possible) from a point.
    public void floodFrom(GraphConnector<T> gc, OrderedSet<GraphConnector<T>> flood){
        flood.clear();
        Seq<GraphConnector<T>> front = new Seq<>();
        front.add(gc);
        flood.add(gc);
        while(front.any()){
            var current = front.pop();
            for(GraphEdge ge:current.connections){
                var next = ge.other(current);
                if(flood.contains(next)){
                    continue;
                }
                front.add(next);
                flood.add(next);
            }
        }
    }

    //returns if two vertexes are connected, if not, returns everything on v1's side of the graph.
    public boolean isConnected (GraphConnector<T> v1, GraphConnector<T> v2, OrderedSet<GraphConnector<T>> flood){
        //temp naive implementation
        floodFrom(v1,flood);
        return flood.contains(v2);
    }

    public boolean canConnect(GraphConnector<T> v1, GraphConnector<T> v2){
        return true;
    }
    public void onVertexAdded(GraphConnector<T> vertex){}
    public void addVertex(GraphConnector<T> vertex){
        vertexes.add(vertex);
        vertex.graph = (T)this;
        onVertexAdded(vertex);
        onGraphChanged();
    }

    public void mergeGraph(Graph<T> graph){
        onMergeBegin((T)graph);
        for(var vertex : graph.vertexes){
            addVertex(vertex);
        }
        for(var edge : graph.edges){
            edges.put(edge.key,edge.value);
        }
    }

    public void onGraphChanged(){}

    public void update(){
        if( Core.graphics.getFrameId() == lastFrameUpdated){
            return;
        }
        lastFrameUpdated = Core.graphics.getFrameId();
        onUpdate();
    }
    public abstract void onUpdate();

    public int size(){return vertexes.size;}

    public void each(Cons<GraphConnector<T>> cons){
        vertexes.each(cons);
    }
    public void eachEdge(Cons<GraphEdge> cons){
        edges.forEach((e)->cons.get(e.value));
    }
    public GraphConnector<T> randomVertex(){
        int skip = Mathf.random((int)vertexes.size);
        for(var vertex : vertexes){
            skip--;
            if(skip<0){
                return vertex;
            }
        }
        return vertexes.isEmpty()?null:vertexes.first();
    }

}
