package ch.idsia.credici.inference.ace;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.math3.util.FastMath;

import ace.AceEvalExt;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;
import ch.idsia.credici.collections.FIntIntHashMap;

/**
 * Class abstracting a little the management of the AceExt class. 
 * This will hide all network and node details and offer a clean interface between {@link StructuralCausalModel} objects and 
 * the ACE compiler and Circuit evaluator.
 */
public class AceInference {
    private File networkFile;
    private StructuralCausalModel model; 
    private AceEvalExt ace; 

    private String acePath; 

    private int[] exo; 

    private Map<Integer, BayesianFactor> factors;
    
    private double pe;
    private boolean dirty = true;
    private TIntIntMap evidence;


    public AceInference(String acepath) {
        this.acePath = acepath;
    }

    public String getCircuitFileName() {
        return ace.getACFileName();
    }
    
    public File init(StructuralCausalModel network, boolean table) throws IOException {
        networkFile = File.createTempFile("CrediciAceModel", ".net");
        networkFile.deleteOnExit();
        
        //Logger.getGlobal().info(networkFile.toString());

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

    /**
     * Update the parameters of the model with new data. 
     * @param f - {@link BayesianFactor} the new parameters
     */
    public void update(BayesianFactor f) {
        if (f.getDomain().getSize() != 1) 
            throw new IllegalArgumentException("only single var domains");

        int U = f.getDomain().getVariables()[0];
        ace.update_CPTs(U, f.getData());
        this.dirty = true;
    }

    /** 
     * Mark the model as in need of computing an updating
     */
    public void dirty() {
        this.dirty = true;
    }

    /**
     * get the marginal parameters for the specified variable.
     * @param u the variable
     * @return the parameters stored in a Bayesian factor.
     */
    public BayesianFactor marginals(int u) {
        return factors.get(u);
    }

    public BayesianFactor posterior(int u) {
        BayesianFactor post = factors.get(u).copy();
        
        double[] data = post.getInteralData();

        if (post.isLog()) {
            double logpe = FastMath.log(pe);
            for (int id = 0; id < data.length; ++id) {
                data[id] -= logpe;
            }
        } else {
            for (int id = 0; id < data.length; ++id) {
                data[id] /= pe;
            }
        }
        
        return post;
    }
    
    public Map<Integer, BayesianFactor> compute(TIntIntMap evidence) throws Exception {
        if (dirty) {
            //ace.commitLmap();
            
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
            
            
            this.evidence = new FIntIntHashMap(evidence);
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



    public int getCircuitSize() {
        return ace.getCircuitSize();
    }
}
