package ch.idsia.credici.learning.component;

import ch.idsia.credici.Table;
import ch.idsia.credici.learning.inference.ComponentInference;
import ch.idsia.credici.model.StructuralCausalModel;

public class EMCCConfig {
    int threads = 0;
    int runs = 20;
    int maxIterations = 1000;

    ComponentInference inference;
    StructuralCausalModel model;
    Table data;


    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }


    public void setData(Table data) {
        this.data = data;
    }
    
    public void setInference(ComponentInference inference) {
        this.inference = inference;
    }
    
    public void setModel(StructuralCausalModel model) {
        this.model = model;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }


    /**
     * return the number of threads that should be used. If threads is less or equal to
     * zero it will return guess of a good number of threads. 
     * 
     * @return
     */
    public int getActualThreads() {
        return this.threads <= 0 ? 
             Runtime.getRuntime().availableProcessors():
             this.threads;
    }
}