package unity.mod;

public interface DevBuild{
    default void setup(){}

    default void init(){}

    default boolean isDev(){
        return false;
    }
}
