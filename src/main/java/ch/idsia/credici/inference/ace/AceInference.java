package ch.idsia.credici.inference.ace;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ace.AceEvalExt;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class AceInference {
    private File networkFile;
    private StructuralCausalModel model; 
    private AceEvalExt ace; 
    private String acePath; 
    private int[] exo; 


    public AceInference(String acepath) {
        this.acePath = acepath;
    }


    public File init(StructuralCausalModel network, boolean table) throws IOException {
        networkFile = File.createTempFile("CrediciAceModel", ".net");
        networkFile.deleteOnExit();
        
        Logger.getGlobal().info(networkFile.toString());

        try(NetworkWriter writer = new NetworkWriter(networkFile, "n", "s")) {
            writer.write(network.toBnet());
        }
        
        this.model = network;
        this.exo = network.getExogenousVars();

        try { 
            ace = new AceEvalExt(networkFile.getAbsolutePath(), exo, acePath, table);
            
        } catch(Exception e) {
            throw new IOException("Exception instantiating AaceEvalExt", e);
        }
        return networkFile;
    }

    public void update(StructuralCausalModel network) {
        for (int ex : network.getExogenousVars()) {
            BayesianFactor f = network.getFactor(ex);
            double[] dta = f.getData();
            ace.update_CPTs(ex, dta);
        }    
        this.dirty = true;
    }

    public void update(BayesianFactor f) {
        if (f.getDomain().getSize() != 1) 
            throw new IllegalArgumentException("only single var domains");

        int U = f.getDomain().getVariables()[0];
        ace.update_CPTs(U, f.getData());
        this.dirty = true;
    }

    private Map<Integer, BayesianFactor> factors;
    private double pe;
    private boolean dirty = true;
    private TIntIntMap evidence;

    public void dirty() {
        this.dirty = true;
    }
    
    public BayesianFactor posterior(int u) {
        return factors.get(u);
    }
    
    public Map<Integer, BayesianFactor> compute(TIntIntMap evidence) throws Exception {
        if (dirty) {
            //  ace.commitLmap();
            
            
            var res = ace.evaluate(model.getExogenousVars(), evidence);
            factors = new HashMap<>(res.size());

            for (var entry : res.entrySet()) {
                if (entry.getKey() == -1) {
                    pe = entry.getValue()[0];
                } else  {
                    BayesianFactor factor = model.getFactor(entry.getKey()).copy();
                    factor.setData(entry.getValue());
                    factors.put(entry.getKey(), factor);
                }
            }
            
            
            this.evidence = new TIntIntHashMap(evidence);
            dirty = false;
            return factors;
        } else { 
            if(!evidence.equals(this.evidence)) 
                throw new IllegalArgumentException("Update first");
            return factors;
        }
    }

   
    public double pevidence() {
        return pe;
    }




    // String stateName(int node, int state){
    //     return "s" + state;
    // }
    // String nodeName(int node) {
    //     return "n" + node;
    // }

    public double getQueryTime() { 
        return ace.getLastQueryTime();
    }
    public double getReadTime() { 
        return ace.getLastReadTime();
    }
    public double getSetupTime() { 
        return ace.getLastSetupTime();
    }
}
