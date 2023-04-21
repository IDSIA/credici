package ch.idsia.credici;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class Tasks {
    static class X implements Runnable {
        int id; 
        X(int id) {
            this.id = id;  
        }

        public void run() {
            try {
                System.out.println("Started " + this.id);
                Thread.sleep((int)(Math.random() * 1000 + 1));
                System.out.println("Done " + this.id);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int processors = Runtime.getRuntime().availableProcessors();
        
        //System.out.println(processors);
        Executor executor = Executors.newFixedThreadPool(processors);
        for (int i = 0; i < 100; ++i) {
            executor.execute(new X(i));
        }
    }
}
