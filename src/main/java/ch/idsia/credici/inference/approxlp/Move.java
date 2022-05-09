package ch.idsia.credici.inference.approxlp;

import ch.idsia.crema.factor.bayesian.BayesianFactor;

import java.util.Arrays;

public class Move {

	private double scorecache;
	private BayesianFactor valuescache;
	private int free;

	public Move(int free) {
		this.free = free;
		scorecache = Double.NaN;
	}

	public double getScore() {
		return scorecache;
	}

	public void setScore(double scorecache) {
		this.scorecache = scorecache;
	}

	/**
	 * Returns P(Xj|∏j) as a vector of doubles. This is what the linear solver found as
	 * solution to the problem.
	 *
	 * @return
	 */
	public BayesianFactor getValues() {
		return valuescache;
	}

	public void setValues(BayesianFactor valuescache) {
		this.valuescache = valuescache;
	}

	public int getFree() {
		return free;
	}

	public void setFree(int free) {
		this.free = free;
	}

	@Override
	public String toString() {
		return "Free " + free + " scores " + scorecache + " values " + (valuescache == null ? "null" : Arrays.toString(valuescache.getData()));
	}

}
