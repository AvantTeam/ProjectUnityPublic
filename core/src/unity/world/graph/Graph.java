package unity.world.graph;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import unity.world.graph.connectors.GraphConnectorType.*;

/**
 * @author Xelo
 * @author GlennFolker
 */
@SuppressWarnings("unchecked")
public abstract class Graph<T extends Graph<T>> implements GraphI<T>{
    public final int id;
    private static int lastId;

    public OrderedSet<GraphConnector<T>> vertices = new OrderedSet<>();
    public LongMap<GraphEdge<T>> edges = new LongMap<>();

    protected OrderedSet<GraphConnector<T>> floodTemp = new OrderedSet<>();

    {
        id = lastId++;
    }

    public boolean authoritative = false;
    public long authoritativeUntil = 0;

    private long lastFrameUpdated;

    public abstract int type();

    @Override
    public void update(){
        if(Core.graphics.getFrameId() == lastFrameUpdated) return;
        lastFrameUpdated = Core.graphics.getFrameId();

        onUpdate();
        if(authoritative && lastFrameUpdated > authoritativeUntil) authoritative = false;
    }

    public abstract void onUpdate();

    public abstract <G extends Graph<T>> G copy();
    public <G extends Graph<T>> G createFromThis(){
        var c = copy();
        c.authoritative = authoritative;
        c.authoritativeUntil = authoritativeUntil;
        return (G)c;
    }

    public abstract void onMergeBegin(T g);
    public abstract void authoritativeOverride(T g);

    public void addEdge(GraphEdge<T> edge){
        edges.put(edge.id, edge);

        Graph<T> g = edge.other(this).graph();
        if(g != this){
            // Xelo: CONSUME THE INFERIOR GRAPH
            if(g.vertices.size < vertices.size){
                mergeGraph(g);
            }else{
                g.mergeGraph(this);
            }
        }
    }

    public void removeOnlyVertex(GraphConnector<T> vertex){
        if(vertex.graph != this){
            Log.warn("Tried to remove invalid vertex.");
            return;
        }

        vertices.remove(vertex);
        onVertexRemoved(vertex);

        vertex.graph = null;
        onGraphChanged();
    }

    public void removeVertex(GraphConnector<T> vertex){
        if(vertex.graph != this) return;
        if(vertex.connections.size == 0){
            // Xelo: trivial case 1, node has no connections anyway
            if(vertices.size > 1){
                // Xelo: somehow disconnected but still in the graph?
                Graph<T> ngraph = createFromThis();
                ngraph.addVertex(vertex);
                vertices.remove(vertex);
                onVertexRemoved(vertex);
                onGraphChanged();
            }

            return;
        }else if(vertex.connections.size == 1){
            // Xelo: trivial case 2, node has 1 connection so we can just detach node into new graph,
            removeEdgeNonSplit(vertex.connections.first());
            Graph<T> ngraph = createFromThis();
            ngraph.addVertex(vertex);
            vertices.remove(vertex);
            onVertexRemoved(vertex);
            onGraphChanged();
        }else{
            // Xelo: non trivial case, need to detach edges and check for splits.
            //       probably change to do all the edges at once hmm
            int size = vertex.connections.size;
            for(int i = 0; i < size; i++){
                if(vertex.graph != this){
                    vertex.graph.removeVertex(vertex);
                    if(vertices.contains(vertex)) throw new IllegalStateException("Graph still contains deleted vertex after splitting.");

                    return;
                }else{
                    GraphEdge vconn = vertex.connections.removeIndex(0);
                    removeEdge(vconn);
                }
            }

            if(vertices.size > 1){
                vertices.remove(vertex);
                onVertexRemoved(vertex);
            }

            onGraphChanged();
        }
    }

    public void removeEdgeNonSplit(GraphEdge<T> edge){
        edges.remove(edge.id);
        edge.n1.removeEdge(edge);
        edge.n2.removeEdge(edge);
    }

    public void removeEdge(GraphEdge<T> edge){
        removeEdgeNonSplit(edge);
        onGraphChanged();
        if(!isConnected(edge.n1.as(), edge.n2.as(), floodTemp)){
            // Xelo: OHNO
            if(floodTemp.size <= vertices.size - floodTemp.size){
                // Xelo: new graph will be the flooded area
                Graph<T> ngraph = createFromThis();
                for(GraphConnector<T> other : floodTemp){
                    removeOnlyVertex(other);
                    ngraph.addVertex(other);
                    for(GraphEdge<T> ge : other.connections){
                        if(ge != edge){
                            ngraph.edges.put(ge.id, ge);
                            edges.remove(ge.id);
                        }
                    }
                }
            }else{
                // Xelo: this graph will be the flooded area
                Graph<T> ngraph = createFromThis();
                for(GraphConnector<T> other : vertices){
                    if(!floodTemp.contains(other)){
                        ngraph.addVertex(other);
                        for(GraphEdge<T> ge: other.connections){
                            if(ge != edge){
                                ngraph.edges.put(ge.id, ge);
                                edges.remove(ge.id);
                            }
                        }
                    }
                }

                for(GraphConnector<T> other : ngraph.vertices){
                    vertices.remove(other);
                    onVertexRemoved(other);
                }
            }
        }
    }

    /** Floods the entire graph (if possible) from a point. */
    public void floodFrom(GraphConnector<T> gc, OrderedSet<GraphConnector<T>> flood){
        flood.clear();

        Seq<GraphConnector<T>> front = new Seq<>();
        front.add(gc);
        flood.add(gc);

        while(front.any()){
            var current = front.pop();
            for(GraphEdge<T> ge : current.connections){
                GraphConnector<T> next = ge.other(current);
                if(flood.contains(next)) continue;

                front.add(next);
                flood.add(next);
            }
        }
    }

    /** @return Whether two vertices are connected, if not, returns everything on v1's side of the graph. */
    public boolean isConnected(GraphConnector<T> v1, GraphConnector<T> v2, OrderedSet<GraphConnector<T>> flood){
        // Xelo: temp naive implementation
        floodFrom(v1, flood);
        return flood.contains(v2);
    }

    /** @return Whether it's allowed to join the graph. */
    public boolean canConnect(GraphConnector<T> v1, GraphConnector<T> v2){
        return true;
    }

    public void onVertexRemoved(GraphConnector<T> vertex){}
    public void onVertexAdded(GraphConnector<T> vertex){}
    public void addVertex(GraphConnector<T> vertex){
        vertices.add(vertex);
        vertex.graph = (T)this;
        onVertexAdded(vertex);
        onGraphChanged();
    }

    public void mergeGraph(Graph<T> graph){
        if(!graph.authoritative && !authoritative){
            onMergeBegin((T)graph);
        }else{
            if(graph.authoritative){
                graph.authoritativeOverride((T)this);
                authoritative = true;
                authoritativeUntil = graph.authoritativeUntil;
            }
        }

        for(var vertex : graph.vertices) addVertex(vertex);
        for(var edge : graph.edges) edges.put(edge.key, edge.value);
    }

    public void onGraphChanged(){}

    public int size(){
        return vertices.size;
    }

    public void each(Cons<GraphConnector<T>> cons){
        vertices.each(cons);
    }

    public void eachEdge(Cons<GraphEdge<T>> cons){
        for(var edge : edges.values()) cons.get(edge);
    }

    // Xelo: used for saving.
    public boolean hasVertex(GraphConnector<T> connector){
        return vertices.contains(connector);
    }

    public GraphConnector<T> firstVertex(){
        return vertices.first();
    }

    public GraphConnector<T> randomVertex(){
        int skip = Mathf.random((int)vertices.size);
        for(var vertex : vertices){
            skip--;
            if(skip < 0) return vertex;
        }

        return vertices.isEmpty() ? null : vertices.first();
    }

    public void write(Writes write){}

    public void read(Reads read){
        authoritative = true;
        authoritativeUntil = Core.graphics.getFrameId() + 1;
    }

    public boolean isRoot(GraphConnector<T> t){
        return vertices.first() == t;
    }
}
