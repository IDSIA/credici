package ch.idsia.credici.learning.eqem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoublePredicate;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.FastMath;

import ch.idsia.credici.learning.ve.VE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.dot.DetailedDotSerializer;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.utility.Probability;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

class ComponentEM {
	private Logger logger;
	
	private boolean equations = true;
	
	int numRuns = 1;
	int maxIterations = 1000;

	
	private StructuralCausalModel sourceModel;
	private DoubleTable dataset;

	private TIntObjectMap<TIntSet> endoLocked;
	private TIntSet doNotTouch;

	private int[] variables;

	private int[] sequence;

	private Randomizer random;
 

	public ComponentEM(StructuralCausalModel model, DoubleTable dataset, long seed) {
		this.random = new Randomizer(seed);
		init(model, dataset);
	}

	public ComponentEM(StructuralCausalModel model, DoubleTable dataset) {
		random = new Randomizer();
		init(model, dataset);
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
	
	/**
	 * Initialize the EM
	 * 
	 * @param themodel
	 */
	private void init(StructuralCausalModel themodel, DoubleTable dataset) {
		this.logger = Logger.getLogger("ComponentEM");

		logger.config("Setting up inference for: " + themodel.getName() + " ("
				+ Arrays.toString(themodel.getVariables()) + ")");
		sourceModel = themodel;

		MinFillOrdering order = new MinFillOrdering();
		sequence = order.apply(themodel);

		// model may contain some additional variables (that are not to be optimized)
		variables = ArraysUtil.append(themodel.getExogenousVars(), themodel.getEndogenousVars());
		
		this.dataset = dataset;

		BayesianNetwork network = themodel.getEmpiricalNet(dataset.toMap(false));
		double ll = Probability.maxLogLikelihood(themodel, dataset.toMap(false));
		
	
		
		
	}
	
	/**
	 * Initialize a run of the EM, called for each restart.
	 * 
	 * @param runid
	 * @return
	 */
	private StructuralCausalModel runInit(int runid) {
		logger.config("Starting new run (#" + runid + ") on " + sourceModel.getName());

		StructuralCausalModel model = sourceModel.copy();
		for (int variable : variables) {
			random.randomizeInplace(model.getFactor(variable), variable);
		}

		doNotTouch = new TIntHashSet();
		initLocked(model);

		likelihoods = new TDoubleArrayList();

		oldll = Double.NEGATIVE_INFINITY;
		return model;
	}

	/**
	 * Close the run. Called once run has converged/ended
	 * 
	 * @param model
	 * @param runid
	 * @param iterations
	 */
	protected void runClose(StructuralCausalModel model, int runid, int iterations) {
		System.out.println(Arrays.toString(likelihoods.toArray()));
	}

	
	/**
	 * Main optimization start. This will execute runs in sequence
	 * 
	 * @param data
	 * @throws InterruptedException
	 */
	public void run(Consumer<StructuralCausalModel> resultsCallback) throws InterruptedException {
		for (int r = 0; r < numRuns; ++r) {
			StructuralCausalModel model = runInit(r);
			try {
				int iteration;
				for (iteration = 0; iteration < maxIterations; ++iteration) {
					int stepid = (r << 16) + iteration;
					boolean more = step(model, dataset, equations, stepid);
					if (!more)
						break;
				}
				runClose(model, r, iteration);
				resultsCallback.accept(model);
			} catch (UnreacheableSolutionException ex) {
				runClose(model, r, -1);

			}
		}
	}

	
	public boolean step(StructuralCausalModel model, DoubleTable data, boolean free_endo, int stepid) throws UnreacheableSolutionException, InterruptedException {
		PUTable counts = expectation(model, data, stepid);
		if (counts == null) {
			throw new UnreacheableSolutionException();
		}
		
		
		NumberFormat f = new DecimalFormat("0000000000");

		DetailedDotSerializer.saveModel(model, data, f.format(stepid) + "_0.png");
		boolean converged = maximization(model, counts, free_endo, stepid);
		DetailedDotSerializer.saveModel(model, null, f.format(stepid) + "_1.png");

		likelihoods.add(counts.getMetadata());
		if (converged) {
			DetailedDotSerializer.saveModel(model, null, f.format(stepid) + "_2.png");

			var extreme = getExtreme(model);
			System.out.println("Lock: " + extreme.getLeft() + "->" + extreme.getRight());

			if (extreme.getKey() == -1) {
				// nothing found, nothing else to do
				// we converged and do not have any further variable to lock
				return false;
			}
			lock(model, extreme.getKey(), extreme.getValue());

			var s = new SingletonMap<Integer, Set<Integer>>(extreme.getKey(),
					Collections.singleton(extreme.getValue()));

			DetailedDotSerializer.saveModel(model, null, f.format(stepid) + "_3.png", s);
		}
		return true;
	}

	
	
	
	
	public PUTable expectation(StructuralCausalModel model, DoubleTable data, int stepid) throws InterruptedException {

		int[] columns = model.getEndogenousVars(true); // ArraysUtil.append(model.getEndogenousVars(),
														// model.getExogenousVars());
		double loglike = 0;

		// a new table for the results
		PUTable result = new PUTable(columns);

		for (Pair<TIntIntMap, Double> p : data.mapIterable()) {
			TIntIntMap observation = p.getLeft();
			double w = p.getRight();
			result.init(observation, w);

			boolean first = true;

			// compute the probability for each exogenous variable
			for (int u : model.getExogenousVars()) {
				BayesianFactor phidden_obs = inference(model, u, observation);

				// Likelihood of this observation is the normalizing factor
				BayesianFactor ll = phidden_obs.marginalize(u);
				System.out.println(observation + ""+FastMath.log(ll.getData()[0]) * w);
				
				// by the definition of a CComponent we always have P(E) as the
				// normalizing factor. So only the first is needed
				if (first) {
					first = false;
					if (ll.getData()[0] == 0) {
						// impossible case
						return null;
					}
					loglike += FastMath.log(ll.getData()[0]) * w;
				}

				final double[] dta = phidden_obs.divide(ll).getData();

				result.add(observation, u, dta);
			}
		}

		result.setMetadata(loglike);
		return result;
	}

	private TDoubleList likelihoods;
	private double oldll = -1;

	/**
	 * Maximization stage.
	 * 
	 * Recompute all free distributions from the imputed data Table.
	 * 
	 * @param counts
	 * @return
	 */
	public boolean maximization(StructuralCausalModel model, PUTable counts, boolean include_endo, int stepid) {

		int[] to_maximize = include_endo ? variables : model.getExogenousVars();

		for (int variable : to_maximize) {

			// is the variable fully locked?
			if (doNotTouch.contains(variable))
				continue;

			BayesianFactor factor = model.getFactor(variable);
			Strides domain = factor.getDomain();

			int stride = domain.getStride(variable);
			int states = domain.getCardinality(variable);

			double[] original = factor.getData();
			double[] data = counts.getWeights(domain);

			// locked columns need their data to be restore to the original
			// values.
			@SuppressWarnings("unused")
			int changed = 0;
			if (endoLocked.containsKey(variable)) {
				TIntSet locked = endoLocked.get(variable);
				var iter = locked.iterator();

				while (iter.hasNext()) {
					int offset = iter.next();
					for (int state = 0; state < states; ++state) {
						int id = offset + state * stride;
						data[id] = original[id];
					}
					++changed;
				}
			}

			// check if there was an impossible column
			// thanks Laura for the debug support (rubberduck)!!!

			factor.setData(data);

			BayesianFactor norm_factor = factor.marginalize(variable);
			Strides norm_domain = norm_factor.getDomain();
			double[] norm_data = norm_factor.getInteralData();

			DoublePredicate iszero = norm_factor.isLog() ? (v) -> v == Double.NEGATIVE_INFINITY : (v) -> v == 0;

			// ready to use data with internal values on the factor,
			// not just the normalizing factor.
			double zero = 0;
			double one = 1;

			boolean impossible = false;
			for (int i = 0; i < norm_data.length; ++i) {
				if (!iszero.test(norm_data[i]))
					continue;

				int[] cstates = norm_domain.statesOf(i);
				int offset = domain.getPartialOffset(norm_domain.getVariables(), cstates);
				int fix = argmax(data, offset, stride, states);

				for (int state = 0; state < states; ++state) {
					data[offset + state * stride] = fix == state ? one : zero;
				}
				norm_data[i] = norm_factor.isLog() ? 0 : 1;
				impossible = true;
			}

			if (impossible) {
				factor.setData(data);
			}

			// normalize the factor to get a proper CPT
			factor = factor.divide(norm_factor);

			model.setFactor(variable, factor);
		}

		double diff = Math.abs(oldll - counts.getMetadata());

		oldll = counts.getMetadata();
		return diff <= 0.001;
	}

	/**
	 * Perform a bayesian inference of type P(Query,e)
	 * 
	 * considerations:
	 * <ul>
	 * <li>since we are using the weighted counts there is no two same observation
	 * sets. No no chance to cache on that
	 * <li>we expect to be working on ccomponent, so no filtering of vars needed
	 * </ul>
	 *
	 * <p/>
	 * With CComponents, running cut observed and remove barren could be done once
	 * and then simply replace the factor in the children of the dependent
	 * variables.
	 *
	 * @param model
	 * @param query
	 * @param obs
	 * @return
	 * @throws InterruptedException
	 */
	BayesianFactor inference(StructuralCausalModel model, int query, TIntIntMap obs) throws InterruptedException {
//		StructuralCausalModel infModel = (StructuralCausalModel) new CutObserved().execute(model, obs);
//		RemoveBarren rem = new RemoveBarren();
//		infModel = rem.execute(infModel, query, obs);

//		TIntSet vars = new TIntHashSet(infModel.getVariables());
//		vars.retainAll(obs.keySet());
		StructuralCausalModel infModel = model;
		TIntIntMap newObs = new TIntIntHashMap(obs);
		//rem.filter(newObs);

//		for (int x : vars.toArray()) {
//			newObs.put(x, obs.get(x));
//		}

		VE<BayesianFactor> fve = new VE<BayesianFactor>();
		fve.setNormalize(false);
		fve.setSequence(sequence);

		return (BayesianFactor) fve.apply(infModel, query, newObs);
	}

	/**
	 * Initialize the list of locked columns
	 */
	private void initLocked(StructuralCausalModel model) {
		endoLocked = new TIntObjectHashMap<TIntSet>();

		for (int endo : model.getEndogenousVars()) {
//			int[] parents = model.getParents(endo);
//			Strides conditioning = model.getDomain(parents);
			endoLocked.put(endo, new TIntHashSet());
		}
	}

	/**
	 * Find argmax of the data in the specified array using strides and size to
	 * advance.
	 * 
	 * The data can be normal or in log space.
	 * 
	 * @param data   double[] the source data
	 * @param offset int start offset
	 * @param stride int item stride
	 * @param size   int number of items
	 * @return the state of the variable with max value
	 */
	int argmax(double[] data, int offset, int stride, int size) {
		int index = 0;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < size; ++i) {
			int idx = offset + stride * i;
			if (data[idx] > max) {
				max = data[idx];
				index = i;
			}
		}
		return index;
	}

	/**
	 * Lock a variable into some state.
	 * 
	 * @param model    the model where we lock the variable
	 * @param variable the variable to be locked
	 * @param offset   the offset of the conditioning
	 */
	void lock(StructuralCausalModel model, int variable, int offset) {
		BayesianFactor factor = model.getFactor(variable);
		int stride = factor.getDomain().getStride(variable);
		int states = factor.getDomain().getCardinality(variable);

		double[] data = factor.getInteralData();

		int top = argmax(data, offset, stride, states);

		double one = factor.isLog() ? 0 : 1;
		double zero = factor.isLog() ? Double.NEGATIVE_INFINITY : 0;

		for (int i = 0; i < states; ++i) {
			int id = i * stride + offset;
			data[id] = i == top ? one : zero;
		}
		endoLocked.get(variable).add(offset);
		// System.out.println("Lock " + variable + " - " + offset);
	}

	/**
	 * Get the most exreme endogenous distribution. This will look for the column in
	 * each endogenous factor that is closest to be deterministic. And which is not
	 * already deterministic (locked).
	 * 
	 * Method returns a pair (variable, column offset)
	 * 
	 * @return
	 */
	Pair<Integer, Integer> getExtreme(StructuralCausalModel model) {
		int best_var = -1;

		double best_score = Double.MAX_VALUE;
		int best_offset = 0;

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
				}
			}
		}
		return Pair.of(best_var, best_offset);
	}

	/**
	 * Get the min mae of the current column from the nearest deterministic
	 * function.
	 * 
	 * In the deterministic function only one state will have p=1 all other will
	 * have p=0; The method traverses the column only once. It will not look for the
	 * maximum first, but will continuously correct the score if a higher p is
	 * found.
	 * 
	 * @param factor        the factor
	 * @param column_offset the column to be considered
	 * @param stride        the stride of the variable
	 * @param states        the number of strates of the variable
	 * @return
	 */
	@SuppressWarnings("unused")
	private double scoreExtremeMae(BayesianFactor factor, int column_offset, int stride, int states) {
		// start with an impossibly high value for both score and high_delta.
		// at the first state we will cancel them out
		double score = 2;
		double high_delta = score;
		double low_delta = 0;

		for (int state = 0; state < states; ++state) {
			double v = factor.getValueAt(column_offset + state * stride);
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
			double v = factor.getValue(column_offset + state * stride);
			if (v == 0)
				continue; // 0log0 == 0
			score += v * FastMath.log(v);
		}
		return -score * l2; // / Math.log(2); // to get log2 results

		// same with streams
//		IntStream indices = IntStream.iterate(0, s -> s < states, s -> s + 1).map(state -> column_offset + state * stride);
//		return indices.mapToDouble(factor::getValueAt).filter(v -> v > 0).map(v -> v * FastMath.log(v)).sum();
	}
	
	
	// getters and setters
	
	
	
	public void setOptimizeEquations(boolean equations) {
		this.equations = equations;
	}
	
	public boolean isOptimizeEquations() {
		return this.equations;
	}
	
	public void setMaxIterations(int iters) {
		this.maxIterations = iters;
	}
	
	public int getMaxIterations() {
		return this.maxIterations;
	}
	
	public void setNumberRuns(int runs) {
		this.numRuns = runs;
	}
}
