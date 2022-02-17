package unity.world.graph;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.type.*;
import unity.ui.*;
import unity.world.graph.CrucibleGraph.*;
import unity.world.meta.*;

import static mindustry.Vars.content;


public class CrucibleGraphNode extends GraphNode<CrucibleGraph>{
    public float capacity;
    public float transferrate = 0.1f;
    public boolean doesCrafting = true;
    public float baseSize = 1;
    Color color = new Color();
    public GraphConnector<CrucibleGraph> accessConnector = null;

    public OrderedMap<Item, CrucibleFluid> fluids = new OrderedMap<>();
    public CrucibleGraphNode(GraphBuild build, float totalCapacity){
        super(build);
        this.capacity =totalCapacity;
        baseSize = Mathf.sqr(build.getBuild().block.size);
    }
    public CrucibleGraphNode(GraphBuild build, float totalCapacity, boolean crafts){
        super(build);
        this.capacity =totalCapacity;
        this.doesCrafting = crafts;
        baseSize = Mathf.sqr(build.getBuild().block.size);
    }

    @Override
    public void displayBars(Table table){
        table.row();
        ///temp
        var cell =table.add( new CrucibleDisplayElement(fluids,3)).grow();
        cell.update((element)->cell.height(element.getMinHeight()));
    }

    //returns how many items went in
    public int addItem(Item i, int amount){
        var f = getFluid(i);
        if(f.total()+amount<=capacity){
            f.solid+=amount;
            return amount;
        }else{
            float space = capacity-f.total();
            f.solid+= Mathf.floor(space);
            return Mathf.floor(space);
        }
    }

    public CrucibleFluid getFluid(Item i){
        if(!fluids.containsKey(i)){
            fluids.put(i, new CrucibleFluid(i));
        }
        return fluids.get(i);
    }
    public Color getColor(){
        return color;
    }
    public void updateColor(){
        Vec3 color = new Vec3();
        float t = 0;
        float tt;
        for(var fluid : fluids){
            tt = fluid.value.melted;
            t+=tt;
            color.add(fluid.key.color.r*tt,fluid.key.color.g*tt,fluid.key.color.b*tt);
        }
        if(t == 0){
            this.color.set(Color.clear);
            return;
        }
        color.scl(1f/t);
        this.color.set(color.x,color.y,color.z,Mathf.clamp(10f*t/capacity));
    }

    private Seq<Item> smeltOrder = new Seq<>();
    private Seq<Item> coolOrder = new Seq<>();

    @Override
    public void update(){
        var heat = this.build.heatNode();
        if( heat!= null){
            if(!doesCrafting){
                return;
            }
            smeltOrder.clear();
            coolOrder.clear();
            for(var fluid : fluids){
                if(!CrucibleRecipes.items.containsKey(fluid.key)){
                    continue;
                }
                var i = CrucibleRecipes.items.get(fluid.key);
                if(heat.getTemp()>=i.meltingpoint && fluid.value.solid>0){
                    smeltOrder.add(i.item);
                }
                if(heat.getTemp()<i.meltingpoint && fluid.value.melted>0){
                    coolOrder.add(i.item);
                }
            }
            smeltOrder.sort((a,b)-> Float.compare(CrucibleRecipes.items.get(a).meltingpoint,CrucibleRecipes.items.get(b).meltingpoint));
            coolOrder.sort((a,b)-> -Float.compare(CrucibleRecipes.items.get(a).meltingpoint,CrucibleRecipes.items.get(b).meltingpoint));

            //solidify

            for(var item:coolOrder){
                var i = CrucibleRecipes.items.get(item);
                float remaining = (i.meltingpoint-heat.getTemp())*heat.heatcapacity;
                if(remaining<=0){
                    break;
                }
                float reqSmelt = Math.min(fluids.get(item).melted, Math.max(0.1f,fluids.get(item).melted*i.meltspeed* Time.delta));
                float reqSmeltEnergy = reqSmelt*i.phaseChangeEnergy;
                float smeltRatio = Mathf.clamp(remaining/reqSmeltEnergy);
                getFluid(item).melt(-smeltRatio*reqSmelt);
                heat.addHeatEnergy(smeltRatio*reqSmeltEnergy);
            }
            //melt
            for(var item:smeltOrder){
                var i = CrucibleRecipes.items.get(item);
                float remaining = (heat.getTemp()-i.meltingpoint)*heat.heatcapacity;
                if(remaining<=0){
                    break;
                }
                float reqSmelt = Math.min(fluids.get(item).solid, Math.max(0.1f,fluids.get(item).solid*i.meltspeed* Time.delta));
                float reqSmeltEnergy = reqSmelt*i.phaseChangeEnergy;
                float smeltRatio = Mathf.clamp(remaining/reqSmeltEnergy);
                getFluid(item).melt(smeltRatio*reqSmelt);
                heat.addHeatEnergy(-smeltRatio*reqSmeltEnergy);
            }
            ///craft
            for(var recipe: CrucibleRecipes.recipes){
                if(recipe.minTemp>heat.getTemp()){
                    continue;
                }
                float maxam = capacity-getFluid(recipe.output).total();
                for(int i = 0;i<recipe.items.length;i++){
                    var fluid = getFluid(recipe.items[i].item);
                    if(fluid.total()==0){
                        maxam = 0;
                        break;
                    }
                    maxam = Math.min(maxam, (recipe.items[i].melted? fluid.melted : fluid.total())/recipe.items[i].amount);
                }

                if(maxam<=0){
                    continue;
                }
                maxam *= recipe.speed;
                for(int i = 0;i<recipe.items.length;i++){
                    var fluid = getFluid(recipe.items[i].item);
                    if(recipe.items[i].melted){
                        fluid.melted-=maxam*recipe.items[i].amount;
                    }else{
                        fluid.melted-=maxam*recipe.items[i].amount;
                        if(fluid.melted<0){
                            fluid.solid+=fluid.melted;
                            fluid.melted = 0;
                        }
                    }
                }
                getFluid(recipe.output).melted += maxam;
            }
        }
        updateColor();
    }

    @Override
    public void read(Reads read){
        int total = read.i();
        for(int i =0; i<total; i++){
            Item item = content.item(read.s());
            var fluid = new CrucibleFluid(item);
            fluid.solid = read.f();
            fluid.melted = read.f();
            fluids.put(item, fluid);
        }
    }

    @Override
    public void write(Writes write){
        int t = 0;
        for(var fluid : fluids){
            t += fluid.value.total()>0?1:0;
        }
        write.i(t);
        for(var fluid : fluids){
            if(fluid.value.total()<=0){
                continue;
            }
            write.s(fluid.key.id);
            write.f(fluid.value.solid);
            write.f(fluid.value.melted);
        }
    }
}
