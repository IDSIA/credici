package ch.idsia.credici.learning.eqem;

import ch.idsia.crema.factor.bayesian.BayesianFactor;

import java.util.Arrays;

import org.apache.commons.math3.util.FastMath;

/**
 * A collector to marginalize a variable out of a domain in logspace.
 * 
 * Author:  Claudio "Dna" Bonesana
 * Project: crema
 * Date:    21.04.2021 21:23
 */
public class LogBayesianMarginalHelper {

	private final int[] offsets;
	private final int size;

	/**
	 * Construct the collector that summs all values of the variable.
	 * This will compute the set of offsets defined by the strides. 
	 * 
	 * @param size the size of the variable to be collected
	 * @param stride the stride of the variable.
	 */
	public LogBayesianMarginalHelper(int size, int stride) {
		this.size = size;
		offsets = new int[size];
		
		// we can safely start from 1 as the index 0 is always 0.
		for (int i = 1; i < size; ++i) {
			offsets[i] = i * stride;
		}
	}

	public final double collect(double[] logdata, int source) {

		// // 130
		double value = logdata[source + offsets[0]]; 
		for (int i = 1; i < size; ++i) {
			double v = logdata[source + offsets[i]]; 

			if (v > value) {
				value = v + Math.log1p(FastMath.exp(value - v));
			} else {
				value += Math.log1p(FastMath.exp(v - value));
			}
		}
		return value;
	}
}
