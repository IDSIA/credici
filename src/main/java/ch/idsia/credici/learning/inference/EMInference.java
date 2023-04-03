package ch.idsia.credici.learning.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;

public interface EMInference {
    BayesianFactor run(StructuralCausalModel posteriorModel, int U, TIntIntMap obs, String hash) throws InterruptedException ;
}
