package ch.idsia.credici.inference.approxlp;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.hash.TIntObjectHashMap;


public class Solution {

	private final TIntObjectHashMap<BayesianFactor> data;
	private final int free;

	private double scorecache;

	/**
	 * Initial solutions have no source
	 *
	 * @param data
	 * @param score
	 */
	Solution(TIntObjectHashMap<BayesianFactor> data, double score) {
		this.scorecache = score;
		this.data = data;
		this.free = -1;
	}

	Solution(Solution source, Move move) {
		// shallow copy
		this.data = new TIntObjectHashMap<>(source.data);

		if (move.getValues() == null)
			throw new IllegalArgumentException("The provided move has never been evaluated");

		this.free = move.getFree(); // info about what just change (usefull for neighbourhood)
		this.data.put(free, move.getValues());
		this.scorecache = move.getScore();
	}

	int getFree() {
		return free;
	}

	/**
	 * Packages accessible method to get the internal data of the solution
	 *
	 * @return
	 */
	TIntObjectHashMap<BayesianFactor> getData() {
		return data;
	}

	void setScore(double score) {
		this.scorecache = score;
	}

	public double getScore() {
		return scorecache;
	}

}
