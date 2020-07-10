package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.info.CausalInfo;
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

public class CredalCausalAproxLP extends CausalInference<SparseModel, IntervalFactor> {


    private double epsilon = 0.0;


    public CredalCausalAproxLP(StructuralCausalModel model){
        this(model.toHCredal(model.getEmpiricalProbs()));
    }

    public CredalCausalAproxLP(StructuralCausalModel model, BayesianFactor[] empirical){
        this(model.toHCredal(empirical));

    }
    public CredalCausalAproxLP(StructuralCausalModel model, Collection empirical){
        this(model.toHCredal(empirical));

    }

    public CredalCausalAproxLP(SparseModel model){
        CausalInfo.assertIsHCredal(model);
        this.model = model;

    }


    @Override
    public IntervalFactor query(int[] target, TIntIntMap evidence, TIntIntMap intervention) throws InterruptedException {

        if(target.length>1)
            throw new IllegalArgumentException("A single target variable is allowed with CredalCausalAproxLP ");

        SparseModel do_csmodel = applyInterventions(intervention);

        // preprocessing
        RemoveBarren removeBarren = new RemoveBarren();
        do_csmodel = removeBarren
                .execute(new CutObservedSepHalfspace().execute(do_csmodel, evidence), target, evidence);



        for(int v : do_csmodel.getVariables()) {
             do_csmodel.setFactor(v, ((SeparateHalfspaceFactor) do_csmodel.getFactor(v)).removeNormConstraints());
                if(epsilon>0.0){
                   do_csmodel.setFactor(v, ((SeparateHalfspaceFactor) do_csmodel.getFactor(v)).getPerturbedZeroConstraints(epsilon));
             }

        }


        TIntIntHashMap filteredEvidence = new TIntIntHashMap();

        // update the evidence
        for(int v: evidence.keys()){
            if(ArrayUtils.contains(do_csmodel.getVariables(), v)){
                filteredEvidence.put(v, evidence.get(v));
            }
        }


        IntervalFactor result = null;
        Inference lp1 = new Inference();

        if(filteredEvidence.size()>0) {
            int evbin = new BinarizeEvidence().executeInline(do_csmodel, filteredEvidence, filteredEvidence.size(), false);
            result = lp1.query(do_csmodel, target[0], evbin);

        }else{
            result = lp1.query(do_csmodel, target[0]);
        }

        return result;


    }

    public double getEpsilon() {
        return epsilon;
    }

    public CredalCausalAproxLP setEpsilon(double epsilon) {
        this.epsilon = epsilon;
        return this;
    }
}
