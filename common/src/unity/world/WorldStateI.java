package unity.world;

import arc.util.io.*;

public interface WorldStateI{
    void add(WorldModule mod);
    <T extends WorldModule> T remove(Class<T> type);
    <T extends WorldModule> T get(Class<T> type);

    void write(Writes write);
    void read(Reads read);

    interface WorldModule{
        void write(Writes write);
        void read(Reads read, byte revision);

        byte revision();
    }
}
