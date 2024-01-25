package ch.idsia.credici.model.transform;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.traverse.TopologicalOrderIterator;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class EmpiricalNetwork {
	
	public BayesianNetwork apply(StructuralCausalModel model, DoubleTable data) {
		BayesianNetwork bn = new BayesianNetwork();
		
		var comps = components(model);
		
		int[] topo_order = topological(model);
		
		for (int variable: topo_order) {
			bn.addVariable(variable, model.getSize(variable));
		}
		
		for (int i = 0; i < topo_order.length; ++i) {
			int variable = topo_order[i];
			
			// topologically before 
			var topo_before = Arrays.stream(topo_order, 0, i);
			var friends = comps.get(variable);
			
			// get all nodes strictly topologically before variable and that are part of the component
			int[] parents = topo_before.filter(friends::contains).toArray();
			bn.addParents(variable, parents);
		}
		
		quantify(bn, data);

		return bn;
	}
	
	

	public double loglikelihood(BayesianNetwork net, DoubleTable dataset) {
		
		int[] vars = net.getVariables();
		
		double ll = 0;
		for (var row : dataset) {
			int[] states = row.getKey();
			double rowll = 0;
			for(int variable : vars) {
				var factor = net.getFactor(variable);
				int offset = factor.getDomain().getPartialOffset(dataset.getColumns(), states);
				double p = factor.getValueAt(offset);
				rowll += Math.log(p);
			}
			ll += rowll * row.getValue();
		}
		return ll;
	}

	
	
	private void quantify(BayesianNetwork bn, DoubleTable data) {

		for (int variable : bn.getVariables()) {
			int[] parents = bn.getParents(variable);
			
			int[] target = new int[parents.length + 1];
			System.arraycopy(parents, 0, target, 0, parents.length);
			target[parents.length] = variable;
			Arrays.sort(target);
			
			Strides domain = bn.getDomain(target);
			double[] values = data.getWeights(domain.getVariables(), domain.getSizes());
			
			BayesianFactor factor = new BayesianFactor(domain, values, false);
			factor = factor.normalize(parents);
			
			bn.setFactor(variable, factor);
		}
	}
	
	private TIntObjectMap<TIntSet> components(StructuralCausalModel model) {
	
		List<TIntSet> components = new LinkedList<TIntSet>();
		
		TIntSet todo = new TIntHashSet(model.getVariables());
		int[] exogenous = model.getExogenousVars();
		TIntSet exo_close = new TIntHashSet();
		
		for (int exo : exogenous) {
			// already processed
			if(exo_close.contains(exo)) continue;
			exo_close.add(exo);
			
			// visit connected components
			TIntSet component = new TIntHashSet(model.getEndogenousChildren(exo));
			components.add(component);
			
			TIntSet open = new TIntHashSet(component);
			
			while(!open.isEmpty()) {
				int child = open.iterator().next();
				open.remove(child);
				
				TIntSet exo_parents = new TIntHashSet(model.getExogenousParents(child));
				exo_parents.removeAll(exo_close);
				for (int connected_exo_var: exo_parents.toArray()) {
					// for each non closed exo var 
					// add all children to open
					TIntSet siblings = model.getChildrenSet(connected_exo_var);
					// remove already processed children
					siblings.removeAll(component);
					open.addAll(siblings);
					
					component.addAll(siblings);
				}
			}
		}
		
		// assign sets to all variables
		TIntObjectMap<TIntSet> sets = new TIntObjectHashMap<TIntSet>();
		for (TIntSet component : components) {
			component.forEach((int variable) -> {
					for (int i : component.toArray()) {
						int[] p = model.getEndegenousParents(true, i);
						// component include all parents of connected endogenous nodes.
						component.addAll(p);
					}
					sets.put(variable, component);
					return true;
			});
		}
		return sets;
	}
	
	
	
	public int[] topological(StructuralCausalModel model) {
//		return DAGUtil.getTopologicalOrder(model.getEndogenousDAG());

		int[] endo = model.getEndogenousVars(true);
		TIntLinkedList open_list = new TIntLinkedList();
		
		for (int variable : endo) {
			if (model.getEndegenousParents(true, variable).length == 0) {
				open_list.add(variable);
			}
		}
		
		int[] order = new int[endo.length];
		int position = 0;
		
		TIntSet closed = new TIntHashSet();
		
		while(!open_list.isEmpty()) {
			// get first item
			TIntIterator iter = open_list.iterator();
			int item = iter.next();
			iter.remove();
			
			if (closed.contains(item)) continue;
			
			int[] parents_array = model.getEndegenousParents(true, item);
			if (closed.containsAll(parents_array)) {
				// all parents processed 
				closed.add(item);
				order[position++] = item;
			}
				
			open_list.add(model.getChildren(item));
		}
		
		return order;
	}
}
