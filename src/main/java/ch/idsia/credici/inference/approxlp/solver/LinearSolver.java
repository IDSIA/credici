package ch.idsia.credici.inference.approxlp.solver;

import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import ch.idsia.crema.factor.credal.linear.ExtensiveLinearFactor;

/**
 * A linear programming solver's minimal required interface for CreMA's needs.
 *
 * @author huber
 */
public interface LinearSolver {

	/**
	 * Load the constraints from the specified {@link ExtensiveLinearFactor}.
	 *
	 * @param factor - the set of constraints as an {@link ExtensiveLinearFactor}.
	 * @param goal   - {@link GoalType} the direction of the optimization.
	 */
	@SuppressWarnings("rawtypes")
	void loadProblem(ExtensiveLinearFactor factor, GoalType goal);


	/**
	 * Load the constraints from the specified {@link LinearConstraintSet}.
	 *
	 * @param data - the set of constraints as an {@link LinearConstraintSet}.
	 * @param goal - {@link GoalType} the direction of the optimization.
	 */
	void loadProblem(LinearConstraintSet data, GoalType goal);

	/**
	 * Start the solver with the specified objective function and constant term
	 *
	 * @param objective
	 * @param constant
	 */
	void solve(double[] objective, double constant);

	/**
	 * Get the objective function's value
	 *
	 * @return
	 */
	double getValue();

	/**
	 * Get the vertex of the found solution.
	 *
	 * @return
	 */
	double[] getVertex();

	/**
	 * Return whether the found solution is optimal.
	 *
	 * @return
	 */
	boolean isOptimal();

}
