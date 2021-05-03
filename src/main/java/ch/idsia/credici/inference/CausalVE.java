package ch.idsia.credici.inference;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.counterfactual.WorldMapping;
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
        this.elimOrder = null;
    }

    @Override
    public StructuralCausalModel getInferenceModel(Query q) {

        target = q.getTarget();
        TIntIntMap evidence = q.getEvidence();
        TIntIntMap intervention = q.getIntervention();

        // Get the inference model (simple mutilated or twin graph)
        StructuralCausalModel infModel=null;
        if(!q.isCounterfactual()) {
            infModel = (StructuralCausalModel) CausalOps.applyInterventions(model, intervention);
        }else{
            infModel = (StructuralCausalModel) CausalOps.counterfactualModel(model, intervention);
            //map the target to the alternative world
            target = WorldMapping.getMap(infModel).getEquivalentVars(1, target);
        }
        RemoveBarren removeBarren = new RemoveBarren();
        infModel = removeBarren
                .execute(new CutObserved().execute(infModel, evidence), target, evidence);

        if(elimOrder==null)
            elimOrder = infModel.getVariables();
        return infModel;
    }

    public BayesianFactor run(Query q){

        StructuralCausalModel infModel = getInferenceModel(q);

        TIntIntHashMap filteredEvidence = new TIntIntHashMap();
        // update the evidence
        for(int v: q.getEvidence().keys()){
            if(ArrayUtils.contains(model.getVariables(), v)){
                filteredEvidence.put(v, q.getEvidence().get(v));
            }
        }

        int[] newElimOrder = ArraysUtil.intersection(elimOrder, infModel.getVariables());

        // run variable elimination as usual
        VariableElimination ve = new FactorVariableElimination(newElimOrder);
        if(filteredEvidence.size()>0) ve.setEvidence(filteredEvidence);
        ve.setFactors(infModel.getFactors());
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
