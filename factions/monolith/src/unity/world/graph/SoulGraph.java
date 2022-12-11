package unity.world.graph;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import unity.gen.graph.*;
import unity.world.graph.*;
import unity.world.graph.connectors.GraphConnectorType.*;
import unity.world.graph.nodes.GraphNodeType.*;
import unity.world.graph.nodes.SoulNodeType.*;

/** @author GlennFolker */
@SuppressWarnings("unchecked")
public class SoulGraph extends Graph<SoulGraph> implements SoulGraphI<SoulGraph>{
    protected OrderedSet<GraphNode<SoulGraph>> setA = new OrderedSet<>(), setB = new OrderedSet<>();

    @Override
    public SoulGraph copy(){
        return new SoulGraph();
    }

    @Override
    public int type(){
        return Graphs.soul;
    }

    @Override
    public void onUpdate(){
        setA.clear();
        setB.clear();
        setA.addAll(nodes);

        OrderedSet<GraphNode<SoulGraph>> nodes = setA, fill = setB;
        while(!nodes.isEmpty()){
            for(var node : nodes){
                SoulNode n = node.as();
                if(!n.canTransfer()) continue;

                int total = n.transferCount();
                if(total == 0) continue;

                float resist = n.resistance() * Time.delta;
                float transferAmount = Math.min(
                    n.amount - resist - n.consumption(),
                    n.maxThroughput() * n.build().delta() - n.transferred
                );

                if(transferAmount / total <= 0.001f) continue;
                for(var v : n.connectors){
                    int count = n.transferCount(v);
                    if(count == 0) continue;

                    float amount = transferAmount / count;
                    for(var e : v.connections){
                        SoulNode dst = e.n1.node();
                        if(e.n2 != v || !dst.canReceive()) continue;

                        fill.add(dst);
                        dst.amount += amount;
                    }
                }

                n.amount -= transferAmount + resist;
                n.transferred += transferAmount;
            }
            nodes.clear();

            var tmp = nodes;
            nodes = fill;
            fill = tmp;
        }
    }

    // All connectors in a node should share the same graph, and the graph shouldn't split.
    @Override
    public void addVertex(GraphConnector<SoulGraph> vertex){
        var conns = vertex.<SoulNode>node().connectors;
        var target = conns.isEmpty() ? this : (SoulGraph)conns.first().graph();

        target.vertices.add(vertex);
        vertex.graph = target;

        target.onVertexAdded(vertex);
        target.onGraphChanged();
    }

    @Override
    public void removeVertex(GraphConnector<SoulGraph> vertex){
        if(vertex.graph != this) return;

        var ngraph = createFromThis();
        for(var v : vertex.<SoulNode>node().connectors){
            for(int i = 0, len = v.connections.size; i < len; i++){
                removeEdge(v.connections.first());
            }

            ngraph.vertices.add(v);
            vertices.remove(v);

            v.graph = (SoulGraph)ngraph;
            ngraph.onVertexAdded(v);
            ngraph.onGraphChanged();
        }

        onGraphChanged();
    }

    @Override
    public void removeEdge(GraphEdge<SoulGraph> edge){
        removeEdgeNonSplit(edge);
        onGraphChanged();
    }

    @Override
    public void mergeGraph(Graph<SoulGraph> graph){
        if(!graph.authoritative && !authoritative){
            onMergeBegin((SoulGraph)graph);
        }else{
            if(graph.authoritative){
                graph.authoritativeOverride(this);
                authoritative = true;
                authoritativeUntil = graph.authoritativeUntil;
            }
        }

        for(var vertex : graph.vertices) super.addVertex(vertex);
        for(var edge : graph.edges) edges.put(edge.key, edge.value);
    }
}
