package unity.world.graph;

import arc.struct.*;
import mindustry.type.*;

public class CrucibleGraph extends Graph<CrucibleGraph>{
    //probably have each crucible store their own shit, then the graph distributes it as usual
    //abstract guass siedel graph?
    //public float totalCapacity;
    //ObjectMap<Item,CrucibleFluid> fluids = new ObjectMap<>();

    @Override
    public <U extends Graph<CrucibleGraph>> U copy(){
        CrucibleGraph crucibleGraph = new CrucibleGraph();
        return (U)crucibleGraph;
    }

    @Override
    public void onMergeBegin(CrucibleGraph g){ }

    @Override
    public void authoritativeOverride(CrucibleGraph g){ }

    @Override
    public void onUpdate(){
        for(GraphConnector<CrucibleGraph> v : vertexes){
            CrucibleGraphNode cgn = (CrucibleGraphNode)v.getNode();
            for(var fluid: cgn.fluids){
                fluid.value.meltedBuffer = fluid.value.melted;
            }
        }
        for(GraphConnector<CrucibleGraph> v : vertexes){
           CrucibleGraphNode cgn = (CrucibleGraphNode)v.getNode();
            for(GraphEdge ge : v.connections){
                CrucibleGraphNode cgno = (CrucibleGraphNode)ge.other(v).getNode();
                float transfer = 0;
                for(var fluid: cgn.fluids){
                    transfer = cgn.getFluid(fluid.key).total() - cgno.getFluid(fluid.key).total();
                    if(transfer>0){
                        transfer = Math.min(transfer, cgn.getFluid(fluid.key).melted);
                    }else{
                        transfer = Math.min(transfer, cgno.getFluid(fluid.key).melted);
                    }
                    transfer *= 0.1f;
                    cgno.getFluid(fluid.key).meltedBuffer += transfer;
                    cgn.getFluid(fluid.key).meltedBuffer -= transfer;
                }
            }
        }
        for(GraphConnector<CrucibleGraph> v : vertexes){
            CrucibleGraphNode cgn = (CrucibleGraphNode)v.getNode();
            for(var fluid: cgn.fluids){
                fluid.value.melted = fluid.value.meltedBuffer;
            }
        }
    }


    public static class CrucibleFluid{
        Item item;
        public float melted;
        public float meltedBuffer;//?
        public float solid;

        public CrucibleFluid(Item item){
            this.item = item;
        }

        public float total(){
            return solid+melted;
        }
        public float meltedRatio(){
            return melted/total();
        }

        public Item getItem(){
            return item;
        }

        public void melt(float t){
            solid-=t;
            melted+=t;
        }
    }

    @Override
    public boolean isRoot(GraphConnector<CrucibleGraph> t){
       return true;
    }

}
