package ch.idsia.credici.learning.eqem.stop;

import ch.idsia.credici.learning.eqem.StopCriterion;
import ch.idsia.crema.factor.bayesian.BayesianFactor;

public class LLStop implements StopCriterion {

	double score; 
	double oldscore; 
	
	double maxdiff = 0;
	
	public void reset() { 
		score = Double.NEGATIVE_INFINITY;
	}
	
	@Override
	public void newIteration(double ll) {
		oldscore = score;
		score = ll;
		maxdiff = 0;
	}

	@Override
	public void accumulate( BayesianFactor newfactor, double[] originaldata) {
//		double[] newdata = newfactor.getData();
//		for (int i = 0; i < newdata.length; ++i) {
//			maxdiff = Math.max(maxdiff, Math.abs(originaldata[i] - newdata[i]));
//		}
	}

	@Override
	public boolean converged() {
		boolean converged = score - oldscore <= 0;
//		if (converged) System.out.println("Converged with max diff: " + maxdiff);
		return converged;
	}
}
