package unity.world.graph;

import arc.util.*;
import mindustry.type.*;
import unity.world.meta.CrucibleRecipes.*;

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
           if(cgn.accessConnector!=null && v!=cgn.accessConnector){
               continue;
           }
            float transfer = 0;
            for(GraphEdge ge : v.connections){
                CrucibleGraphNode cgno = (CrucibleGraphNode)ge.other(v).getNode();
                if(cgno.accessConnector!=null && ge.other(v)!=cgno.accessConnector){
                   continue;
                }
                for(var fluid: cgn.fluids){
                    var otherfluid = cgno.getFluid(fluid.key);
                    var thisfluid = cgn.getFluid(fluid.key);
                    transfer = thisfluid.total()/cgn.baseSize - otherfluid.total()/cgno.baseSize;
                    if(transfer>0.1f){
                        transfer *= 0.3f* Time.delta;
                        transfer = Math.min(transfer, Math.min(thisfluid.melted,cgno.capacity-otherfluid.total()));
                    }else{
                        continue;
                    }

                    otherfluid.melted += transfer;
                    thisfluid.melted -= transfer;
                }
            }
        }
    }


    public static class CrucibleFluid{
        CrucibleIngredient ingredient;
        public float melted;
        public float meltedBuffer;//?
        public float solid;

        public CrucibleFluid(CrucibleIngredient item){
            this.ingredient = item;
        }

        public float total(){
            return solid+melted;
        }
        public float meltedRatio(){
            return melted/total();
        }

        public CrucibleIngredient getIngredient(){
            return ingredient;
        }

        public Item getItem(){
            if(ingredient instanceof CrucibleItem ci){
                return ci.item;
            }
            return null;
        }

        public void melt(float t){
            solid-=t;
            melted+=t;
        }
        public void vapourise(float t){
            melted-=t;
            if(melted<0){
                melted = 0;
            }
        }
    }

    @Override
    public boolean isRoot(GraphConnector<CrucibleGraph> t){
       return true;
    }

}
