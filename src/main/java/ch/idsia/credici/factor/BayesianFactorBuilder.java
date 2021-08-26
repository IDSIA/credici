package ch.idsia.credici.factor;

import ch.idsia.crema.IO;
import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.model.graphical.DAGModel;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.IntStream;

public class BayesianFactorBuilder {

	public static BayesianFactor as(Strides domain, double[] data){
		return BayesianFactorFactory.factory().domain(domain).data(data).get();
	}

	public static BayesianFactor constant(Strides domain, double val){
		double[] data = IntStream.range(0, domain.getCombinations()).mapToDouble(i -> val).toArray();
		return as(domain, data);
	}
	public static BayesianFactor zeros(Strides domain){
		return constant(domain, 0.0);
	}



	/**
	 * Static method that builds a deterministic factor (values can only be ones or zeros).
	 * Thus, children variables are determined by the values of the parents
	 * @param left	Strides - children variables.
	 * @param right	Strides - parent variables
	 * @param assignments assignments of each combination of the parent
	 * @return
	 */
	public static BayesianFactor deterministic(Strides left, Strides right, int... assignments){

		if (assignments.length != right.getCombinations())
			throw new IllegalArgumentException("ERROR: length of assignments should be equal to the number of combinations of the parents");

		if (Ints.min(assignments)<0 || Ints.max(assignments)>= left.getCombinations())
			throw new IllegalArgumentException("ERROR: assignments of deterministic function should be in the inteval [0,"+left.getCombinations()+")");


		double[] values = new double[right.union(left).getCombinations()];
		for(int i=0; i< right.getCombinations(); i++){
			values[i * left.getCombinations() + assignments[i]] = 1.0;
		}

		return as(left.concat(right), values);
	}


	/**
	 * Static method that builds a deterministic factor (values can only be ones or zeros)
	 * without parent variables.
	 * @param left	Strides - children variables.
	 * @param assignment int - single value to assign
	 * @return
	 */

	public static BayesianFactor deterministic(Strides left, int assignment){
		return deterministic(left, Strides.empty(), assignment);
	}



	public static void main(String[] args) throws IOException {

		Path folder = Path.of(".");
		folder.resolve("models/party.uai");

		DAGModel m = IO.read(folder.resolve("models/party.uai").toString());

		BayesianFactor f = deterministic(m.getDomain(3), m.getDomain(2,1), 0,0,1,1);
		System.out.println(f);

	}
}
