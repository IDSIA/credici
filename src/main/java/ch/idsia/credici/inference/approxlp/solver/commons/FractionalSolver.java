package ch.idsia.credici.inference.approxlp.solver.commons;

import ch.idsia.credici.inference.approxlp.solver.LinearFractionalSolver;
import ch.idsia.crema.factor.credal.linear.ExtensiveLinearFactor;

import ch.idsia.crema.utility.ArraysUtil;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Charnes-Cooper transformation
 *
 * @author davidhuber
 */
public class FractionalSolver implements LinearFractionalSolver {

	private PointValuePair solution;
	private LinearConstraintSet problem;
	private int size;
	private GoalType goalType;
	private Collection<LinearConstraint> base;

	@Override
	public void loadProblem(ExtensiveLinearFactor factor, GoalType type) {
		loadProblem(factor.getLinearProblem(), type);
	}

	@Override
	public void loadProblem(LinearConstraintSet data, GoalType type) {
		this.problem = data;
		this.goalType = type;
		size = 0;

		for (LinearConstraint c : data.getConstraints()) {
			size = Math.max(size, c.getCoefficients().getDimension());
		}

		base = new ArrayList<>(data.getConstraints().size() + 1);

		RealVector vector = new OpenMapRealVector(size + 1);
		vector.setEntry(size, 1);
		base.add(new LinearConstraint(vector, Relationship.GEQ, 0.0));

		// we have to move from Ax <= b to Ax <= bt which is actually
		// Ax - bt <= 0 (we actually take the relation from the input constraint
		// and not necessarily <=)
		for (LinearConstraint constraint : problem.getConstraints()) {
			RealVector row_vector = new OpenMapRealVector(size + 1);
			row_vector.setSubVector(0, constraint.getCoefficients());
			row_vector.setEntry(size, -constraint.getValue());

			base.add(new LinearConstraint(row_vector, constraint.getRelationship(), 0.0));
		}
	}

	public void solve(double[] numerator, double[] denominator, double mult) {
		// numerator & denom must be of length size + 1

		ArrayList<LinearConstraint> constraints = new ArrayList<>(base);
		int columns = size + 1;

		RealVector vector = new OpenMapRealVector(columns);
		vector.setSubVector(0, new ArrayRealVector(denominator));
		constraints.add(new LinearConstraint(vector, Relationship.EQ, mult));
		//constraints.add(new LinearConstraint(vector, Relationship.GEQ, mult-0.01));
		//constraints.add(new LinearConstraint(vector, Relationship.LEQ, mult+0.01));


		//constraints = (ArrayList<LinearConstraint>) SeparateHalfspaceFactor.getNoisedConstraintSet(constraints, 0.00001);


		SimplexSolver solver = new SimplexSolver();

		solution = solver.optimize(new LinearConstraintSet(constraints), new LinearObjectiveFunction(numerator, 0), goalType, new NonNegativeConstraint(true));
	}

	@Override
	public void solve(double[] numerator, double num_const, double[] denominator, double denom_const) {
		solve(ArraysUtil.append(numerator, num_const), ArraysUtil.append(denominator, denom_const),1);
	}

	@Override
	public double getValue() {
		return solution.getValue();
	}

	@Override
	public double[] getVertex() {
		double[] vertex = java.util.Arrays.copyOf(solution.getPointRef(), size);
		double q = solution.getPointRef()[size];
		for (int i = 0; i < size; ++i)
			vertex[i] /= q;
		return vertex;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (LinearConstraint constraint : base) {
			builder.append(Arrays.toString(constraint.getCoefficients().toArray()));
			builder.append(constraint.getRelationship());
			builder.append(constraint.getValue());
			builder.append("\n");
		}
		return builder.toString();
	}

	@Override
	public void solve(double[] numerator, double alpha, double[] denominator, double beta, double mult) {
		solve(ArraysUtil.append(numerator, alpha), ArraysUtil.append(denominator, beta), mult);

	}
}
