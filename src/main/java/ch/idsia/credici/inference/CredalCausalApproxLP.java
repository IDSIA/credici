package ch.idsia.credici.inference;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.counterfactual.WorldMapping;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.preprocess.CutObservedSepHalfspace;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.interval.IntervalFactor;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactor;
import ch.idsia.crema.inference.approxlp1.ApproxLP1;
import ch.idsia.crema.model.graphical.DAGModel;
import ch.idsia.crema.preprocess.RemoveBarren;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;

public class CredalCausalApproxLP extends CausalInference<DAGModel<SeparateHalfspaceFactor>, IntervalFactor> {


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

    public CredalCausalApproxLP(DAGModel<SeparateHalfspaceFactor> model){
        CausalInfo.assertIsHCredal(model);
        this.model = model;

    }

    @Override
    public DAGModel<SeparateHalfspaceFactor> getInferenceModel(Query q, boolean simplify) {

        target = q.getTarget();
        epsilon = q.getEpsilon();

        TIntIntMap evidence = q.getEvidence();
        TIntIntMap intervention = q.getIntervention();

        if(target.length>1)
            throw new IllegalArgumentException("A single target variable is allowed with CredalCausalAproxLP ");


        // Get the inference model (simple mutilated or twin graph)
        DAGModel<SeparateHalfspaceFactor> infModel=null;
        if(!q.isCounterfactual()) {
            infModel = (DAGModel<SeparateHalfspaceFactor>) CausalOps.applyInterventions(model, intervention);
        }else{
            infModel = (DAGModel<SeparateHalfspaceFactor>) CausalOps.counterfactualModel(model, intervention);
            //map the target to the alternative world
            q.setCounterfactualMapping(WorldMapping.getMap(infModel));
            target = q.getCounterfactualMapping().getEquivalentVars(1, target);
        }

        // preprocessing
        if (simplify) {
            RemoveBarren removeBarren = new RemoveBarren();
            infModel = (DAGModel<SeparateHalfspaceFactor>) removeBarren
                    .execute(new CutObservedSepHalfspace().execute(infModel, evidence), evidence, target);
            // Add to credici this old functionality or check if the new one works well
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

        DAGModel<SeparateHalfspaceFactor> infModel = getInferenceModel(q);

        TIntIntHashMap filteredEvidence = new TIntIntHashMap();

        // update the evidence
        for(int v: q.getEvidence().keys()){
            if(ArrayUtils.contains(infModel.getVariables(), v)){
                filteredEvidence.put(v, q.getEvidence().get(v));
            }
        }

        IntervalFactor result = null;
        ApproxLP1<SeparateHalfspaceFactor> lp1 = new ApproxLP1();

        if(filteredEvidence.size()>0) {
            //int evbin = new BinarizeEvidence().executeInline(infModel, filteredEvidence, filteredEvidence.size(), false);
            result = lp1.query(infModel, filteredEvidence, target[0]);

        }else{
            result = lp1.query(infModel, target[0]);
        }

        return result;


    }

    public double getEpsilon() {
        return epsilon;
    }

    public CredalCausalApproxLP setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        return this;
    }
}
