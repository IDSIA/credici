package ch.idsia.credici.factor;

import ch.idsia.crema.IO;
import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactorFactory;
import ch.idsia.crema.model.graphical.DAGModel;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.nio.file.Path;

public class VertexFactorBuilder {


	/**
	 * Static method that builds a deterministic factor (values can only be ones or zeros).
	 * Thus, children variables are determined by the values of the parents
	 * @param left	Strides - children variables.
	 * @param right	Strides - parent variables
	 * @param assignments assignments of each combination of the parent
	 * @return
	 */
	public static VertexFactor deterministic(Strides left, Strides right, int... assignments){

		if (assignments.length != right.getCombinations())
			throw new IllegalArgumentException("ERROR: length of assignments should be equal to the number of combinations of the parents");

		if (Ints.min(assignments)<0 || Ints.max(assignments)>= left.getCombinations())
			throw new IllegalArgumentException("ERROR: assignments of deterministic function should be in the inteval [0,"+left.getCombinations()+")");



		VertexFactorFactory factory = VertexFactorFactory.factory().domain(left, right);

		for(int i=0; i< right.getCombinations(); i++){
			double[] values = new double[left.getCombinations()];
			values[assignments[i]] = 1.0;
			factory.addVertex(values, i);
		}
		return factory.get();
	}
	/**
	 * Static method that builds a deterministic factor (values can only be ones or zeros)
	 * without parent variables.
	 * @param left	Strides - children variables.
	 * @param assignment int - single value to assign
	 * @return
	 */

	public static VertexFactor deterministic(Strides left, int assignment){
		return deterministic(left, Strides.empty(), assignment);
	}


	public static void main(String[] args) throws IOException {

		Path folder = Path.of("/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/tmp/crema");

		folder.resolve("models/party.uai");


		DAGModel m = IO.read(folder.resolve("models/party.uai").toString());

		VertexFactor f = deterministic(m.getDomain(3), m.getDomain(2,1), 0,0,1,1);
		System.out.println(f);

	}
}
