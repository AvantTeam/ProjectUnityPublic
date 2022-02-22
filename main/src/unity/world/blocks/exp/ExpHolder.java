package unity.world.blocks.exp;

import mindustry.gen.*;

public interface ExpHolder {
    int getExp();
    int handleExp(int amount);

    default int unloadExp(int amount){
        return 0;
    }

    default boolean handleOrb(int orbExp){
        return handleExp(orbExp) > 0;
    }

    default boolean acceptOrb(){
        return false;
    }

    default int handleTower(int amount, float angle){
        return handleExp(amount);
    }

    default boolean hubbable(){
        return false;
    }

    default boolean canHub(Building build){
        return false;
    }

    default void setHub(ExpHub.ExpHubBuild build){}
}
