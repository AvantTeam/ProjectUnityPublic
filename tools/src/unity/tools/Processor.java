package unity.tools;

import arc.util.*;

import java.util.concurrent.*;

/**
 * Base processor interface, used to submit asynchronous threads to process assets.
 * @author GlennFolker
 */
public interface Processor{
    /**
     * Submit processing threads here. A call to {@link #submit(ExecutorService, String, UnsafeRunnable)} should be used to
     * submit the threads to get a pleasant and proper error message in case any of the threads encountered an uncaught
     * exception.
     * @param exec The executor service for submitting threads.
     */
    void process(ExecutorService exec);

    /** Called after all processing threads are finished. */
    default void finish(){}

    default void submit(ExecutorService exec, String name, UnsafeRunnable run){
        exec.submit(() -> {
            try{
                run.run();
            }catch(Throwable t){
                String msg = Strings.getFinalMessage(t);
                Log.err("@: @", name, msg != null ? msg : Strings.getFinalCause(t).toString());
            }
        });
    }
}
