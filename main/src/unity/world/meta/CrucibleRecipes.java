package unity.world.meta;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.type.*;
import unity.content.*;
import unity.content.effects.*;
import unity.world.graph.*;

public class CrucibleRecipes{
    public static IntMap<CrucibleIngredient> ingredients = new IntMap<>();
    public static ObjectMap<Item,CrucibleItem> items = new ObjectMap<>();
    public static ObjectMap<Liquid,CrucibleLiquid> liquids = new ObjectMap<>();
    public static Seq<CrucibleRecipe> recipes = new Seq<>();
    static {
        int id = 0;
        addItem(id++,Items.copper,900,0.1f,30);
        addItem(id++,Items.lead,400,0.15f, 10);
        addItem(id++,UnityItems.nickel,930,0.15f, 40);
        addItem(id++,UnityItems.cupronickel,800, 0.15f, 60);
        addItem(id++,Items.sand,1300, 0.15f, 20);
        addItem(id++,Items.metaglass,900, 0.15f, 25);
        addItem(id++,Items.silicon,1000, 0.15f, 60);
        addItem(id++,Items.titanium,1750, 0.15f, 60);
        addItem(id++,Items.plastanium,400, 0.15f, 30);
        addItem(id++,Items.thorium,1750, 0.15f, 30);
        addItem(id++,Items.surgeAlloy,1500, 0.15f, 120);
        addItem(id++,UnityItems.superAlloy,1800, 0.15f, 200);
        addItem(id++,Items.coal,-(HeatGraphNode.celsiusZero+1), 0.15f, 220);
        addItem(id++,Items.graphite,-(HeatGraphNode.celsiusZero+1), 0.15f, 220);
        addItem(id++,Items.pyratite,500, 0.15f, 220);
        addLiquid(id++,Liquids.water,0,0.05f,100,0.05f,450);
        addLiquid(id++,Liquids.cryofluid,-200,0.1f,-190,0.1f,50);
        addLiquid(id++,Liquids.slag,400,0.1f,2200,0.1f,60);
        addLiquid(id++,Liquids.oil,-150,0.1f,350,0.1f,120);
        addItem(id++,UnityItems.stone,-(HeatGraphNode.celsiusZero+1), 0.1f, 220);
        addItem(id++,UnityItems.denseAlloy,900, 0.15f, 120);
        addItem(id++,UnityItems.steel,1500, 0.15f, 50);
        addItem(id++,UnityItems.dirium,2500, 0.1f, 520);
        addItem(id++,UnityItems.uranium,1500, 0.1f, 80);
        addLiquid(id++,UnityLiquids.lava,1300,0.1f,3250,0.1f,300);
        addLiquid(id++,Liquids.neoplasm,-10,0.1f,850,0.01f,100);

        recipes.add(new CrucibleRecipe(items.get(UnityItems.cupronickel),0.1f,HeatGraphNode.celsiusZero + 150,
            needs(Items.copper, 2,true),
                   needs(UnityItems.nickel, 1,true)));
        recipes.add(new CrucibleRecipe(items.get(Items.metaglass), 0.1f,HeatGraphNode.celsiusZero + 950,
            needs(Items.lead, 0.5f,true),
                   needs(Items.sand, 0.5f,false)));
        recipes.add(new CrucibleRecipe(items.get(Items.silicon), 0.15f,HeatGraphNode.celsiusZero + 1350,
            needs(Items.coal, 0.5f,false),
                   needs(Items.sand, 0.5f,true)));
        recipes.add(new CrucibleRecipe(items.get(Items.silicon), 0.15f,HeatGraphNode.celsiusZero + 1500,
            needs(Items.graphite, 0.25f,false),
                   needs(Items.sand, 0.5f,true)));
        recipes.add(new CrucibleRecipe(items.get(Items.graphite), 0.02f,HeatGraphNode.celsiusZero + 2000,
            needs(Items.coal, 0.5f,false))); // super coal-efficient graphite, if you can get these temps.
        recipes.add(new CrucibleRecipe(items.get(Items.surgeAlloy),0.1f,HeatGraphNode.celsiusZero + 1200,
            needs(Items.copper, 2,true),
                   needs(Items.lead, 2,true),
                   needs(Items.titanium, 2,true),
                   needs(Items.silicon, 2,true)));
        recipes.add(new CrucibleRecipe(items.get(UnityItems.superAlloy),0.1f,HeatGraphNode.celsiusZero + 2000,
            needs(UnityItems.cupronickel, 3,true),
                   needs(Items.surgeAlloy, 2,true),
                   needs(Items.pyratite, 2,false)));
        ///delibratly very slow,, perhaps some casual time manipulation can speed it up?
        recipes.add(new CrucibleRecipe(items.get(UnityItems.dirium),0.0002f,HeatGraphNode.celsiusZero + 2000,
            needs(UnityItems.uranium, 1,true),
                   needs(UnityItems.steel, 1,true),
                   needs(Items.titanium, 1,true),
                   needs(Items.pyratite, 2,false)));
        recipes.add(new CrucibleRecipe(items.get(UnityItems.denseAlloy),0.02f,HeatGraphNode.celsiusZero + 1500,
            needs(UnityLiquids.lava, 0.5f,true),
                   needs(Items.copper, 1,true),
                   needs(Items.lead, 2,true)));
        recipes.add(new CrucibleRecipe(items.get(UnityItems.steel),0.003f,HeatGraphNode.celsiusZero + 1500,
            needs(Items.graphite, 1,false),
                   needs(UnityItems.denseAlloy, 2,true)));
        recipes.add(new CrucibleRecipe(liquids.get(UnityLiquids.lava),0.05f,HeatGraphNode.celsiusZero + 1500,
            needs(UnityItems.stone, 1,false)));
        recipes.add(new CrucibleRecipe(items.get(UnityItems.stone),1f,0,
            needs(UnityLiquids.lava, 1,false,true)));
    }

    public static void addItem(int id, Item item,float meltpointCelsius, float meltspeed, float energy){
        items.put(item,new CrucibleItem(id,item, HeatGraphNode.celsiusZero + meltpointCelsius, meltspeed, energy));
        ingredients.put(id,items.get(item));
    }
    public static void addLiquid(int id, Liquid liquid,float meltpointCelsius, float meltspeed,float boilingpointCelsius, float boilingspeed, float energy){
        liquids.put(liquid,new CrucibleLiquid(id,liquid, HeatGraphNode.celsiusZero + meltpointCelsius, meltspeed, HeatGraphNode.celsiusZero + boilingpointCelsius,boilingspeed,energy));
        ingredients.put(id,liquids.get(liquid));
    }

    public static class CrucibleIngredient{
        public TextureRegion icon;
        public String name;
        public Color color = Color.pink;
        public int id;
        public float meltingpoint=-1;
        public float meltspeed;
        public float phaseChangeEnergy=0;
        public float boilpoint=-1;
        public float boilspeed;

        public CrucibleIngredient(TextureRegion icon, String name, int id){
            this.icon = icon;
            this.name = name;
            this.id = id;
        }
        public void onVapourise(CrucibleGraphNode cgn, float am){}
        public void onMelt(CrucibleGraphNode cgn, float am){}
        public void onSolidify(CrucibleGraphNode cgn, float am){}
        public void onTemperature(CrucibleGraphNode cgn, float temp){}
    }
    public static class CrucibleItem extends CrucibleIngredient{
        public Item item;

        public CrucibleItem(int id, Item item, float meltingpoint, float meltspeed, float phaseChangeEnergy){
            super(item.fullIcon,item.name,id);
            this.item = item;
            this.meltingpoint = meltingpoint;
            this.meltspeed = meltspeed;
            this.phaseChangeEnergy = phaseChangeEnergy;
            color = item.color;
        }
    }
    public static class CrucibleLiquid extends CrucibleIngredient{
        public Liquid liquid;

        public CrucibleLiquid(int id, Liquid liquid, float meltingpoint, float meltspeed, float boilingpoint, float boilingspeed, float phaseChangeEnergy){
            super(liquid.fullIcon,liquid.name,id);
            this.liquid = liquid;
            this.meltingpoint = meltingpoint;
            this.meltspeed = meltspeed;
            this.boilpoint = boilingpoint;
            this.boilspeed = boilingspeed;
            this.phaseChangeEnergy = phaseChangeEnergy;
            color = liquid.color;
        }

        @Override
        public void onVapourise(CrucibleGraphNode cgn, float am){
            if(liquid.flammability>0.5){
                if(Mathf.random()<am && Math.random()>0.9){
                    Fires.create(cgn.build().tile);
                }
                if(Mathf.random(3)<am && Math.random()>0.97){
                    Bullets.fireball.createNet(Team.derelict, cgn.build().x, cgn.build().y, Mathf.random(360f), -1f, 1, 1);
                }
            }
            if(liquid.explosiveness>0.5){
                cgn.build().damage(am*liquid.explosiveness);
            }
            if(liquid.temperature<=0.6){
                float s = cgn.block().size*2.5f;
                if(Mathf.random()<am && Math.random()>0.3){
                    OtherFx.steamSlow.at(cgn.build().x + Mathf.range(s), cgn.build().y + Mathf.range(s));
                    Fx.bubble.at(cgn.build().x + Mathf.range(s), cgn.build().y + Mathf.range(s),Color.white);
                }
            }
        }
    }

    public static class RecipeIngredient{
        public CrucibleIngredient ingredient;
        public float amount;
        public boolean melted;
        public boolean requiresSolid;

        public RecipeIngredient(CrucibleIngredient item, float amount, boolean melted){
            this.ingredient = item;
            this.amount = amount;
            this.melted = melted;
        }
        public RecipeIngredient(CrucibleIngredient item, float amount, boolean melted,boolean requiresSolid){
            this.ingredient = item;
            this.amount = amount;
            this.melted = melted;
            this.requiresSolid=requiresSolid;
        }
    }
    static RecipeIngredient needs(Item item, float amount, boolean melted){
        return new RecipeIngredient(items.get(item),amount,melted);
    }
    static RecipeIngredient needs(Item item, float amount, boolean melted,boolean solid){
         return new RecipeIngredient(items.get(item),amount,melted,solid);
     }
    static RecipeIngredient needs(Liquid liquid, float amount, boolean melted){
         return new RecipeIngredient(liquids.get(liquid),amount,melted);
     }
    static RecipeIngredient needs(Liquid liquid, float amount, boolean melted,boolean solid){
        return new RecipeIngredient(liquids.get(liquid),amount,melted,solid);
    }

    public static class CrucibleRecipe{
        public RecipeIngredient items[];
        public CrucibleIngredient output;
        public float minTemp = 0;
        public float speed = 0.1f;

        //most would be melted items i think
        CrucibleRecipe(CrucibleIngredient output, float speed, float minTemp,RecipeIngredient... items){
            this.output=output;
            this.items = items;
            this.speed=speed;
            this.minTemp=minTemp;
        }
    }
    public static ItemStack[] with(Object... items){
        var stacks = new ItemStack[items.length / 2];
        for(int i = 0; i < items.length; i += 2){
            stacks[i / 2] = new ItemStack((Item)items[i], ((Number)items[i + 1]).intValue());
        }
        return stacks;
    }
}
