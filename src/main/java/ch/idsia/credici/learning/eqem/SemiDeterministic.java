package ch.idsia.credici.learning.eqem;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.user.core.Variable;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;





/**
 * Given an {@link StructuralCausalModel} DAG structure this Transformer
 * generates a model with partial deterministic equations. These will guarantee
 * the surjectivity of the model.
 */
public class SemiDeterministic {

	public StructuralCausalModel applyMarkvian(StructuralCausalModel model, DoubleTable data, TIntObjectMap<TIntSet> locked) {
		StructuralCausalModel sm = model.copy();
		for (var endo : sm.getEndogenousVars()) {
			// there is only one in markovian n
			int exo = sm.getExogenousParents(endo)[0];
			int exo_size = sm.getSize(exo);
			int endo_size = sm.getSize(endo);
			
			var factor = sm.getFactor(endo);
			
			var zero = factor.isLog() ? Double.NEGATIVE_INFINITY : 0;
			var one = factor.isLog() ? 0 : 1;
			
			var domain = factor.getDomain();
			
			int exo_stride = domain.getStride(exo);
			int endo_stride = domain.getStride(endo);
			
			// lock in the first parent configuration with an identity configuration
			TIntSet offsets = new TIntHashSet();
			
			double[] factor_data = factor.getInteralData();
			for (int exo_state = 0; exo_state < endo_size; ++exo_state) {
				int exo_offset = exo_state * exo_stride;
				
				offsets.add(exo_offset);
				
				// set a state to one. 
				factor_data[exo_offset + exo_state * endo_stride] = one;
				
				// all other states are set to zero
				for (int endo_state = 0; endo_state < endo_size; ++endo_state) {
					if (endo_state == exo_state) continue;
					factor_data[exo_offset + endo_state * endo_stride] = zero;
				}
			} 
			
			locked.put(endo, offsets);
		}
		return sm;
	}
	
	
	/**
	 * Every U controls a number of Endogenous variables. 
	 * We need to focus only on endogenous variables. No extra vars, they are not controllable.
	 * 
	 * We need to cover the whole dataset in terms of endogenous instantiations
	 */
	public StructuralCausalModel apply(StructuralCausalModel model, DoubleTable data, TIntObjectMap<TIntSet> locked) {
		if (model.isMarkovian()) {
			return applyMarkvian(model, data, locked);
		} else {
			throw new UnsupportedOperationException();
		}
	}

}
