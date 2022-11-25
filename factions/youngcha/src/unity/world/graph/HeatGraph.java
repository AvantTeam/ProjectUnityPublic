package unity.world.graph;

import arc.util.*;
import mindustry.*;
import mindustry.world.meta.*;
import unity.gen.graph.*;
import unity.world.graph.connectors.GraphConnectorType.*;
import unity.world.graph.nodes.*;
import unity.world.graph.nodes.HeatNodeType.*;

@SuppressWarnings("unchecked")
public class HeatGraph extends Graph<HeatGraph> implements HeatGraphI<HeatGraph>{
    @Override
    public HeatGraph copy(){
        return new HeatGraph();
    }

    @Override
    public int type(){
        return Graphs.heat;
    }

    @Override
    public void onMergeBegin(HeatGraph g){}

    @Override
    public void authoritativeOverride(HeatGraph g){}

    @Override
    public void onUpdate(){
        HeatNodeType.ambientTemp = HeatNodeType.celsiusZero + 20f;
        if((Vars.state.rules.env & Env.scorching) != 0){
            HeatNodeType.ambientTemp = HeatNodeType.celsiusZero + 500f;
        }else if((Vars.state.rules.env & Env.space) != 0){
            HeatNodeType.ambientTemp = HeatNodeType.celsiusZero;
        }

        // Xelo: for each vertex distribute heat to neighbours via the gauss siedel method.
        float k = 0;
        float e = 0;
        float cond = 0;
        float b = 0;
        HeatNode hgn;
        HeatNode hgno;

        for(GraphConnector<HeatGraph> v : vertices){
            ((HeatNode)v.getNode()).flux=0;
        }

        int iter = 3; // convergence iterations
        for(int i = 0;i<iter;i++){
            for(GraphConnector<HeatGraph> v : vertices){
                hgno = ((HeatNode)v.node);
                k = 0;
                e = hgno.heatEnergy;
                cond = hgno.conductivity;
                // Xelo: my brain hurt
                //       but essentially the energy only GS equality is eₙ = (e꜀ + kTₛ)/(1+k/c) as T꜀ is e꜀/c
                for(GraphEdge<HeatGraph> ge : v.connections){
                    hgn = ((HeatNode)ge.other(v).node);
                    b =  (hgn.conductivity + cond) * Time.delta;
                    k += b;
                    e += b * hgn.getTemp();
                }

                hgno.energyBuffer = e / (1f + k / hgno.heatCapacity);
            }

            for(GraphConnector<HeatGraph> v : vertices){
                hgno = ((HeatNode)v.node);
                hgno.flux += hgno.energyBuffer-hgno.heatEnergy;
                hgno.heatEnergy = hgno.energyBuffer;
            }
        }
    }

    @Override //more like disconnect vertex
    public void removeVertex(GraphConnector<HeatGraph> vertex){
        int s = vertex.connections.size;
        for(int i = 0; i < s; i++) removeEdge(vertex.connections.first());

        var ngraph  = createFromThis();
        ngraph.addVertex(vertex);
        vertices.remove(vertex);
        onGraphChanged();
    }

    @Override
    public void removeEdge(GraphEdge<HeatGraph> edge){
        removeEdgeNonSplit(edge);
        //no graph splitting.
    }

    @Override
    public boolean isRoot(GraphConnector<HeatGraph> t){
        return true;
    }
}
