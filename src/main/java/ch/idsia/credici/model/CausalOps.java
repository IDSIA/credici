package ch.idsia.credici.model;

import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.SeparatelySpecified;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.GenericSparseModel;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Arrays;
import java.util.stream.IntStream;

public class CausalOps {

    /**
     * Merge the a SCM with other equivalent ones to create a counterfactual model.
     * @param models
     * @return
     */
    public static StructuralCausalModel merge(StructuralCausalModel reality, StructuralCausalModel... models) {
        // ALESSANDRO: code for counterfactual graphs
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
            if (!Arrays.equals(reality.getVariables(), m.getVariables()))
                throw new IllegalArgumentException("Error: models cannot be merged: different variables");
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

            int[] m_endogenous =  ArraysUtil.intersection(
                    CausalInfo.of(reality).getEndogenousVars(),
                    m.getVariables());

            // Add all the endogenous variables
            for(int x_0: m_endogenous)
            {
                int x_w = merged.addVariable(m.getSize(x_0));
                map.set(x_w,w,x_0);
            }
            // add the arcs
            for(int x_0:  m_endogenous) {
                int x_w = map.getEquivalentVars(w,x_0);
                for(int pa_0: m.getParents(x_0)) {
                    int pa_w = pa_0;
                    if( ArraysUtil.contains(pa_0, m_endogenous))
                        pa_w = map.getEquivalentVars(w, pa_0);;
                    merged.addParent(x_w, pa_w);
                }
                // Set the factor with the new domain
                GenericFactor f = (GenericFactor) m.getFactor(x_0);

                int[] leftVars;
                int[] rightVars;
                if(f instanceof VertexFactor) {
                    leftVars = ((VertexFactor)f).getDataDomain() .getVariables();
                    rightVars = ((VertexFactor)f).getSeparatingDomain() .getVariables();
                }else{
                    leftVars = ((SeparateHalfspaceFactor)f).getDataDomain() .getVariables();
                    rightVars = ((SeparateHalfspaceFactor)f).getSeparatingDomain() .getVariables();
                }
                f = f.renameDomain(map.getEquivalentVars(w,Ints.concat(leftVars,rightVars)));

                f = (GenericFactor) ((SeparatelySpecified)f).sortParents();


                merged.setFactor(x_w, f);
            }
            w++;
        }

        // set the map
        map.setModel(merged);

        return merged;

    }


    public static GenericSparseModel intervention(GenericSparseModel model, int var, int state, boolean... removeDisconnected){
        if(removeDisconnected.length == 0)
            removeDisconnected = new boolean[]{false};
        else if(removeDisconnected.length>1)
            throw new IllegalArgumentException("wrong length of removeDisconnected");

        GenericSparseModel do_model = model.copy();
        // remove the parents
        for(int v: model.getParents(var)){
            do_model.removeParent(var, v);
        }

        // remove any variable that is now disconnected
        if(removeDisconnected[0])
            for(int v: do_model.getVariables()) {
                if(!do_model.areConnected(v,var))
                    do_model.removeVariable(v);
            }

        // Fix the value of the intervened variable
        do_model.setFactor(var, model.getFactor(var).get_deterministic(var, state));
        return do_model;

    }

    public static StructuralCausalModel intervention(StructuralCausalModel model, int var, int state, boolean... removeDisconnected){
        return (StructuralCausalModel) intervention((GenericSparseModel) model, var, state, removeDisconnected);
    }

    public static SparseModel intervention(SparseModel model, int var, int state, boolean... removeDisconnected){
        return (SparseModel) intervention((GenericSparseModel) model, var, state, removeDisconnected);
    }


    public static GenericSparseModel applyInterventions(GenericSparseModel model, TIntIntMap intervention, boolean... removeDisconnected){
        GenericSparseModel do_model = model;
        for(int i=0; i<intervention.size(); i++) {
            do_model =  CausalOps.intervention(do_model, intervention.keys()[i], intervention.values()[i], removeDisconnected);
        }
        return do_model;
    }


    /**
     * Builds the counterfactual model from a set of interventions.
     * @param model
     * @param intervention
     * @return
     */
    private static GenericSparseModel counterfactualModel(GenericSparseModel model, TIntIntMap... intervention){
        if(intervention.length > 1)
            throw new NotImplementedException("Counterfactual case with multiple worlds not implemented yet ");
        GenericSparseModel alternative =  applyInterventions(model, intervention[0]);
        if(model instanceof SparseModel)
            return merge((SparseModel)model, (SparseModel)alternative);
        return merge((StructuralCausalModel) model, (StructuralCausalModel)alternative);

    }


    /**
     * Builds the counterfactual model from a set of interventions.
     * @param model
     * @param intervention
     * @return
     */
    public static StructuralCausalModel counterfactualModel(StructuralCausalModel model, TIntIntMap... intervention){
        return (StructuralCausalModel) counterfactualModel((GenericSparseModel) model, intervention);
    }

    /**
     * Builds the counterfactual model from a set of interventions.
     * @param model
     * @param intervention
     * @return
     */

    public static SparseModel counterfactualModel(SparseModel model, TIntIntMap... intervention){
        return (SparseModel) counterfactualModel((GenericSparseModel) model, intervention);
    }


    public static SparseDirectedAcyclicGraph makeValidForSCMs(SparseDirectedAcyclicGraph dag) {
        // todo: implement
        return dag;
    }


}
