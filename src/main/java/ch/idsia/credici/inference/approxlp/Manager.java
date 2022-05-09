package ch.idsia.credici.inference.approxlp;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.SeparateLinearToExtensiveHalfspaceFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.search.ObjectiveFunction;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import gnu.trove.map.TIntIntMap;

abstract class Manager implements ObjectiveFunction<Move, Solution> {

	public static final double BAD = Double.NaN;
	private static final double EPS = 0.000000000001;

	protected SeparateLinearToExtensiveHalfspaceFactor sep2ext = new SeparateLinearToExtensiveHalfspaceFactor();

	// the underlying model
	protected GraphicalModel<? extends GenericFactor> model;
	protected GoalType goal;

	protected int x0;
	protected int x0state;

	protected int[] sequence;

	private BayesianFactor x0factor;

	public Manager(GraphicalModel<? extends GenericFactor> model, GoalType dir, int x0, int x0state) {
		this.model = model;
		this.goal = dir;
		this.x0 = x0;
		this.x0state = x0state;

		this.sequence = new MinFillOrdering().apply(model);
	}

	public GoalType getGoal() {
		return goal;
	}

	/**
	 * This is a custom bayesian factor over x0 with a 1 for x0state.
	 * The value is cached.
	 *
	 * @return
	 */
	protected BayesianFactor getX0factor() {
		if (x0factor == null) {
			int size = model.getSize(x0);
			double[] realObj = new double[size];
			realObj[x0state] = 1.0;

			x0factor = new BayesianFactor(model.getDomain(x0), realObj);
		}
		return x0factor;
	}

	/**
	 * Make non vertex changing moves illegal.
	 *
	 * @param from
	 * @param move
	 */
	protected void fixNotMoving(Solution from, Move move) {
		if (Double.isNaN(from.getScore()))
			return;

		BayesianFactor original = from.getData().get(move.getFree());
		if (original.equals(move.getValues())) {
			move.setScore(BAD);
		}
	}

	/** 
	 * Beware this is only possible because we condition on both xj and pj
	 */
	protected BayesianFactor calcConditional(Solution sol, int[] query, Strides condition) {
	//		GraphicalModel<? extends GenericFactor> model = this.model.copy();
	//		RemoveBarren<? extends GenericFactor> barren = new RemoveBarren<>();
	//		barren.execute(model, query);
	
		int[] seq = sequence;
		for (var v: condition.getVariables()) {
			int idx = ArraysUtil.indexOf(v, seq);
			seq = ArraysUtil.remove(seq, idx);
		}

		VariableElimination<BayesianFactor> ve = new FactorVariableElimination<>(seq);
		ve.setFactors(sol.getData().valueCollection());
		ve.setNormalize(false);

		BayesianFactor bf = ve.run(query);
		Strides s = bf.getDomain().union(condition);

		// if bf's domain already correct return
		if (s.union(bf.getDomain()).getSize() == s.getSize()) return bf;

		double[] data = new double[s.getCombinations()];

		IndexIterator iter = bf.getDomain().getIterator(s);
		int i = 0;
		while(iter.hasNext()) data[i++] = bf.getValueAt(iter.next());
		return new BayesianFactor(s, data);
	}
		
	protected BayesianFactor calcMarginal(Solution sol, int[] query) {
//		GraphicalModel<? extends GenericFactor> model = this.model.copy();
//		RemoveBarren<? extends GenericFactor> barren = new RemoveBarren<>();
//		barren.execute(model, query);

		VariableElimination<BayesianFactor> ve = new FactorVariableElimination<>(sequence);
		ve.setFactors(sol.getData().valueCollection());
		ve.setNormalize(false);
		return ve.run(query);
	}

	protected BayesianFactor calcPosterior(Solution sol, int[] query, TIntIntMap ev) {
//		GraphicalModel<? extends GenericFactor> model = this.model.copy();
//		RemoveBarren<? extends GenericFactor> barren = new RemoveBarren<>();
//		barren.execute(model, query, ev);

		VariableElimination<BayesianFactor> ve = new FactorVariableElimination<>(sequence);
		ve.setFactors(sol.getData().valueCollection());
		ve.setEvidence(ev);
		ve.setNormalize(false);
		return ve.run(query);
	}

	@Override
	public int compare(Solution sol1, Solution sol2) {
		if (goal == GoalType.MINIMIZE) {
			return (int) Math.signum(sol1.getScore() - sol2.getScore());
		} else {
			return (int) Math.signum(sol2.getScore() - sol1.getScore());
		}
	}

	@Override
	public boolean isImprovement(double change) {
		if (goal == GoalType.MAXIMIZE)
			change = -change;
		return change < 0;
	}

	@Override
	public boolean isImprovement(double from, double to) {
		if (Double.isNaN(to))
			return false;
		if (Double.isNaN(from))
			return true;

		return isImprovement(to - from);
	}

	@Override
	public boolean isBound(double value) {
		if (goal == GoalType.MAXIMIZE && value >= 1.0 - EPS)
			return true;
		if (goal == GoalType.MINIMIZE && value <= 0.0 + EPS)
			return true;
		return false;
	}

}
