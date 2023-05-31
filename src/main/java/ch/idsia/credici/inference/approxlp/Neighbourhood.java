package ch.idsia.credici.inference.approxlp;

import ch.idsia.credici.collections.FIntHashSet;
import ch.idsia.credici.collections.FIntObjectHashMap;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.ExtensiveLinearToRandomBayesianFactor;
import ch.idsia.crema.factor.convert.SeparateLinearToRandomBayesian;
import ch.idsia.crema.factor.credal.linear.ExtensiveLinearFactor;
import ch.idsia.crema.factor.credal.linear.SeparateLinearFactor;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.search.NeighbourhoodFunction;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.List;

public class Neighbourhood implements NeighbourhoodFunction<Move, Solution> {

	private int[] freeable;
	private final GraphicalModel<? extends GenericFactor> model;

	public Neighbourhood(GraphicalModel<? extends GenericFactor> model, int... locked) {
		this.model = model;
		initialize(new FIntHashSet(locked));
	}

	@Override
	public List<Move> neighbours(Solution solution) {
		ArrayList<Move> moves = new ArrayList<>();
		int free = solution.getFree();
		for (int var : freeable) {
			if (var != free) {
				moves.add(new Move(var));
			}
		}
		return moves;
	}

	@Override
	public Solution random() {
		TIntObjectMap<BayesianFactor> factors = new FIntObjectHashMap<>();
		for (int var : model.getVariables()) {
			// lets assume the model has factors for all variables!
			BayesianFactor r = random(model.getFactor(var));
			if (r instanceof BayesianFactor)
				r = ((BayesianFactor) r).replace(0.0, ApproxLP1.EPS);
			factors.put(var, r);
		}
		return new Solution(factors, Double.NaN);
	}

	@Override
	public Solution move(Solution from, Move doing) {
		return new Solution(from, doing);
	}

	private BayesianFactor random(GenericFactor factor) {
		if (factor instanceof ExtensiveLinearFactor) {
			return new ExtensiveLinearToRandomBayesianFactor().apply((ExtensiveLinearFactor<?>) factor);
		// TODO: issue #98, make this work without non-negative constraints
		//} else if (factor instanceof SeparateHalfspaceFactor) {
		//	return new HalfspaceToRandomBayesianFactor().apply((SeparateHalfspaceFactor) factor);
		} else if (factor instanceof SeparateLinearFactor) {
			return new SeparateLinearToRandomBayesian().apply((SeparateLinearFactor<?>) factor);
		} else if (factor instanceof BayesianFactor) {
			return (BayesianFactor) factor;
		}
		throw new IllegalArgumentException("Unsupported class for random generation: " + factor.getClass());
	}

	private void initialize(TIntSet locked) {
		TIntArrayList freeableVariable = new TIntArrayList();
		for (int var : model.getVariables()) {
			// locked variables are not to be freed
			if (locked.contains(var)) continue;

			GenericFactor factor = model.getFactor(var);
			if (factor != null) {
				if (factor instanceof ExtensiveLinearFactor ||
						factor instanceof SeparateLinearFactor) {
					freeableVariable.add(var);
				}
			} else {
				// vacuous
				freeableVariable.add(var);
			}
		}

		freeable = freeableVariable.toArray();
	}
}
