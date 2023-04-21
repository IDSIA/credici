package ch.idsia.credici.learning.inference;

import java.io.IOError;
import java.io.IOException;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;

public interface ComponentInference {
    public BayesianFactor posterior(int U);

	public void set(StructuralCausalModel posteriorModel);

	public void update(TIntIntMap observation) throws Exception;

}
