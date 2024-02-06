package ch.idsia.credici.learning.eqem;

import ch.idsia.crema.factor.bayesian.BayesianFactor;

public interface StopCriterion {
	public void reset();
	public void newIteration(double expectationLL);
	public void accumulate(BayesianFactor newfactor, double[] originaldata);
	public boolean converged();
	
}
