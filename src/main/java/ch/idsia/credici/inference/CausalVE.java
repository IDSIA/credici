package ch.idsia.credici.inference;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;


public class CausalVE extends CausalInference<StructuralCausalModel, BayesianFactor> {


    private int[] elimOrder;

    public CausalVE(StructuralCausalModel model){
        this.model = model.copy();
        this.elimOrder = null;
    }

    @Override
    public StructuralCausalModel getInferenceModel(Query q, boolean simplify) {

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
            q.setCounterfactualMapping(WorldMapping.getMap(infModel));
            target = q.getCounterfactualMapping().getEquivalentVars(1, target);
        }
        if (simplify) {
            RemoveBarren removeBarren = new RemoveBarren();
            infModel = removeBarren
                    .execute(new CutObserved().execute(infModel, evidence), target, evidence);
        }
        if(elimOrder==null)
            elimOrder = new MinFillOrdering().apply(infModel);
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


    public BayesianFactor probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {
        StructuralCausalModel reality = this.getModel();
        StructuralCausalModel doTrue = (StructuralCausalModel)this.causalQuery().setIntervention(cause, trueState).getInferenceModel(false);
        StructuralCausalModel doFalse = (StructuralCausalModel)this.causalQuery().setIntervention(cause, falseState).getInferenceModel(false);

        StructuralCausalModel pns_model = (StructuralCausalModel) CausalOps.merge(reality, doTrue, doFalse);

        WorldMapping map = WorldMapping.getMap(pns_model);
        int target[] = new int[] {map.getEquivalentVars(1, effect),map.getEquivalentVars(2, effect)};

        CausalVE infInternal =  new CausalVE(pns_model);
        BayesianFactor prob = (BayesianFactor) infInternal.causalQuery().setTarget(target).run();
        return (BayesianFactor) FactorUtil.filter(FactorUtil.filter(prob, target[0], trueState), target[1], falseState);
    }


}
