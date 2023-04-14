package ch.idsia.credici.learning.inference;

import java.io.IOException;

import ch.idsia.credici.learning.ve.VE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class CremaVE implements EMInference {


    @Override
    public BayesianFactor run(StructuralCausalModel posteriorModel, int U, TIntIntMap obs,  TIntIntMap filteredObs, String hash) throws InterruptedException, IOException {
        int[] order = (new MinFillOrdering()).apply(posteriorModel);
        VE<BayesianFactor> inferenceEngine = new VE<>(order);

        StructuralCausalModel infModel = new CutObserved().execute(posteriorModel, obs);
        RemoveBarren rb = new RemoveBarren();
        infModel = rb.execute(infModel, U, obs);
                
        TIntIntMap newObs = new TIntIntHashMap(obs);
        rb.filter(newObs);

        return inferenceEngine.apply(infModel, U, newObs);  // P(U|X=obs)
    }
}
