package ch.idsia.credici.learning.eqem;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.SingletonMap;

import ch.idsia.credici.utility.table.DataTable;
import ch.idsia.crema.model.Strides;
import gnu.trove.map.TIntIntMap;

final class Item {
	double weight;
	Map<Integer, double[]> probs;

	Item(double weight) {
		this.weight = weight;
		probs = Collections.emptyMap();
	}

	Item(int variable, double[] probs) {
		this.weight = Double.NaN; // mark as special will hopefully intercepted (see bi constructor)
		this.probs = new SingletonMap<Integer, double[]>(variable, probs);
	}

	Item(Item a, Item b) {
		if (Double.isNaN(a.weight)) {
			throw new IllegalStateException("Adding without init, Please call init first to set weight");
		}

		weight = a.weight; // use left weight
		probs = new HashMap<Integer, double[]>(a.probs);
		probs.putAll(b.probs);
	}

	// not really to be used
	Item(double weight, int variable, double[] probs) {
		this.weight = weight;
		this.probs = new SingletonMap<Integer, double[]>(variable, probs);
	}
}

/**
 * a data table that collects new columns (for exogenous variable), rather than
 * new rows/weights.
 * 
 * The weight is now a pair of weight (one per row) and a map of variable
 * probabilities. When asking for counts this will implicitly expand the table
 * to the whole permutations and sum for the required domain.
 */
public class PUTable extends DataTable<Item, Double> {
	
	protected PUTable(int[] columns) {
		super(columns, null, null, Item::new, null);
	}

	public void init(TIntIntMap addr, double weight) {
		super.add(addr, new Item(weight));
	}

	public void init(int[] addr, double weight) {
		super.add(addr, new Item(weight));
	}

	public void add(TIntIntMap addr, int variable, double[] pu) {
		super.add(addr, new Item(variable, pu));
	}

	public void add(int[] addr, int variable, double[] pu) {
		super.add(addr, new Item(variable, pu));
	}
//	
//	public void add(int[] addr, double weight, int variable, double[] pu) {
//		super.add(addr, new Item(weight, variable, pu));
//	}

	public void add(int[] order, int[] addr, int variable, double[] pu) {
		super.add(order, addr, new Item(variable, pu));
	}

	private int getPartialOffset(Strides domain, int[] vars, int[] states) {
		int offset = 0;
		for (int vid = 0; vid < vars.length; ++vid) {
			int v = vars[vid];

			int vindex = domain.indexOf(v);
			if (vindex >= 0) {
				offset += domain.getStrideAt(vindex) * states[vid];
			}
		}
		return offset;
	}
	
	public double[] getWeights(Strides domain) {
		return getWeights(domain, 0d);
	}
	
	
	/**
	 * Get the weights
	 * @param domain
	 * @param s
	 * @return
	 */
	public double[] getWeights(Strides domain, double s) {

		// all other variables must be part of the domain
		int[] cols = columns.clone();
		Arrays.sort(cols);
 		Strides exogenous = domain.remove(cols);

		double[] target = new double[domain.getCombinations()];
		for (int i = 0; i < target.length; ++i) target[i] = Math.log(s);
		
		for (var row : this) {
			int[] states = row.getKey();
			int offset = getPartialOffset(domain, columns, states);
			
			//int o2 = domain.getPartialOffset(columns, states);

			double base = row.getValue().weight;
			Map<Integer, double[]> probs = row.getValue().probs;

			var iter = domain.getIterator(exogenous);
  			while (iter.hasNext()) {
				int[] exostate = iter.getPositions().clone();
				int exoffset = iter.next();
				int[] exovars = exogenous.getVariables();
				double prob = 0;
				
				for (int eid = 0; eid < exovars.length; ++eid) {
					int ev = exovars[eid];
					int es = exostate[eid];
					
					prob += probs.get(ev)[es];
				}
				target[offset + exoffset] = LogBayesianMarginalHelper.logsum(target[offset + exoffset],Math.log(base) + prob);
			}
		}

		return target;
	}
}
