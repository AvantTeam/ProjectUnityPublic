package unity.world.graph;

public class HeatGraph extends Graph<HeatGraph>{

    @Override
    public HeatGraph copy(){
        return new HeatGraph();
    }
    @Override
    public void onMergeBegin(HeatGraph g){}

    @Override
    public void authoritativeOverride(HeatGraph g){ }

    @Override
    public void onUpdate(){
        //for each vertex distribute heat to neighbours via the gauss siedel method.
        float k = 0;
        float t = 0;
        float cond = 0;
        float b = 0;
        HeatGraphNode hgn;
        HeatGraphNode hgno;
        int iter = 3; // convergence iterations
        for(int i = 0;i<iter;i++){
            for(GraphConnector<HeatGraph> v : vertexes){
                hgno = ((HeatGraphNode)v.node);
                k = 1;
                t = hgno.temp;
                cond = hgno.conductivity;
                //
                for(GraphEdge ge : v.connections){
                    hgn = ((HeatGraphNode)ge.other(v).node);
                    b = 0.5f * (hgn.conductivity + cond);
                    k += b;
                    t += b * hgn.temp;
                }
                hgno.tempBuffer = t / k;
            }
            if(i!=iter-1){
                for(GraphConnector<HeatGraph> v : vertexes){
                    hgno = ((HeatGraphNode)v.node);
                    hgno.temp = hgno.tempBuffer;
                }
            }
        }
    }

    @Override //more like disconnect vertex
    public void removeVertex(GraphConnector<HeatGraph> vertex){
        int s = vertex.connections.size;
        for(int i = 0;i<s;i++){
            removeEdge(vertex.connections.first());
        }
        var ngraph  = createFromThis();
        ngraph.addVertex(vertex);
        vertexes.remove(vertex);
        onGraphChanged();
    }

    @Override
    public void removeEdge(GraphEdge edge){
        removeEdgeNonSplit(edge);
        //no graph splitting.
    }
}
