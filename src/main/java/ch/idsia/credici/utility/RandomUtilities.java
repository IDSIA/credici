package ch.idsia.credici.utility;

import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.bayesian.BayesianDefaultFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorUtilities;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.vertex.separate.VertexDefaultFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactorUtilities;
import ch.idsia.crema.utility.RandomUtil;
import ch.idsia.crema.utility.hull.ConvexHull;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandomUtilities {

	/**
	 * Sample of vector where the sum of all its elements is 1
	 *
	 * @param size
	 * @param num_decimals
	 * @param zero_allowed
	 * @return
	 */
	public static double[] sampleNormalized(int size, int num_decimals, boolean zero_allowed) {

		int upper = (int) Math.pow(10, num_decimals);
		if (!zero_allowed)
			upper -= size;

		double[] data = new double[size];
		int sum = 0;
		for (int i = 0; i < size - 1; i++) {
			if (sum < upper) {
				int x = RandomUtil.getRandom().nextInt(upper - sum);
				sum += x;
				data[i] = x;
			}

		}
		data[data.length - 1] = ((double) upper - sum);

		for (int i = 0; i < size; i++) {
			if (!zero_allowed) {
				data[i]++;
				data[i] = data[i] / (upper + size);
			} else {
				data[i] = data[i] / upper;
			}
		}
		List<Double> dataList = Doubles.asList(data);
		Collections.shuffle(dataList, RandomUtil.getRandom());
		return Doubles.toArray(dataList);
	}



	public static BayesianFactor BayesianFactorRandom(Strides left, Strides right, int num_decimals, boolean zero_allowed) {
		double[][] data = new double[right.getCombinations()][];

		for (int i = 0; i < data.length; i++) {
			data[i] = sampleNormalized(left.getCombinations(), num_decimals, zero_allowed);
		}

		return new BayesianDefaultFactor(left.concat(right), Doubles.concat(data));

	}




	/**
	 * Method for generating a random VertexFactor of a conditional credal set.
	 *
	 * @param leftDomain:   strides of the conditionated variables.
	 * @param rightDomain:  strides of the conditioning variables.
	 * @param k:            number of vertices
	 * @param num_decimals: number of decimals in the probability values.
	 * @param zero_allowed: flag to control if random probabilities can be zero.
	 * @return
	 */
	public static VertexFactor VertexFactorRandom(Strides leftDomain, Strides rightDomain, int k, int num_decimals, boolean zero_allowed) {
		double[][][] data = new double[rightDomain.getCombinations()][][];

		for(int i = 0; i < data.length; ++i) {
			data[i] = VertexFactorRandom(leftDomain, k, num_decimals, zero_allowed).getVerticesAt(0);
		}

		return new VertexDefaultFactor(leftDomain, rightDomain, data);
	}

	/**
	 * Method for generating a random VertexFactor of a joint credal set.
	 *
	 * @param leftDomain:   strides of the variables.
	 * @param k:            number of vertices
	 * @param num_decimals: number of decimals in the probability values.
	 * @param zero_allowed: flag to control if random probabilities can be zero.
	 * @return
	 */
	public static VertexFactor VertexFactorRandom(Strides leftDomain, int k, int num_decimals, boolean zero_allowed) {
		if (leftDomain.getVariables().length > 1) {
			throw new IllegalArgumentException("leftDomain must have only one variable.");
		} else {
			int leftVar = leftDomain.getVariables()[0];
			if (leftDomain.getCardinality(leftVar) == 2) {
				k = Math.min(k, 2);
			}

			List PMFs = (List) IntStream.range(0, k - 1).mapToObj((i) -> {
				return BayesianFactorRandom(leftDomain, Strides.empty(), num_decimals, zero_allowed);
			}).map((fx) -> {
				return (new BayesianToVertex()).apply(fx, leftVar);
			}).collect(Collectors.toList());

			VertexFactor out;
			do {
				if (k > 1) {
					VertexFactor f = (new BayesianToVertex()).apply(BayesianFactorRandom(leftDomain, Strides.empty(), num_decimals, zero_allowed), leftVar);
					PMFs.add(f);
				}

				out = VertexFactorUtilities.mergeVertices((VertexFactor[])PMFs.toArray((x$0) -> {
					return new VertexFactor[x$0];
				})).convexHull(ConvexHull.DEFAULT);
			} while(out.getVerticesAt(0).length < k);

			return out;
		}
	}


	/**
	 * Sample an array of itegers from a uniform between 0 and a given upper bound (not included).
	 * @param size - length of the output
	 * @param upper_bound - indicates the upper bound of the sampling interval.
	 * @param sample_all - allow to constraint that all passible values should appear in the output.
	 * @return
	 */

	public static int[] sampleUniform(int size, int upper_bound, boolean sample_all){

		if(sample_all && upper_bound>size){
			throw new IllegalArgumentException("ERROR: upper_bound cannot be greater than the array size");
		}

		int[] data = new int[size];

		int current_i = 0;

		if(sample_all){
			for(int i=0; i<upper_bound; i++)
				data[i] = i;
			current_i = upper_bound;
		}
		for(int i = current_i; i<size; i++){
			data[i] = RandomUtil.getRandom().nextInt(upper_bound);
		}

		List dataList =  Ints.asList(data);
		Collections.shuffle(dataList, RandomUtil.getRandom());
		return Ints.toArray(dataList);
	}



}
