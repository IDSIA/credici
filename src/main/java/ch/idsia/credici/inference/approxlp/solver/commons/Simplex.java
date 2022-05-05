package ch.idsia.credici.inference.approxlp.solver.commons;


import ch.idsia.credici.inference.approxlp.solver.LinearSolver;
import ch.idsia.crema.factor.credal.linear.ExtensiveLinearFactor;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.*;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import java.util.Arrays;

/**
 * An implementation of the LinearSolver interface based on apache's 
 * {@link SimplexSolver}. 
 * 
 * @author huber
 *
 */
public class Simplex implements LinearSolver {
	
	private SimplexSolver solver;
	
	private LinearConstraintSet constraints;
	
	private PointValuePair solution;
	
	private GoalType goal;
	
	public Simplex() {
		solver = new SimplexSolver();
		
	}
	
	@Override
	public void loadProblem(ExtensiveLinearFactor factor, GoalType goal) {
		loadProblem(factor.getLinearProblem(), goal);
	}
	
	@Override
	public double getValue() {
		return solution.getValue();
	}
	
	@Override
	public double[] getVertex() {
		return solution.getPoint();
	}
	
	@Override
	public void solve(double[] objective, double constant) {
		solution = solver.optimize(constraints, 
			new NonNegativeConstraint(true), 
			new LinearObjectiveFunction(objective, constant), 
			goal, 
			PivotSelectionRule.BLAND);
	}

	@Override
	public void loadProblem(LinearConstraintSet data, GoalType goal) {
		this.goal = goal;
		this.constraints = data;
	}
	
	@Override
	public boolean isOptimal() {
		return Arrays.equals(solver.getLowerBound(), solver.getUpperBound());
	}
}
