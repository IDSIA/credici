package ch.idsia.credici.learning.inference;

import java.io.IOError;
import java.io.IOException;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;

public interface EMInference {
    public BayesianFactor run(StructuralCausalModel posteriorModel, int U, TIntIntMap obs,  TIntIntMap filteredObs, String hash)
     throws InterruptedException, IOException;

}
