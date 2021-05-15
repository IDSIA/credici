package ch.idsia.credici.inference;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;

public class CredalCausalVE extends CausalInference<SparseModel, VertexFactor> {


    public CredalCausalVE(StructuralCausalModel model){
        this(model.toVCredal(model.getEmpiricalProbs()));
    }


    public CredalCausalVE(StructuralCausalModel model, BayesianFactor[] empirical){
        this(model.toVCredal(empirical));
    }


    public CredalCausalVE(StructuralCausalModel model, Collection empirical){
        this(model.toVCredal(empirical));
    }

    public CredalCausalVE(SparseModel model){
        CausalInfo.assertIsVCredal(model);
        this.model = model;
    }


    @Override
    public SparseModel getInferenceModel(Query q, boolean simplify) {

        target = q.getTarget();
        TIntIntMap evidence = q.getEvidence();
        TIntIntMap intervention = q.getIntervention();

        // Get the inference model (simple mutilated or twin graph)
        SparseModel infModel=null;
        if(!q.isCounterfactual()) {
            infModel = (SparseModel) CausalOps.applyInterventions(model, intervention);
        }else{
            infModel = (SparseModel) CausalOps.counterfactualModel(model, intervention);
            //map the target to the alternative world
            q.setCounterfactualMapping(WorldMapping.getMap(infModel));
            target = q.getCounterfactualMapping().getEquivalentVars(1, target);
        }

        // cut arcs coming from an observed node and remove barren w.r.t the target
        if (simplify) {
            RemoveBarren removeBarren = new RemoveBarren();
            infModel = removeBarren
                    .execute(new CutObserved().execute(infModel, evidence), target, evidence);
        }
        return infModel;
    }

    @Override
    public VertexFactor run(Query q) throws InterruptedException {

        //Build the inference model
        SparseModel infModel = getInferenceModel(q);

        // Update the evidence
        TIntIntHashMap filteredEvidence = new TIntIntHashMap();
        // update the evidence
        for(int v: q.getEvidence().keys()){
            if(ArrayUtils.contains(infModel.getVariables(), v)){
                filteredEvidence.put(v, q.getEvidence().get(v));
            }
        }
        // Get the  elimination order
        int[] elimOrder = infModel.getVariables();

        FactorVariableElimination ve = new FactorVariableElimination(elimOrder);
        if(filteredEvidence.size()>0)
            ve.setEvidence(filteredEvidence);
        ve.setNormalize(false);
        VertexFactor.CONVEX_HULL_MARG = true;
        ve.setFactors(infModel.getFactors());
        return ((VertexFactor) ve.run(target)).normalize().convexHull(true);

    }


    public VertexFactor probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {
        SparseModel reality = (SparseModel) this.getModel();
        SparseModel doTrue = (SparseModel)this.causalQuery().setIntervention(cause, trueState).getInferenceModel(false);
        SparseModel doFalse = (SparseModel)this.causalQuery().setIntervention(cause, falseState).getInferenceModel(false);

        SparseModel pns_model = (SparseModel) CausalOps.merge(reality, doTrue, doFalse);

        WorldMapping map = WorldMapping.getMap(pns_model);
        int target[] = new int[] {map.getEquivalentVars(1, effect),map.getEquivalentVars(2, effect)};

        CausalInference infInternal =  new CredalCausalVE(pns_model);
        VertexFactor prob = (VertexFactor) infInternal.causalQuery().setTarget(target).run();
        return (VertexFactor) FactorUtil.filter(FactorUtil.filter(prob, target[0], trueState), target[1], falseState);

    }

}
