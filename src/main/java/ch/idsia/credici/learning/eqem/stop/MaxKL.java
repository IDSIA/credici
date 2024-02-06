package ch.idsia.credici.learning.eqem.stop;

import org.apache.commons.math3.util.FastMath;

import ch.idsia.credici.learning.eqem.StopCriterion;
import ch.idsia.crema.factor.bayesian.BayesianFactor;

public class MaxKL implements StopCriterion {

	double score;
	
	
	@Override
	public void reset() {
	}

	@Override
	public void newIteration(double expectationLL) {
		score = 0;
	}

	@Override
	public void accumulate(BayesianFactor newfactor, double[] originaldata) {
		double kl = 0;
		double[] nf = newfactor.getData();
		// p * log(p/q)
		for (int i = 0; i < nf.length; ++i) {
			kl += Math.abs(nf[i] - originaldata[i]);// nf[i] * FastMath.log(nf[i]/originaldata[i]);
		}
		score = FastMath.max(score, kl);
	}

	@Override
	public boolean converged() {
		return score == 0;
	}

}

