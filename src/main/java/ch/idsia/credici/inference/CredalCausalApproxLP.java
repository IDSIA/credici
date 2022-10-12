package ch.idsia.credici.inference;

import java.util.Collection;

import ch.idsia.credici.model.tools.CausalInfo;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;

import ch.idsia.credici.inference.approxlp.ApproxLP1;
import ch.idsia.credici.model.tools.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.BinarizeEvidence;
import ch.idsia.crema.preprocess.CutObservedSepHalfspace;
import ch.idsia.crema.preprocess.RemoveBarren;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class CredalCausalApproxLP extends CausalInference<SparseModel, IntervalFactor> {


    private double epsilon = 0.0;


    public CredalCausalApproxLP(StructuralCausalModel model){
        this(model.toHCredal(model.getEmpiricalProbs()));
    }

    public CredalCausalApproxLP(StructuralCausalModel model, BayesianFactor[] empirical){
        this(model.toHCredal(empirical));

    }
    public CredalCausalApproxLP(StructuralCausalModel model, Collection empirical){
        this(model.toHCredal(empirical));

    }

    public CredalCausalApproxLP(SparseModel model){
        //CausalInfo.assertIsHCredal(model);
        this.model = model;

    }

    @Override
    public SparseModel getInferenceModel(Query q, boolean simplify) {

        target = q.getTarget();
        epsilon = q.getEpsilon();

        TIntIntMap evidence = q.getEvidence();
        TIntIntMap intervention = q.getIntervention();

        if(target.length>1)
            throw new IllegalArgumentException("A single target variable is allowed with CredalCausalAproxLP ");


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

        // preprocessing
        if (simplify) {
            RemoveBarren removeBarren = new RemoveBarren();
            infModel = removeBarren
                    .execute(new CutObservedSepHalfspace().execute(infModel, evidence), target, evidence);
        }

        for(int v : infModel.getVariables()) {
            infModel.setFactor(v, ((SeparateHalfspaceFactor) infModel.getFactor(v)).removeNormConstraints());
            if(epsilon>0.0){
                infModel.setFactor(v, ((SeparateHalfspaceFactor) infModel.getFactor(v)).getPerturbedZeroConstraints(epsilon));
            }

        }
        return infModel;

    }

    @Override
    public IntervalFactor run(Query q) throws InterruptedException {

        SparseModel infModel = getInferenceModel(q);

        TIntIntHashMap filteredEvidence = new TIntIntHashMap();

        // update the evidence
        for(int v: q.getEvidence().keys()){
            if(ArrayUtils.contains(infModel.getVariables(), v)){
                filteredEvidence.put(v, q.getEvidence().get(v));
            }
        }

        IntervalFactor result = null;
        ApproxLP1 lp1 = new ApproxLP1();

        if(filteredEvidence.size()>0) {
            int evbin = new BinarizeEvidence().executeInline(infModel, filteredEvidence, filteredEvidence.size(), false);
            result = lp1.query(infModel, target[0], evbin);

        }else{
            result = lp1.query(infModel, target[0], -1);
        }

        return result;


    }

    public IntervalFactor probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {
        SparseModel reality = (SparseModel) this.getModel();
        SparseModel doTrue = (SparseModel)this.causalQuery().setIntervention(cause, trueState).getInferenceModel(false);
        SparseModel doFalse = (SparseModel)this.causalQuery().setIntervention(cause, falseState).getInferenceModel(false);

        SparseModel pns_model = (SparseModel) CausalOps.merge(reality, doTrue, doFalse);

        WorldMapping map = WorldMapping.getMap(pns_model);
        int target[] = new int[] {map.getEquivalentVars(1, effect),map.getEquivalentVars(2, effect)};
        for(int x:CausalInfo.of(reality).getEndogenousVars()) pns_model.removeVariable(x);

        CausalInference infInternal =  new CredalCausalVE(pns_model);
        VertexFactor prob = (VertexFactor) infInternal.causalQuery().setTarget(target).run();
        return (IntervalFactor) FactorUtil.filter(FactorUtil.filter(prob, target[0], trueState), target[1], falseState);

    }

    public double getEpsilon() {
        return epsilon;
    }

    public CredalCausalApproxLP setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        return this;
    }
}
