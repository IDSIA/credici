package ch.idsia.credici.factor;

import ch.idsia.crema.IO;
import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactorFactory;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.model.graphical.DAGModel;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntObjectMap;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.Relationship;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HalfSpaceFactorBuilder {

	public static SeparateHalfspaceFactor as(Strides left, Strides right, TIntObjectMap<List<LinearConstraint>> data) {
		return SeparateHalfspaceFactorFactory.factory().domain(left,right)
				.data(data.valueCollection().
						stream()
						.collect(Collectors.toList()))
				.get();
	}

	public static SeparateHalfspaceFactor as(Strides left, Strides right, List<LinearConstraint[]> data) {

		List<List<LinearConstraint>> data_ = data.stream().map(v -> List.of(v)).collect(Collectors.toList());
		return SeparateHalfspaceFactorFactory.factory().domain(left,right)
				.data(data_)
				.get();
	}


	public static SeparateHalfspaceFactor as(Strides leftRightDomain, TIntObjectMap<List<LinearConstraint>> data) {
		Strides left = Strides.as(leftRightDomain.getVariables()[0], leftRightDomain.getSizes()[0]);
		Strides right = leftRightDomain.remove(left);
		return as(left,right, data);
	}

	public static LinearConstraint[] buildConstraints(boolean normalized, boolean nonnegative, double[][] coefficients, double[] values, Relationship... rel) {

		int left_combinations = coefficients[0].length;
		List<LinearConstraint> C = new ArrayList<LinearConstraint>();


		// check the coefficient shape
		for (double[] c : coefficients) {
			if (c.length != left_combinations)
				throw new IllegalArgumentException("ERROR: coefficient matrix shape");
		}

		// check the relationship vector length
		if (rel.length == 0) rel = new Relationship[]{Relationship.EQ};
		if (rel.length == 1) {
			Relationship[] rel_aux = new Relationship[coefficients.length];
			for (int i = 0; i < coefficients.length; i++)
				rel_aux[i] = rel[0];
			rel = rel_aux;
		} else if (rel.length != coefficients.length) {
			throw new IllegalArgumentException("ERROR: wrong relationship vector length: " + rel.length);
		}

		for (int i = 0; i < coefficients.length; i++) {
			C.add(new LinearConstraint(coefficients[i], rel[i], values[i]));
		}


		// normalization constraint
		if (normalized) {
			double[] ones = new double[left_combinations];
			for (int i = 0; i < ones.length; i++)
				ones[i] = 1.;
			C.add(new LinearConstraint(ones, Relationship.EQ, 1.0));
		}

		// non-negative constraints
		if (nonnegative) {
			double[] zeros = new double[left_combinations];
			for (int i = 0; i < left_combinations; i++) {
				double[] c = zeros.clone();
				c[i] = 1.;
				C.add(new LinearConstraint(c, Relationship.GEQ, 0));

			}
		}

		return C.toArray(LinearConstraint[]::new);
	}


	public static SeparateHalfspaceFactor deterministic(Strides left, Strides right, int... assignments) {

		if (assignments.length != right.getCombinations())
			throw new IllegalArgumentException("ERROR: length of assignments should be equal to the number of combinations of the parents");

		if (Ints.min(assignments) < 0 || Ints.max(assignments) >= left.getCombinations())
			throw new IllegalArgumentException("ERROR: assignments of deterministic function should be in the inteval [0," + left.getCombinations() + ")");


		SeparateHalfspaceFactorFactory factory =
				SeparateHalfspaceFactorFactory.factory().domain(left, right);


		int left_combinations = left.getCombinations();

		for (int i = 0; i < right.getCombinations(); i++) {
			double[][] coeff = new double[left_combinations][left_combinations];
			for (int j = 0; j < left_combinations; j++) {
				coeff[j][j] = 1.;
			}
			double[] values = new double[left_combinations];
			values[assignments[i]] = 1.;

			// Build the constraints
			LinearConstraint[] C = buildConstraints(true, true, coeff, values, Relationship.EQ);

			// Add the constraints
			for (LinearConstraint c : C) {
				factory.constraint(c, i);
			}
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

	public static SeparateHalfspaceFactor deterministic(Strides left, int assignment){
		return deterministic(left, Strides.empty(), assignment);
	}


	public static void main(String[] args) throws IOException {

		Path folder = Path.of(".");

		folder.resolve("models/party.uai");


		DAGModel m = IO.read(folder.resolve("models/party.uai").toString());

		SeparateHalfspaceFactor f = deterministic(m.getDomain(3), m.getDomain(2,1), 0,0,1,1);
		System.out.println(f);



	}

}
