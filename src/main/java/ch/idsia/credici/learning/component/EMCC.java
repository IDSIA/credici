package ch.idsia.credici.learning.component;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EMCC {
    private EMCCConfig config;
    private Executor executor;

    /** use a factory to create */
    EMCC(EMCCConfig config) {
        this.config = config;

        int th = Runtime.getRuntime().availableProcessors();
        int threads = config.threads <= 0 ? th : config.threads;
        
        executor = Executors.newFixedThreadPool(threads);
    }
 
    public void run() {
        
    }
}
