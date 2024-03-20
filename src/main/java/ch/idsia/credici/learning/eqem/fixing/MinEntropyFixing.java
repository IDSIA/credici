package ch.idsia.credici.learning.eqem.fixing;

import org.apache.commons.lang3.tuple.Triple;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;
import net.jafama.FastMath;

public class MinEntropyFixing implements EquationFixing {

	@Override
	public Triple<Integer, Integer, Integer> choose(StructuralCausalModel model, DoubleTable data, TIntObjectMap<TIntSet> endoLocked) {
		int best_var = -1;

		double best_score = Double.MAX_VALUE;
		int best_offset = 0;
		int best_state = 0;

		for (int endo : model.getEndogenousVars()) {
			if (!endoLocked.containsKey(endo))
				continue;

			TIntSet locked = endoLocked.get(endo);

			BayesianFactor factor = model.getFactor(endo);
			Strides domain = factor.getDomain();
			Strides parents = domain.remove(endo);

			int stride = domain.getStride(endo);
			int states = domain.getCardinality(endo);

			var iterator = domain.getIterator(parents);
			while (iterator.hasNext()) {
				int offset = iterator.next();
				if (locked.contains(offset))
					continue;

				double score = scoreExtreme(factor, offset, stride, states);
				if (score < best_score) {
					best_score = score;
//					best_parents_states = iterator.getPositions().clone();
					best_offset = offset;
					best_var = endo;
					best_state = EquationFixing.argmax(factor.getInteralData(), offset, stride, states);
				}
			}
		}
		return Triple.of(best_var, best_offset, best_state);
	}
	
	/**
	 * Get the entropy of the specified column.
	 * 
	 * @param factor        the factor
	 * @param column_offset the column to be considered
	 * @param stride        the stride of the variable
	 * @param states        the number of strates of the variable
	 * @return
	 */
	private double scoreExtreme(BayesianFactor factor, int column_offset, int stride, int states) {
		final double l2 = 1. / FastMath.log(states);
		double score = 0;
		for (int state = 0; state < states; ++state) {
			double v = factor.getValueAt(column_offset + state * stride);
			if (v == 0)
				continue; // 0log0 == 0
			score += v * FastMath.log(v);
		}
		return -score * l2; // / Math.log(2); // to get log2 results

		// same with streams (too slow)
//		IntStream indices = IntStream.iterate(0, s -> s < states, s -> s + 1).map(state -> column_offset + state * stride);
//		return indices.mapToDouble(factor::getValueAt).filter(v -> v > 0).map(v -> v * FastMath.log(v)).sum();
	}

}
