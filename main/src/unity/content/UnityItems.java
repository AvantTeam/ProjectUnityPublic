package unity.content;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.type.*;
import unity.graphics.*;

public class UnityItems{
    public static Item
    //faction-items
    //koruh
    stone, denseAlloy, steel, dirium, uranium,
    //youngcha
    nickel, cupronickel, superAlloy,sulfur;

    public static void load(){
        //region faction-alloys

        stone = new Item("stone", Color.valueOf("8a8a8a")){{
            hardness = 1;
            cost = 0.4f;
            lowPriority = true;
        }};

        denseAlloy = new Item("dense-alloy", Color.valueOf("a68a84")){{
            hardness = 2;
            cost = 2f;
        }};

        steel = new Item("steel", Color.valueOf("e1e3ed")){{
            hardness = 4;
            cost = 2.5f;
        }};

        dirium = new Item("dirium", Color.valueOf("96f7c3")){{
            cost = 0.3f;
            hardness = 9;
        }};

        uranium = new Item("uranium", Color.valueOf("ace284")){{
            cost = 2f;
            hardness = 3;
            radioactivity = 1f;
        }};

        //youngcha

        cupronickel = new Item("cupronickel", Color.valueOf("a19975")){{
            cost = 2.5f;
        }};

        superAlloy = new Item("super-alloy", Color.valueOf("67a8a0")){{
            cost = 30f; // ridiculously high cost to be justified later
        }};

        nickel = new Item("nickel", Color.valueOf("6e9675")){{
            hardness = 3;
            cost = 1.5f;
        }};

        sulfur = new Item("sulfur", Color.valueOf("ffbb2b")){{
            hardness = 1;
            cost = 1.0f;
        }};


        //endregion
        //region meta
        /*
        MeltInfo meltCopper = new MeltInfo(Items.copper, 750f, 0.1f, 0.02f, 2100f, 1);
        MeltInfo meltLead = new MeltInfo(Items.lead, 570f, 0.2f, 0.02f, 1900f, 1);
        MeltInfo meltTitanium = new MeltInfo(Items.titanium, 1600f, 0.07f, 1);
        MeltInfo meltSand = new MeltInfo(Items.sand, 1000f, 0.25f, 1);
        MeltInfo carbon = new MeltInfo("carbon", 4000f, 0.01f, 0.01f, 600f, 0);
        new MeltInfo(Items.coal, carbon, 0.5f, 0, true);
        new MeltInfo(Items.graphite, carbon, 1f, 0, true);
        MeltInfo meltNickel = new MeltInfo(nickel, 1100f, 0.15f, 1);
        MeltInfo meltCuproNickel = new MeltInfo(cupronickel, 850f, 0.05f, 2);
        MeltInfo meltMetaglass = new MeltInfo(Items.metaglass, 950f, 0.05f, 2);
        MeltInfo meltSilicon = new MeltInfo(Items.silicon, 900f, 0.2f, 2);
        MeltInfo meltSurgeAlloy = new MeltInfo(Items.surgeAlloy, 1500f, 0.05f, 3);
        MeltInfo meltThorium = new MeltInfo(Items.thorium, 1650f, 0.03f, 1);
        MeltInfo meltSuperAlloy = new MeltInfo(superAlloy, 1800f, 0.02f, 4);

        new CrucibleRecipe(meltCuproNickel, 0.6f, new InputRecipe(meltNickel, 0.8f, false), new InputRecipe(meltCopper, 2f));
        new CrucibleRecipe(meltSilicon, 0.25f, new InputRecipe(meltSand, 1.25f), new InputRecipe(carbon, 0.25f, false));
        new CrucibleRecipe(meltMetaglass, 0.5f, new InputRecipe(meltSand, 1f / 3f), new InputRecipe(meltLead, 1f / 3f));
        new CrucibleRecipe(meltSurgeAlloy, 0.25f, new InputRecipe(meltSilicon, 1f), new InputRecipe(meltLead, 2f), new InputRecipe(meltCopper, 1f), new InputRecipe(meltTitanium, 1.5f));
        new CrucibleRecipe(meltSuperAlloy, 0.2f, new InputRecipe(meltCuproNickel, 1f), new InputRecipe(meltSilicon, 1f), new InputRecipe(meltThorium, 1f), new InputRecipe(meltTitanium, 1f));
        */
        //endregion
    }
}
