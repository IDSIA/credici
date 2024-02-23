package ch.idsia.credici.model.transform;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.StructuralCausalModel.VarType;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class EmpiricalNetwork implements BiFunction<StructuralCausalModel, DoubleTable, BayesianNetwork>{
	
	TIntSet dependant;
	public BayesianNetwork apply(StructuralCausalModel model, DoubleTable data) {
		var m2 = model.copy();

		// dependent variable are third party components
		dependant = new TIntHashSet(m2.getDependentSet());
		for (int v : dependant.toArray()) {
			int u = m2.addVariable(2, VarType.EXOGENOUS);
			m2.addParent(v, u);
			m2.setVariableType(v, VarType.ENDOGENOUS);
		}
		
		for (int variable : model.getEndogenousVars()) {
			if(model.getParents(variable).length == 0) {
				dependant.add(variable);
			}
		}
		
		// just because rafa's method requires distributions set
		Randomizer r = new Randomizer();
		for (var v : m2.getVariables()) {
			var factor = r.randomFactor(m2.getFullDomain(v), v, false, false); // domain is already sorted
			m2.setFactor(v, factor);
		}
		
		
		BayesianNetwork bn = m2.getEmpiricalNet();
		DetailedDotSerializer.saveModel("ban.png", new Info().model(bn).hideTables());
		quantify(bn, data, bn.getVariables());
		
	//	DetailedDotSerializer.saveModel("ban.png", new Info().model(bn));
//		for (int w : model.getDependentSet().toArray()) {
//			Strides domain = model.getFullDomain(w);
//			BayesianFactor bf = new BayesianFactor(domain, true);
//			bn.setFactor(w, bf);
//		}
		
	//	DetailedDotSerializer.saveModel("bn.png", new Info().model(bn));
		
		return bn;
	}
	
	
//	public BayesianNetwork apply(StructuralCausalModel model, DoubleTable data) {
//		BayesianNetwork bn = new BayesianNetwork();
//		
//		var comps = components(model);
//		
//		int[] topo_order = topological(model);
//		
//		for (int variable: topo_order) {
//			bn.addVariable(variable, model.getSize(variable));
//		}
//		
//		for (int i = 0; i < topo_order.length; ++i) {
//			int variable = topo_order[i];
//			
//			// topologically before 
//			var topo_before = Arrays.stream(topo_order, 0, i);
//			var friends = comps.get(variable);
//			
//			// get all nodes strictly topologically before variable and that are part of the component
//			int[] parents = topo_before.filter(friends::contains).toArray();
//			bn.addParents(variable, parents);
//		}
//		
//		quantify(bn, data);
//
//		return bn;
//	}
	
	

	public double loglikelihood(BayesianNetwork net, DoubleTable dataset) {
		
		int[] vars = net.getVariables();
		
		int[] cols = dataset.getColumns();
		int[] order = ArraysUtil.order(cols);
		cols = ArraysUtil.at(cols, order);

		double ll = 0;
		for (var row : dataset.sorted()) {
			int[] states = ArraysUtil.at(row.getKey(), order);
			double rowll = 0;
			for(int variable : vars) {
				var factor = net.getFactor(variable);
				double[] idata = factor.getInteralData();
				
				int offset = factor.getDomain().getPartialOffset(cols, states);
				double p = idata[offset];
				
				if (!factor.isLog())
					rowll += Math.log(p);
				else 
					rowll += p;
			}
			ll += rowll * row.getValue();
		}
		return ll;
	}

	
	
	
	private void quantify(BayesianNetwork bn, DoubleTable data, int[] variables) {

		for (int variable : variables) {
			int[] parents = bn.getParents(variable);
			int[] target = new int[parents.length + 1];
			System.arraycopy(parents, 0, target, 0, parents.length);
			target[parents.length] = variable;
			Arrays.sort(target);
			
			Strides domain = bn.getDomain(target);
			double[] values;
			BayesianFactor factor;
			if (dependant.contains(variable)) {
//				double lsize = - Math.log(domain.getCardinality(variable)); // log(1/size)
				values = new double[domain.getCombinations()];
				
				//by default arrays are initialized to zero
//				for (int i = 0; i < values.length;++i) {
//					values[i] = 0;//lsize;
//				}
				factor = new BayesianFactor(domain, values, true);
			} else {
				values = data.getWeights(domain.getVariables(), domain.getSizes());
				for (int i = 0; i < values.length; i++) {
					values[i] = Math.log(values[i]);
				}
				factor = new BayesianFactor(domain, values, true);
				factor = factor.normalize(parents);
			}
			
			bn.setFactor(variable, factor);
		}
	}
	
	
	private TIntObjectMap<TIntSet> components(StructuralCausalModel model) {
	
		List<TIntSet> components = new LinkedList<TIntSet>();
		
		TIntSet todo = new TIntHashSet(model.getVariables());
		
		TIntSet exogenous = new TIntHashSet(model.getExogenousSet());
		TIntSet exo_close = new TIntHashSet();
		while (!exogenous.isEmpty()) {
			int exo = exogenous.iterator().next();
			exogenous.remove(exo);
			
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
				
				// we are processing the exogenous parents
				exogenous.removeAll(exo_parents);
				exo_close.addAll(exo_parents);
				
				// add to the open variables all the children of the exogenous 
				for (int connected_exo_var : exo_parents.toArray()) {
					
					// for each non closed exo var 
					// add all children to open
					TIntSet siblings = model.getChildrenSet(connected_exo_var);
					// remove already processed children
					siblings.removeAll(component);
					open.addAll(siblings);
					
					component.addAll(siblings);
				}
			}
			
			// add all parents of endogenous variables
			int[] compvars = component.toArray();
			for (int variable : compvars) {
				var parents = model.getParentsSet(variable);
				parents.removeAll(model.getExogenousSet());
				component.addAll(parents);
			}
		}
		

		// assign sets to all variables
		TIntObjectMap<TIntSet> sets = new TIntObjectHashMap<TIntSet>();
		
		for (TIntSet component : components) {
			
			
		
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
