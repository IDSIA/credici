package ch.idsia.credici.inference.approxlp.solver.lpsolve;


import ch.idsia.credici.inference.approxlp.solver.LinearSolver;
import ch.idsia.crema.factor.credal.linear.ExtensiveLinearFactor;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

public class Simplex implements LinearSolver {
	private LpSolve lp;
	private double constant = 0;
	private boolean optimal = false;
	private int[] columnNumbers;

	public Simplex() {
	}

	@Override
	public void loadProblem(ExtensiveLinearFactor factor, GoalType goal) {
		loadProblem(factor.getLinearProblem(), goal);
	}

	private synchronized void dispose() {
		if (lp != null) {
			lp.deleteLp();
			lp = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		dispose();
	}

	@Override
	public void loadProblem(LinearConstraintSet data, GoalType goal) {
		dispose();

		int cols = 0;
		for (LinearConstraint constraint : data.getConstraints()) {
			cols = Math.max(constraint.getCoefficients().getDimension(), cols);
		}

		try {
			lp = LpSolve.makeLp(0, cols);
			//lp.setEpslevel(3);
			lp.setAddRowmode(true);
			columnNumbers = new int[cols];
			for (int i = 0; i < cols; ++i) {
				//lp.setBounds(i + 1, 0, 1);
				columnNumbers[i] = i + 1;
			}
			
			for (LinearConstraint constraint : data.getConstraints()) {
				int rel = 0;
				switch (constraint.getRelationship()) {
				case EQ:
					rel = LpSolve.EQ;
					break; 
				case GEQ:
					rel = LpSolve.GE;
					break;
				case LEQ:
					rel = LpSolve.LE;
					break;
				default:
					break;
				}
			
//				lp.addConstraint(constraint.getCoefficients().toArray(), rel, constraint.getValue());
				lp.addConstraintex(cols, constraint.getCoefficients().toArray(), columnNumbers, rel, constraint.getValue());
			}
			lp.setAddRowmode(false);
			lp.setVerbose(LpSolve.IMPORTANT);
		

			if (goal == GoalType.MAXIMIZE)
				lp.setMaxim();
			else
				lp.setMinim();
		} catch (LpSolveException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void solve(double[] objective, double constant) {
		this.constant = constant;
		try {
			lp.setObjFnex(columnNumbers.length, objective, columnNumbers);
			
			int result = lp.solve();
			optimal = (result == LpSolve.OPTIMAL);
		} catch (LpSolveException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double getValue() {
		try {
			return lp.getObjective() + constant;
		} catch (LpSolveException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double[] getVertex() {
		try {
			double[] result = new double[lp.getNcolumns()];
			lp.getVariables(result);
			return result;
		} catch (LpSolveException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isOptimal() {
		return optimal;
	}
}
