package unity.parts;

import arc.scene.ui.layout.*;


public abstract class ModularPartStat{
    public final String name;
    public int mergePriority = MergePriorities.DEFAULT;
    public int mergePostPriority = MergePriorities.DEFAULT_POST;

    public ModularPartStat(String name){
        this.name = name;
    }

    public abstract void merge(ModularPartStatMap id, ModularPart part);

    public void mergePost(ModularPartStatMap id, ModularPart part){}

    public void display(Table e){}


    public static final class MergePriorities{
        public static final int UNIT_ENGINE = 0, UNIT_ENGINE_POST = 0;
        public static final int UNIT_POWER_CONS = 4, UNIT_POWER_CONS_POST = 4;
        public static final int DEFAULT = 10, DEFAULT_POST = 99;
    }
}
