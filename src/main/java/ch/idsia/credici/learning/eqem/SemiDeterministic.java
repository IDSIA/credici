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
import gnu.trove.set.TIntSet;

/**
 * Given an {@link StructuralCausalModel} DAG structure this Transformer
 * generates a model with partial deterministic equations. These will guarantee
 * the surjectivity of the model.
 */
public class SemiDeterministic implements BiFunction<StructuralCausalModel, DoubleTable, StructuralCausalModel> {

	public StructuralCausalModel applyMarkvian(StructuralCausalModel model, DoubleTable data) {
		StructuralCausalModel sm = model.copy();
		for (var endo : sm.getEndogenousVars()) {
			
		}
		return sm;
	}
	
	/**
	 * Every U controls a number of Endogenous variables. 
	 * We need to focus only on endogenous variables. No extra vars, they are not controllable.
	 * 
	 * We need to cover the whole dataset in terms of endogenous instantiations
	 */
	@Override
	public StructuralCausalModel apply(StructuralCausalModel model, DoubleTable data) {
		
		StructuralCausalModel newmodel = model.copy();
		
		TopologicalOrderIterator<Integer, DefaultEdge> order = new TopologicalOrderIterator<>(model.getNetwork());
		TIntList topo = new TIntArrayList(model.getVariablesCount() - model.getExogenousSet().size());

		// collect the endogenous topological ordering
		Predicate<Integer> isEndo = model::isEndogenous;
		StreamSupport.stream(Spliterators.spliteratorUnknownSize(order, Spliterator.ORDERED), false)
				.filter(isEndo).forEach(topo::add);

		TreeMap<int[], TIntSet> assignments = new TreeMap<int[], TIntSet>(Arrays::compare);
		int[] Ustates = IntStream.range(0, data.size()).toArray();
		
		
		//assignments.put(new int[0], );
		
		TIntList done = new TIntArrayList(topo.size());
		
		for (int variable : topo.toArray()) {
			BayesianFactor factor = model.getFactor(variable);
			if (factor == null) {
				factor = new BayesianFactor(model.getFullDomain(variable));
			} else {
				factor = factor.copy();
			}
			
			//makeDeterministic(factor, )
			
			done.add(variable);
		}

		return null;
	}

}
