package ch.idsia.credici.learning.eqem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.table.Table;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.CombinationsIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

class ComponentEM {
	private StructuralCausalModel model;
	private TIntObjectMap<TIntSet> endoLocked;
	private int[] trainableVars;
	private JoinInference<BayesianFactor, BayesianFactor> inferenceEngine;
	
	public ComponentEM(StructuralCausalModel model) {
		this.model = model;
		init();
		initLocked();
	}
	
	private void init() {
		trainableVars = model.getExogenousVars();
	}
	
	public void step(Table data) throws InterruptedException {
		Table counts = expectation(data);
		maximization(counts);
	}
	
	public Table expectation(Table data) throws InterruptedException {
		int[] columns = ArraysUtil.append(model.getEndogenousVars(), model.getExogenousVars());
		
		Table result = new Table(columns);
		for (Pair<TIntIntMap, Double> p : data.mapIterable()) {
			TIntIntMap observation = p.getLeft();
			double w = p.getRight();

			List<Collection<Pair<Integer, Double>>> states = new ArrayList<Collection<Pair<Integer, Double>>>();
			
			for (int u : model.getExogenousVars()) {
				BayesianFactor phidden_obs = posteriorInference(u, observation);
				double[] dta = phidden_obs.getData();
				
				// phidden_obs = phidden_obs.scalarMultiply(w);
				int size = model.getSize(u);
				if (dta.length != size) {
					System.err.println("HALT");
				}
				
				Collection<Pair<Integer, Double>> var_states = 
						IntStream.range(0, size).<Pair<Integer, Double>>mapToObj(i->Pair.of(i, dta[i])).collect(Collectors.toList());
				states.add(var_states);
			}
			
			// the posteriors must now be combined into many rows
			CombinationsIterator<Pair<Integer, Double>> iter = new CombinationsIterator<Pair<Integer, Double>>(states);
			while (iter.hasNext()) {
				List<Pair<Integer, Double>> row = iter.next();
				TIntIntMap new_obs = new TIntIntHashMap(observation);
				
				double weight = w;
				for (int i = 0; i < trainableVars.length; ++i) {
					int variable = trainableVars[i];
					Pair<Integer, Double> d = row.get(i);
					int state = d.getKey();
					
					new_obs.put(variable, state);
					
					double probab = d.getValue();
					weight *= probab;
				}
				
				result.add(new_obs, weight);
			}
		}
		return result;
	}
	
	
	
	public void maximization(Table counts) {
		
	}
	

	BayesianFactor posteriorInference(int query, TIntIntMap obs) throws InterruptedException {
		// considerations:
		// - since we are using the weighted counts there is no two same observation sets. No no chance to cache on that
		// - we expect to be working on ccomponent, so no filtering of vars needed
		
		// String cacheKey = getKey(query, obs);
		
		StructuralCausalModel infModel = (StructuralCausalModel) new CutObserved().execute(model, obs);
		infModel = new RemoveBarren().execute(infModel, query, obs);

		
		TIntIntMap newObs = new TIntIntHashMap();
		for (int x : obs.keys()) {
			if (ArraysUtil.contains(x, infModel.getVariables())) {
				newObs.put(x, obs.get(x));
			}
		}
		
		return inferenceEngine.apply(infModel, query, newObs); // P(U|X=obs)
	}
	
	
	/**
	 * Initialize the list of locked columns
	 */
	private void initLocked() {
		endoLocked = new TIntObjectHashMap<TIntSet>();
		
		for (int endo : model.getEndogenousVars()) {
			int[] parents = model.getParents(endo);
			Strides conditioning = model.getDomain(parents);
			endoLocked.put(endo, new TIntHashSet());
		}
	}
	/**
	 * Find argmax of the data in the specified array using strides and size to advance
	 * 
	 * @param data double[] the source data
	 * @param offset int start offset
	 * @param stride int item stride
	 * @param size int number of items
	 * @param absolute return relative or absolute index
	 * @return 
	 */
	int argmax(double[] data, int offset, int stride, int size, boolean absolute) {
		int index = 0;
		double max = Double.NEGATIVE_INFINITY;
		for (int i =0 ; i<size; ++i) {
			int idx = offset + stride * i;
			if (data[idx] > max) {
				max = data[idx];
				index = i;
			}
		}
		return absolute ? index * stride + offset : index;
	}

	
	void lock(int variable, int offset) {
		BayesianFactor factor = model.getFactor(variable);
		int stride = factor.getDomain().getStride(variable);
		int states = factor.getDomain().getCardinality(variable);
		
		double[] data = factor.getInteralData();

		int top = argmax(data, offset, stride, states, false);
		
		double one = factor.isLog() ? 0 : 1;
		double zero = factor.isLog() ? Double.NEGATIVE_INFINITY : 0;
		
		for (int i = 0; i < states; ++i) {
			int id = i * stride + offset;
			data[id] = i == top ? one : zero;
		}
		endoLocked.get(variable).add(offset);
	}
	
	
	/**
	 * Get the most exreme endogenous distribution.
	 * This will look for the column in each endogenous factor that is closest to be deterministic.
	 * And which is not already deterministic (locked).
	 * 
	 * Method returns a pair (variable, column offset)
	 * @return
	 */
	Pair<Integer, Integer> getExtreme() {
		int best_var = -1; 
		
		double best_score = Double.MAX_VALUE;
//		int[]  best_parents_states = null;
		int    best_offset = 0;
		
		for (int endo : model.getEndogenousVars()) {
			if (!endoLocked.containsKey(endo)) continue;
			
			BayesianFactor factor = model.getFactor(endo);
			Strides parents = model.getDomain(model.getParents(endo));
			
			int stride = factor.getDomain().getStride(endo);
			int states = factor.getDomain().getCardinality(endo);
			
			var iterator = factor.getDomain().getIterator(parents);
			while(iterator.hasNext()) {
				int offset = iterator.next();
				if (endoLocked.get(endo).contains(offset)) continue;
				
				double score = scoreExtreme(factor, offset, stride, states);
				if (score < best_score) {
					best_score = score;
//					best_parents_states = iterator.getPositions().clone();
					best_offset = offset;
					best_var = endo;
				}
			}
		}
		return Pair.of(best_var, best_offset);
	}
	
	
	/**
	 * Get the min mae of the current column from the nearest deterministic function. 
	 * 
	 * @param factor
	 * @param column_offset
	 * @param stride
	 * @param states
	 * @return
	 */
	private double scoreExtreme(BayesianFactor factor, int column_offset, int stride, int states) {
		// start with an impossibly high value for both score and high_delta.
		// at the first state we will cancel them out
		double score = 2;
		double high_delta = score;
		double low_delta = 0;
		
		for (int i = 0; i < states; ++i) {
			double v = factor.getValueAt(column_offset + i * stride);
			double h = 1 - v;
			if (h < high_delta) {
				// found a better winner, fix score
				score -= high_delta;
				score += low_delta;
				
				// this is the winner now
				score += h;
				
				// remember values to be able to cancel the winner out
				high_delta = h;
				low_delta = v;
				
			} else { 
				// not a winning state
				score += v;
			}					
		}
		return score / states;
	}
}
