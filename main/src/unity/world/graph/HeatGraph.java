package unity.world.graph;

import arc.util.*;
import mindustry.*;
import mindustry.world.meta.*;

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
        HeatGraphNode.ambientTemp = HeatGraphNode.celsiusZero+20;
        if((Vars.state.rules.env & Env.scorching) != 0){
            HeatGraphNode.ambientTemp = HeatGraphNode.celsiusZero+500;
        }else if((Vars.state.rules.env & Env.space) != 0){
            HeatGraphNode.ambientTemp = HeatGraphNode.celsiusZero;
        }


        //for each vertex distribute heat to neighbours via the gauss siedel method.
        float k = 0;
        float e = 0;
        float cond = 0;
        float b = 0;
        HeatGraphNode hgn;
        HeatGraphNode hgno;


        for(GraphConnector<HeatGraph> v : vertexes){
            ((HeatGraphNode)v.getNode()).flux=0;
        }

        int iter = 3; // convergence iterations
        for(int i = 0;i<iter;i++){
            for(GraphConnector<HeatGraph> v : vertexes){
                hgno = ((HeatGraphNode)v.node);
                k = 0;
                e = hgno.heatenergy;
                cond = hgno.conductivity;
                // my brain hurt
                //but essentially the energy only GS equality is eₙ = (e꜀ + kTₛ)/(1+k/c) as T꜀ is e꜀/c
                for(GraphEdge ge : v.connections){
                    hgn = ((HeatGraphNode)ge.other(v).node);
                    b =  (hgn.conductivity + cond) * Time.delta;
                    k += b;
                    e += b * hgn.getTemp();
                }
                hgno.energyBuffer = e / (1+k/hgno.heatcapacity);
            }
            for(GraphConnector<HeatGraph> v : vertexes){
                hgno = ((HeatGraphNode)v.node);
                hgno.flux += hgno.energyBuffer-hgno.heatenergy;
                hgno.heatenergy = hgno.energyBuffer;
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

    @Override
    public boolean isRoot(GraphConnector<HeatGraph> t){
        return true;
    }
}
