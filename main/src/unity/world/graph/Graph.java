package unity.world.graph;

import arc.*;
import arc.struct.*;

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
    public void removeVertex(GraphConnector<T> vertex){
        if(vertex.getGraph()!=this){return;}

        vertex.graph.vertexes.remove(vertex); // vertex.graph. bc the graph can change while edges are being removed.
        vertex.graph = null;
        onGraphChanged();
    }
    protected OrderedSet<GraphConnector<T>> floodTemp = new OrderedSet<>();
    public void removeEdge(GraphEdge edge){
        edges.remove(edge.id);
        if(!isConnected(edge.n1,edge.n2,floodTemp)){
            //OHNO
            if(floodTemp.size<= vertexes.size-floodTemp.size){
                //new graph will be the flooded area
                Graph<T> ngraph = createFromThis();
                for(GraphConnector<T> other : floodTemp){
                    this.removeVertex(other);
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

    public void floodFrom(GraphConnector<T> gc){

    }

    //returns if two vertexes are connected, if not, returns everything on v1's side of the graph.
    public boolean isConnected (GraphConnector<T> v1, GraphConnector<T> v2, OrderedSet<GraphConnector<T>> flood){

        return true;
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
}
