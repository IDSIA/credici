package edu.neurips.causalem.inference;

import edu.neurips.causalem.model.CausalOps;
import edu.neurips.causalem.model.StructuralCausalModel;
import edu.neurips.causalem.model.counterfactual.WorldMapping;
import edu.neurips.causalem.model.info.CausalInfo;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.inference.approxlp.Inference;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.BinarizeEvidence;
import ch.idsia.crema.preprocess.CutObservedSepHalfspace;
import ch.idsia.crema.preprocess.RemoveBarren;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;

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
        CausalInfo.assertIsHCredal(model);
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
        Inference lp1 = new Inference();

        if(filteredEvidence.size()>0) {
            int evbin = new BinarizeEvidence().executeInline(infModel, filteredEvidence, filteredEvidence.size(), false);
            result = lp1.query(infModel, target[0], evbin);

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
