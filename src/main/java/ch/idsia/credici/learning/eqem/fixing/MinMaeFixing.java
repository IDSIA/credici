package ch.idsia.credici.learning.eqem.fixing;

import ch.idsia.crema.factor.bayesian.BayesianFactor;

public class MinMaeFixing {

	/**
	 * Get the min mae of the current column from the nearest deterministic
	 * function.
	 * 
	 * In the deterministic function only one state will have p=1 all other will
	 * have p=0; The method traverses the column only once. It will not look for the
	 * maximum first, but will continuously correct the score if a higher p is
	 * found.
	 * 
	 * @param factor        the factor
	 * @param column_offset the column to be considered
	 * @param stride        the stride of the variable
	 * @param states        the number of strates of the variable
	 * @return
	 */
	@SuppressWarnings("unused")
	private double scoreExtremeMae(BayesianFactor factor, int column_offset, int stride, int states) {
		// start with an impossibly high value for both score and high_delta.
		// at the first state we will cancel them out
		double score = 2;
		double high_delta = score;
		double low_delta = 0;

		for (int state = 0; state < states; ++state) {
			double v = factor.getValueAt(column_offset + state * stride);
			double h = 1 - v;

			if (h < high_delta) {
				// found a better winner, fix score
				score -= high_delta;
				score += low_delta;

				// this is the winner now
				score += h;

				// remember values to be able to cancel the winner out
				high_delta = h;
				low_delta = v;

			} else {
				// not a winning state
				score += v;
			}
		}
		return score / states;
	}

	
}
