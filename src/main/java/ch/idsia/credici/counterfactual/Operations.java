package ch.idsia.credici.counterfactual;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Operations {

    /**
     * Merge the a SCM with other equivalent ones to create a counterfactual model.
     * @param models
     * @return
     */
    public static StructuralCausalModel merge(StructuralCausalModel reality, StructuralCausalModel... models) {

        //check that the variables are the same
        for(StructuralCausalModel m : models){
            if (!Arrays.equals(reality.getExogenousVars(), m.getExogenousVars()) ||
                    !Arrays.equals(reality.getEndogenousVars(), m.getEndogenousVars()))
                throw new IllegalArgumentException("Error: models cannot be merged");
        }

        //Indexes of variables should be consecutive
        if(reality.getVariables().length != Ints.max(reality.getVariables())+1){
            throw new IllegalArgumentException("Indexes of variables must be consecutive");
        }

        // get the new variables
        int[] merged_vars = IntStream.range(0, reality.getVariables().length + models.length*reality.getEndogenousVars().length).toArray();

        //counterfactual mapping
        WorldMapping map = new WorldMapping(merged_vars);

        // add variables of world 0 (reality)
        StructuralCausalModel merged = reality.copy();
        IntStream.of(reality.getEndogenousVars())
                .forEach(v->map.set(v,0,v));
        IntStream.of(reality.getExogenousVars())
                .forEach(v->map.set(v, WorldMapping.ALL,v));


        int w = 1;
        for(StructuralCausalModel m: models){
            // Add all the endogenous variables
            for(int x_0: m.getEndogenousVars()) {
                int x_w = merged.addVariable(m.getSize(x_0));
                map.set(x_w,w,x_0);
            }
            // add the arcs
            for(int x_0: m.getEndogenousVars()) {
                int x_w = map.getEquivalentVars(w,x_0);
                for(int pa_0: m.getParents(x_0)) {
                    int pa_w = pa_0;
                    if(m.isEndogenous(pa_0))
                        pa_w = map.getEquivalentVars(w, pa_0);;
                    merged.addParent(x_w, pa_w);
                }
                // Set the factor with the new domain
                BayesianFactor f = m.getFactor(x_0);
                f = f.renameDomain(map.getEquivalentVars(w,f.getDomain().getVariables()));
                merged.setFactor(x_w, f);
            }
            w++;
        }

        // set the map
        map.setModel(merged);

        return merged;

    }


    /**
     * Merge the a SCM with other equivalent ones to create a counterfactual model.
     * @param models
     * @return
     */
    public static SparseModel merge(SparseModel reality, SparseModel... models) {

        //check that the variables are the same
        for(SparseModel m : models){
            if (!Arrays.equals(CausalInfo.of(reality).getExogenousVars(), CausalInfo.of(m).getExogenousVars()) ||
                    !Arrays.equals(CausalInfo.of(reality).getEndogenousVars(), CausalInfo.of(m).getEndogenousVars()))
                throw new IllegalArgumentException("Error: models cannot be merged");
        }

        //Indexes of variables should be consecutive
        if(reality.getVariables().length != Ints.max(reality.getVariables())+1){
            throw new IllegalArgumentException("Indexes of variables must be consecutive");
        }

        // get the new variables
        int[] merged_vars = IntStream.range(0,
                reality.getVariables().length + models.length * CausalInfo.of(reality).getEndogenousVars().length)
                .toArray();

        //counterfactual mapping
        WorldMapping map = new WorldMapping(merged_vars);

        // add variables of world 0 (reality)
        SparseModel merged = (SparseModel) reality.copy();
        IntStream.of(CausalInfo.of(reality).getEndogenousVars())
                .forEach(v->map.set(v,0,v));
        IntStream.of(CausalInfo.of(reality).getExogenousVars())
                .forEach(v->map.set(v, WorldMapping.ALL,v));


        int w = 1;
        for(SparseModel m: models){
            // Add all the endogenous variables
            for(int x_0: CausalInfo.of(m).getEndogenousVars()) {
                int x_w = merged.addVariable(m.getSize(x_0));
                map.set(x_w,w,x_0);
            }
            // add the arcs
            for(int x_0:  CausalInfo.of(m).getEndogenousVars()) {
                int x_w = map.getEquivalentVars(w,x_0);
                for(int pa_0: m.getParents(x_0)) {
                    int pa_w = pa_0;
                    if( CausalInfo.of(m).isEndogenous(pa_0))
                        pa_w = map.getEquivalentVars(w, pa_0);;
                    merged.addParent(x_w, pa_w);
                }
                // Set the factor with the new domain
                Factor f = (Factor) m.getFactor(x_0);
                f = f.renameDomain(map.getEquivalentVars(w,f.getDomain().getVariables()));
                merged.setFactor(x_w, f);
            }
            w++;
        }

        // set the map
        map.setModel(merged);

        return merged;

    }


}
