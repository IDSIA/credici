package ch.idsia.credici.learning.eqem;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoublePredicate;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang3.tuple.Pair;
//import org.apache.commons.math3.util.FastMath;
import org.apache.commons.lang3.tuple.Triple;

import com.google.common.base.Supplier;

import ch.idsia.credici.learning.eqem.ModelInfo.Stage;
import ch.idsia.credici.learning.eqem.fixing.EquationFixing;
import ch.idsia.credici.learning.eqem.fixing.MinEntropyFixing;
import ch.idsia.credici.learning.eqem.stop.LLStar;
import ch.idsia.credici.learning.eqem.stop.LLStop;
import ch.idsia.credici.learning.ve.VE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import ch.javasoft.metabolic.generate.FaColiTest;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.jafama.FastMath;

public class ComponentEM {
	public static final String LL_DATA = "LL-data";

	private Logger logger;

	private Config settings;

	private StructuralCausalModel sourceModel;
	private DoubleTable dataset;

	private TIntObjectMap<TIntSet> endoLocked;
	private EquationFixing determinismStrategy;
	
	
	private TIntSet doNotTouch;

	private int[] variables;

	private int[] sequence;

	private Randomizer random;

	private Integer id;

	public Integer getId() {
		return id;
	}

	/* default no logging and no log generation */
	private Consumer<Supplier<Info>> modelLogger = (b) -> {
	};

	/**
	 * A stop criteria that receives probabilities and computes whether the EM
	 * converged (CAN BE STATEFUL)
	 */
	private StopCriterion stop;

	public ComponentEM(StructuralCausalModel model, DoubleTable dataset, double llStar, Config settings, long seed) {
		this.random = new Randomizer(seed);
		init(model, dataset, llStar, settings);
	}

	public ComponentEM(StructuralCausalModel model, DoubleTable dataset, double llStar, Config settings) {
		random = new Randomizer(2);
		init(model, dataset, llStar, settings);
	}

	public void setModelLogger(Consumer<Supplier<Info>> mlogger) {
		this.modelLogger = mlogger;
	}

	/**
	 * Initialize the EM
	 * 
	 * @param themodel
	 */
	private void init(StructuralCausalModel themodel, DoubleTable dataset, double llStar, Config settings) {
		this.settings = settings;
		this.id = (Integer) themodel.getData(CComponents.CC_KEY);
		this.determinismStrategy = new MinEntropyFixing();
		
		this.logger = Logger.getLogger("ComponentEM");

		logger.config("Setting up inference for: " + themodel.getName() + " ("
				+ Arrays.toString(themodel.getVariables()) + ")");
		sourceModel = themodel;

		MinFillOrdering order = new MinFillOrdering();
		sequence = order.apply(themodel);

		// model may contain some additional variables (that are not to be optimized)
		variables = themodel.getExogenousVars();
		if (settings.freeEndogenous())
			variables = ArraysUtil.append(variables, themodel.getEndogenousVars(false));

		this.dataset = dataset;

		// setup stop criteria
		this.stop = new LLStar(llStar, settings.llEPS()); // MaxKL(); //
//		initCache(sourceModel);
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
			// random.uniformInplace(model.getFactor(variable), variable); // TODO
		}

		modelLogger.accept(() -> new Info().model(model).data(dataset).title("Init").runId(runid));

		doNotTouch = new TIntHashSet();
		initLocked(model);

		// reset stop criteria
		stop.reset();

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
		try {
			modelLogger.accept(() -> new Info().model(model).data(dataset).title("Close").runId(runid));
		} catch (Exception x) {
		}
	}

	/**
	 * Main optimization start. This will execute runs in sequence for a single
	 * component. Generated models will NOT be stored by this class, but will be
	 * passed to the provided model consumer.
	 * 
	 * Failed iterations will still call the resultsCallback!
	 * 
	 * 
	 * @param resultsCallback a consumer of solutions. This is used to collect
	 *                        generated models.
	 * @return true if the method did generate at least one valid model
	 * @throws InterruptedException
	 */
	public boolean run(int runid, Consumer<ModelInfo<BayesianFactor, StructuralCausalModel>> resultsCallback)
			throws InterruptedException {
		long tm = System.currentTimeMillis();
		StructuralCausalModel model = runInit(runid);
		int iteration = 0;
		modelLogger.accept(() -> new Info().model(model).title("prerun").runId(runid));
		try {
			for (iteration = 0; iteration < settings.numIterations(); ++iteration) {
				int stepid = (runid << 24) + (iteration << 16);
				boolean more = step(model, dataset, true, settings.deterministic(), stepid);

				final int tmp = iteration;
				long time = System.currentTimeMillis() - tm;
				modelLogger.accept(() -> new Info().model(model).time(time).title("poststep").runId(runid).iterations(tmp));
				if (!more)
					break;
			}

			// final
			double ll = (Double) model.getData(LL_DATA);
			resultsCallback.accept(ModelInfo.successFirst(model.copy(), ll, runid, iteration));

		} catch (UnreacheableSolutionException ex) {
			resultsCallback.accept(ModelInfo.failed(model.copy(), Stage.FIRST_FAILED, ex));
			runClose(model, runid, -1);
			return false;
		}

		// more fsc
		int pscm_runs = 0;
		for (pscm_runs = 0; pscm_runs < settings.numPSCMRuns(); pscm_runs++) {
			try {
				stop.reset();

				// rndomize exogenous distributions only!
				for (int exo : model.getExogenousVars()) {
					random.randomizeInplace(model.getFactor(exo), exo);
				}
				int pscm_iteration = 0;
				for (pscm_iteration = 0; pscm_iteration < settings.numPSCMIterations(); ++pscm_iteration) {
					int stepid = (pscm_runs << 8) + (pscm_iteration) + (runid << 24) << (iteration << 16);
					boolean more = step(model, dataset, false, settings.deterministic(), stepid);
					if (!more)
						break;
				}

				double ll = (Double) model.getData(LL_DATA);
				resultsCallback
						.accept(ModelInfo.successPSCM(model.copy(), ll, runid, iteration, pscm_runs, pscm_iteration));

			} catch (UnreacheableSolutionException ure) {
				resultsCallback.accept(ModelInfo.failed(model.copy(), Stage.FAILED_PSCM, ure));
			}
		}
		runClose(model, runid, iteration);
		return true;
	}

	/**
	 * Perform a step in the em This will return a boolean indicating whether we
	 * should continue (true) or stop (false).
	 * 
	 * @param model     the model to optimize
	 * @param data      the data to be learned from
	 * @param free_endo
	 * @param true_scm
	 * @param stepid
	 * @return
	 * @throws UnreacheableSolutionException
	 * @throws InterruptedException
	 */
	protected boolean step(StructuralCausalModel model, DoubleTable data, boolean free_endo, boolean true_scm,
			int stepid) throws UnreacheableSolutionException, InterruptedException {

		// modelLogger.accept(new Info().model(model).data(dataset).title("Expectation
		// Model (" + stepid + ", " + free_endo + ", " + true_scm + ")"));
		PUTable counts = expectation(model, data);
		if (counts == null) {
			throw new UnreacheableSolutionException();
		}
		double ll = counts.getMetadata();
		model.setData(LL_DATA, ll);

		stop.newIteration(counts.getMetadata());

		// this will change the model!!!!
		maximization(model, counts, stop, free_endo, true_scm);
		// modelLogger.accept(new
		// Info().model(model).data(dataset).title("PostMaximization ("+ stepid + ", " +
		// free_endo + ", " + true_scm + ")"));

		boolean converged = stop.converged();

		if (converged) {

			if (true_scm) {
				
				var extreme = determinismStrategy.choose(model, data, endoLocked);

				if (extreme.getLeft() == -1) {
					// nothing found, nothing else to do
					// we converged and do not have any further variable to lock
					return false;
				}

				lock(model, extreme.getLeft(), extreme.getMiddle(), extreme.getRight());

//				modelLogger.accept(() -> {
//					// what was locked (for plot)
//					var s = new SingletonMap<Integer, Set<Integer>>(extreme.getKey(),
//							Collections.singleton(extreme.getValue()));
//
//					return new Info().model(model).data(dataset).highlight(s)
//							.title("Locked (" + stepid + ", " + free_endo + ", " + true_scm + ")");
//				});

				// model changed -> no conversion yet
				return true;

			} else {
				// converged, no more steps
				return false;
			}
		}
		return true; // likely more steps
	}

	/**
	 * Compute expected counts for the exogenous variables.
	 * 
	 * @param model
	 * @param data
	 * @param stepid
	 * @return
	 * @throws InterruptedException
	 */
	public PUTable expectation(StructuralCausalModel model, DoubleTable data) throws InterruptedException {

		int[] columns = model.getEndogenousVars(true); // ArraysUtil.append(model.getEndogenousVars(),
														// model.getExogenousVars());
		double loglike = 0;

		// a new table for the results
		PUTable result = new PUTable(columns);

		for (var p : data.mapIterable()) {
			TIntIntMap observation = p.getKey();
			double w = p.getValue();
			result.init(observation, w);

			boolean first = true;

			// compute the probability for each exogenous variable
			for (int u : model.getExogenousVars()) {
				BayesianFactor phidden_obs = quasi_inference(model, u, observation);

				// Likelihood of this observation is the normalizing factor
				BayesianFactor ll = marginalize_fix(phidden_obs, u);

				// by the definition of a CComponent we always have P(E) as the
				// normalizing factor. So only the first is needed
				if (first) {
					first = false;
					double value = ll.getInteralData()[0];

					if (!ll.isLog()) {
						value = Math.log(value);
					}

					if (Double.isNaN(value) || value == Double.NEGATIVE_INFINITY) {
						// zero likelihood
						return null;
					}

					loglike += value * w;
				}

				// lets keep it in logspace
				var pu = phidden_obs.divide(ll);
				double[] dta = pu.getInteralData();
				if (!pu.isLog())
					dta = Arrays.stream(dta).map(Math::log).toArray();
				result.add(observation, u, dta);
			}
		}

		result.setMetadata(loglike);
		return result;
	}

	/**
	 * Maximization stage.
	 * 
	 * Recompute all free distributions from the imputed data Table.
	 * 
	 * @param counts
	 * @return
	 */
	protected void maximization(StructuralCausalModel model, PUTable counts, StopCriterion stop, boolean include_endo,
			boolean deterministic) {

		int[] to_maximize = include_endo ? variables : model.getExogenousVars();

		for (int variable : to_maximize) {

			// is the variable fully locked?
			if (doNotTouch.contains(variable))
				continue;

			BayesianFactor factor = model.getFactor(variable);
			Strides domain = factor.getDomain();

			int stride = domain.getStride(variable);
			int states = domain.getCardinality(variable);

			double[] original = factor.getInteralData();
			
			// compute the data over the whole variable. The locked columns will be 
			// restored next
			double[] data = counts.getWeights(domain);
			if (!factor.isLog()) {
				data = Arrays.stream(data).map(FastMath::exp).toArray();
			}

			// locked columns need their data to be restored to the original
			// values.
			if (endoLocked.containsKey(variable)) {
				TIntSet locked = endoLocked.get(variable);
				var iter = locked.iterator();

				while (iter.hasNext()) {
					int offset = iter.next();
					for (int state = 0; state < states; ++state) {
						int id = offset + state * stride;
						data[id] = original[id];
					}
				}
			}

			// check if there was an impossible column (p=0)
			// thanks Laura for the debug support (rubberduck)!!!
			factor.setInteralData(data);

			BayesianFactor norm_factor = marginalize_fix(factor, variable);
			Strides norm_domain = norm_factor.getDomain();
			double[] norm_data = norm_factor.getInteralData();

			DoublePredicate iszero = norm_factor.isLog() ? (v) -> v == Double.NEGATIVE_INFINITY : (v) -> v == 0;

			// ready to use data with internal values on the factor,
			// not just the normalizing factor.
//			double zero = 0;
//			double one = 1;

			boolean impossible = false;
			for (int i = 0; i < norm_data.length; ++i) {
				if (!iszero.test(norm_data[i]))
					continue;

				int[] cstates = norm_domain.statesOf(i);
				int offset = domain.getPartialOffset(norm_domain.getVariables(), cstates);

				impossible = true;
				double low, high;

				if (deterministic) {
					low = factor.isLog() ? Double.NEGATIVE_INFINITY : 0;
					high = factor.isLog() ? 0 : 1;
				} else {
					low = factor.isLog() ? Math.log(1.0 / states) : 1.0 / states;
					high = factor.isLog() ? Math.log(1.0 / states) : 1.0 / states;
				}
				int state = EquationFixing.argmax(factor.getInteralData(), offset, stride, states);
				lock(data, variable, states, stride, offset, state, low, high);
				norm_data[i] = norm_factor.isLog() ? 0 : 1;
			}

			if (impossible) {
				factor.setInteralData(data);
			}

			// normalize the factor to get a proper CPT
			factor = factor.divide(norm_factor);

			stop.accumulate(factor, original);

			model.setFactor(variable, factor);
		}

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
//		TIntIntMap newObs = new TIntIntHashMap(obs);
//		rem.filter(newObs);

		StructuralCausalModel infModel = model;
		TIntIntMap newObs = obs;

		VE<BayesianFactor> fve = new VE<BayesianFactor>();
		fve.setNormalize(false);
		fve.setSequence(sequence);

		return (BayesianFactor) fve.apply(infModel, query, newObs);
	}

//	private TIntObjectMap<TIntObjectMap<int[]>> cacheVars;
//
//	private void initCache(StructuralCausalModel model) {
//		cacheVars = new TIntObjectHashMap<TIntObjectMap<int[]>>();
//		for (var query : model.getExogenousVars()) {
//			var vars = new TIntObjectHashMap<int[]>();
//			cacheVars.put(query, vars);
//			for (int child : model.getEndogenousChildren(query)) {
//
//				var px = model.getFactor(child);
//				var domain = px.getDomain();
//
//				int[] vars_array = new int[domain.getSize() - 1];
//				int j = 0;
//				for (var variable : domain.getVariables()) {
//					if (variable != query) {
//						vars_array[j++] = variable;
//					}
//				}
//				vars.put(child, vars_array);
//			}
//		}
//	}

	// compute P(U,e)
	BayesianFactor quasi_inference(StructuralCausalModel model, int query, TIntIntMap obs) throws InterruptedException {
		// we have a single p(U) so query is alone
		// P(x0==0|x1==1,U)*p(x1==1)*P(U)

		TIntIntMap newObs = obs;
		int size = model.getSize(query);
		var pu0 = model.getFactor(query);

		double[] pu = pu0.getInteralData().clone();
		if (!pu0.isLog()) {
			pu = Arrays.stream(pu).map(Math::log).toArray();
			System.out.println("BAAAD");
		}
		// the dependent variables are all 1 anyway
//		for (int extra : model.getDependentSet().toArray()) {
//			var px = model.getFactor(extra);
//			var domain = px.getDomain();
//
//			int[] states = Arrays.stream(domain.getVariables()).map(newObs::get).toArray();
//			int offset = domain.getOffset(states);
//
//			double value = px.getInteralData()[offset];
//			if (!px.isLog())
//				value = Math.log(value);
//
//			for (int i = 0; i < size; ++i)
//				pu[i] += value;
//		}

		// we assume that query has no exogenous siblings
		for (int child : model.getEndogenousVars(false)) {
			var px = model.getFactor(child);
			var domain = px.getDomain();
			var stride = domain.getStride(query);

			int[] vars = new int[domain.getSize() - 1];
			int[] states = new int[vars.length];
			int j = 0;
			for (var variable : domain.getVariables()) {
				if (variable != query) {
					states[j] = newObs.get(variable);
					vars[j++] = variable;
				}
			}

			// int[] vars = Arrays.stream(domain.getVariables()).filter((v) -> v !=
			// query).toArray();
			// int[] states = Arrays.stream(vars).map(newObs::get).toArray();

			int offset = domain.getPartialOffset(vars, states);
			// P(x=e|y=e,U)
			double[] idata = px.getInteralData();
			for (int u = 0; u < size; ++u) {
				double value = idata[offset + stride * u];
//				if (!px.isLog())
//					value = FastMath.log(value);
				pu[u] += value;
			}
		}

		// data is log!
		return new BayesianFactor(model.getDomain(query), pu, true);
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
	 * Lock a variable into some state.
	 * 
	 * @param model    the model where we lock the variable
	 * @param variable the variable to be locked
	 * @param offset   the offset of the conditioning
	 */
	void lock(StructuralCausalModel model, int variable, int offset, int state) {
		BayesianFactor factor = model.getFactor(variable);
		
		// if markovian
		if (model.isMarkovian()) {
			int u = model.getExogenousParents(variable)[0];
			int exostride = factor.getDomain().getStride(u);
			
				
		} else {		
			// here check if the things are working
			int stride = factor.getDomain().getStride(variable);
			int states = factor.getDomain().getCardinality(variable);
	
			double[] data = factor.getInteralData();
			double one = factor.isLog() ? 0 : 1;
			double zero = factor.isLog() ? Double.NEGATIVE_INFINITY : 0;
	
			lock(data, variable, states, stride, offset, state, zero, one);
		}
	}

	/** 
	 * When we lock we need to check whether the problem is still reacheable. 
	 * This means that each observation needs to have a corresponding Probability
	 * 
	 * 
	 * @param data
	 * @param variable
	 * @param states
	 * @param stride
	 * @param offset
	 * @param top
	 * @param low
	 * @param high
	 */
	void lock(double[] data, int variable, int states, int stride, int offset,int top, double low, double high) {

		
		for (int state = 0; state < states; ++state) {
			data[state * stride + offset] = state == top ? high : low;
		}


		endoLocked.get(variable).add(offset);
	}

	

	public BayesianFactor marginalize_fix(BayesianFactor factor, int variable) {
		Strides domain = factor.getDomain();
		int offset = domain.indexOf(variable);
		if (offset == -1)
			return factor;

		if (factor.isLog()) {
			final int size = domain.getSizeAt(offset);
			final int stride = domain.getStrideAt(offset);

			LogBayesianMarginalHelper collector = new LogBayesianMarginalHelper(size, stride);

			final int[] new_variables = new int[domain.getSize() - 1];
			final int[] new_sizes = new int[domain.getSize() - 1];

			System.arraycopy(domain.getVariables(), 0, new_variables, 0, offset);
			System.arraycopy(domain.getVariables(), offset + 1, new_variables, offset, new_variables.length - offset);

			System.arraycopy(domain.getSizes(), 0, new_sizes, 0, offset);
			System.arraycopy(domain.getSizes(), offset + 1, new_sizes, offset, new_variables.length - offset);

			final int reset = size * stride;

			int source = 0;
			int next = stride;
			int jump = stride * (size - 1);

			Strides target_domain = new Strides(new_variables, new_sizes);
			final double[] new_data = new double[target_domain.getCombinations()];

			for (int target = 0; target < target_domain.getCombinations(); ++target, ++source) {
				if (source == next) {
					source += jump;
					next += reset;
				}

				new_data[target] = collector.collect(factor.getInteralData(), source);
			}

			return new BayesianFactor(target_domain, new_data, true);
		} else
			return factor.marginalize(variable);
	}

}
