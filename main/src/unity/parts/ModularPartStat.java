package unity.parts;

public abstract class ModularPartStat{
    public String name;

    public ModularPartStat(String name){
        this.name = name;
    }

    public abstract void merge(ModularPartStatMap id, ModularPart part);

    public abstract void mergePost(ModularPartStatMap id, ModularPart part);


}
