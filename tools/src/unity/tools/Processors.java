package unity.tools;

import arc.util.*;
import unity.tools.proc.*;

import java.util.concurrent.*;

/**
 * Static class containing all processors. Call {@link #process()} to initiate asset processing.
 * @author GlennFolker
 */
public final class Processors{
    private static final Processor[] processes = {
        new OutlineRegionProcessor(),
        new UnitProcessor()
    };

    private Processors(){}

    public static void process(){
        for(var process : processes){
            Time.mark();

            ExecutorService exec = Executors.newCachedThreadPool();

            process.process(exec);
            Threads.await(exec);

            process.finish();
            Log.info("@ executed for @ms", process.getClass().getSimpleName(), Time.elapsed());
        }
    }
}
