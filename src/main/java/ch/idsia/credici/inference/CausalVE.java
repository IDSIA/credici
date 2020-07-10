package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.ArrayUtils;


public class CausalVE extends CausalInference<StructuralCausalModel, BayesianFactor> {


    private int[] elimOrder;

    public CausalVE(StructuralCausalModel model){
        this.model = model.copy();
        this.elimOrder = model.getVariables();
    }


    public BayesianFactor run(Query q){

        int[] target = q.getTarget();
        TIntIntMap evidence = q.getEvidence();
        TIntIntMap intervention = q.getIntervention();

        // Get the mutilated model
        StructuralCausalModel do_model = applyInterventions(intervention);

        RemoveBarren removeBarren = new RemoveBarren();
        do_model = removeBarren
                .execute(new CutObserved().execute(do_model, evidence), target, evidence);



        TIntIntHashMap filteredEvidence = new TIntIntHashMap();
        // update the evidence
        for(int v: evidence.keys()){
            if(ArrayUtils.contains(model.getVariables(), v)){
                filteredEvidence.put(v, evidence.get(v));
            }
        }

        int[] newElimOrder = ArraysUtil.intersection(elimOrder, do_model.getVariables());

        // run variable elimination as usual
        VariableElimination ve = new FactorVariableElimination(newElimOrder);
        if(filteredEvidence.size()>0) ve.setEvidence(filteredEvidence);
        ve.setFactors(do_model.getFactors());
        return (BayesianFactor) ve.run(target);
    }

    public int[] getElimOrder() {
        return elimOrder;
    }

    public CausalVE setElimOrder(int[] elimOrder) {
        this.elimOrder = elimOrder;
        return this;
    }





}
