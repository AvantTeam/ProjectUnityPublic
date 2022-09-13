package unity.parts;

import arc.scene.ui.layout.*;

public abstract class ModularPartStat{
    public final String name;
    public int mergePriority = 0;
    public int mergePostPriority = 999;

    public ModularPartStat(String name){
        this.name = name;
    }

    public abstract void merge(ModularPartStatMap id, ModularPart part);

    public abstract void mergePost(ModularPartStatMap id, ModularPart part);

    public void display(Table e){}
}
