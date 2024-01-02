package ch.idsia.credici.learning;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.CausalInfo;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.credici.utility.table.Table;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.learning.DiscreteEM;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.Domain;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.CombinationsIterator;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.summary.Product;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.time.StopWatch;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EquationCausalEM extends DiscreteEM<EquationCausalEM> {

	private HashMap<String, BayesianFactor> posteriorCache = new HashMap<>();

	private TIntObjectHashMap<BayesianFactor> replacedFactors = null;

	private HashMap<String, StructuralCausalModel> modelCache = new HashMap<>();

	StopCriteria stopCriteria = StopCriteria.KL;

	protected double klthreshold = Double.NaN;

	private double threshold = 0.0;

	public enum StopCriteria {
		KL, L1, LLratio
	}

	public EquationCausalEM(GraphicalModel<BayesianFactor> model, int[] elimSeq) {
		this.inferenceEngine = getDefaultInference(model, elimSeq);
		this.priorModel = model;
	}

	public EquationCausalEM(StructuralCausalModel model) {
		//this(model, (new MinFillOrdering()).apply(model));
		this.priorModel = model;
		this.trainableVars = model.getExogenousVars();
	}

	private void init() {

		if (!Double.isNaN(klthreshold)) {
			throw new IllegalArgumentException(
					"The usage of klthreshold is not allowed anymore. Use threshold instead.");
		}

		if (!inline)
			this.posteriorModel = priorModel.copy();
		else
			this.posteriorModel = priorModel;

		if (trainableVars == null)
			trainableVars = posteriorModel.getVariables();

		if (recordIntermediate) {
			intermediateModels = new ArrayList<GraphicalModel<BayesianFactor>>();
			addIntermediateModels(priorModel);
		}

	}

	public void run(Collection stepArgs, int iterations) throws InterruptedException {

		// setData((TIntIntMap[]) stepArgs.toArray(TIntIntMap[]::new));

		Pair<TIntIntMap, Long>[] dataWeighted = DataUtil.getCounts(data);
		stepArgs = Arrays.asList(dataWeighted);

		StopWatch watch = null;
		if (verbose) {
			watch = new StopWatch();
			watch.start();
		}

		init();
		for (int i = 1; i <= iterations; i++) {
			if (verbose) {
				if (i % 10 == 0) {
					watch.stop();
					long time = watch.getTime();
					Logger.getGlobal().debug(i + " EM iterations in " + time + " ms.");
					watch.reset();
					watch.start();
				}
			}
			step(stepArgs);
			if (trainableVars.length == 0)
				break;

		}
		if (verbose && !watch.isStopped())
			watch.stop();

	}

	public void step(Collection stepArgs) throws InterruptedException {
		stepPrivate(stepArgs);
		performedIterations++;
		if (recordIntermediate)
			addIntermediateModels(posteriorModel);

	}

	
	protected void stepPrivate(Collection stepArgs) throws InterruptedException {
		
		// E-stage
		Table counts = expectation((Table) stepArgs);

		// M-stage
		maximization(counts);
	}

	protected Table expectation(Table dataWeighted) throws InterruptedException {
	
		clearCache();
		
		// expecting ccomponents
		
		int[] columns = ArraysUtil.append(dataWeighted.getColumns(), trainableVars);
		
		Table goal = new Table(columns);
		
		// devo generare la marginal posterior delle P(U|row)
		for (Pair<TIntIntMap, Double> p : dataWeighted.mapIterable()) {
			TIntIntMap observation = p.getLeft();
			double w = p.getRight();

			List<Collection<Pair<Integer, Double>>> states = new ArrayList<Collection<Pair<Integer, Double>>>();
			
			for (int u : trainableVars) {
				BayesianFactor phidden_obs = posteriorInference(u, observation);
				double[] dta = phidden_obs.getData();
				
				// phidden_obs = phidden_obs.scalarMultiply(w);
				int size = posteriorModel.getSize(u);
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
				
				goal.add(new_obs, weight);
			}
			
		}

		return goal;
	}


	
	void maximization(Table counts) {

		replacedFactors = new TIntObjectHashMap<BayesianFactor>();
		updated = false;
		
		// exogenous first
		for (int var : trainableVars) {
			BayesianFactor bf = posteriorModel.getFactor(var);
			Strides d = bf.getDomain();
			double[] weights = counts.getWeights(d.getVariables(), d.getSizes());
			bf = new BayesianFactor(d, weights);
			BayesianFactor f = bf.divide(bf.marginalize(var));

			// Store the previous factor and set the new one
			replacedFactors.put(var, posteriorModel.getFactor(var));
			posteriorModel.setFactor(var, f);
		}
		
		// for all other variables we have the counts as they are in the table. 
		// however as the distributions are conditional the counts must be spread.
		
		// Determine which trainable variables should not be trained anymore
		if (stopAtConvergence)
//			for (int[] exoCC : getTrainableExoCC())
//				if (hasConverged(exoCC))
//					trainableVars = ArraysUtil.difference(trainableVars, exoCC);
			trainableVars = ArraysUtil.difference(trainableVars, trainableVars);

	}
//
//	private List<int[]> getTrainableExoCC() {
//		return ((StructuralCausalModel) posteriorModel).exoConnectComponents().stream()
//				.filter(c -> Arrays.stream(c).allMatch(u -> ArraysUtil.contains(u, trainableVars)))
//				.collect(Collectors.toList());
//	}

	private boolean hasConverged(int... exoCC) {
		if (stopCriteria == StopCriteria.KL) {
			return TrajectoryAnalyser.hasConvergedKL((StructuralCausalModel) posteriorModel, replacedFactors, threshold,
					exoCC);
		} else if (stopCriteria == StopCriteria.L1) {
			return TrajectoryAnalyser.hasConvergedL1((StructuralCausalModel) posteriorModel, replacedFactors, threshold,
					exoCC);
		} else if (stopCriteria == StopCriteria.LLratio) {
			return TrajectoryAnalyser.hasConvergedLLratio((StructuralCausalModel) posteriorModel, data, threshold,
					exoCC);

		} else {
			throw new IllegalArgumentException("Wrong stopping Criteria");
		}

	}

	@Override
	public EquationCausalEM setTrainableVars(int[] trainableVars) {

		for (int v : trainableVars)
			if (!CausalInfo.of((StructuralCausalModel) priorModel).isExogenous(v)) {
				String msg = "Only exogenous variables can be trainable. Error with " + v;
				Logger.getGlobal().severe(msg);
				throw new IllegalArgumentException(msg);
			}

		return this;
	}

	private String getKey(int query, TIntIntMap obs) {
		StringBuilder key = new StringBuilder();
		key.append(query);
		key.append("--");
		obs.forEachEntry((k,v) -> {
			key.append(k+"="+v+",");
			return true;
		});
		return key.toString();
	}
	
	
	BayesianFactor posteriorInference(int query, TIntIntMap obs) throws InterruptedException {
		// considerations:
		// - since we are using the weighted counts there is no two same observation sets. No no chance to cache on that
		// - we expect to be working on since ccomponent, so not filtering of vars needed
		
		// String cacheKey = getKey(query, obs);
		
		StructuralCausalModel infModel = (StructuralCausalModel) new CutObserved().execute(posteriorModel, obs);
		infModel = new RemoveBarren().execute(infModel, query, obs);

		
		TIntIntMap newObs = new TIntIntHashMap();
		for (int x : obs.keys()) {
			if (ArraysUtil.contains(x, infModel.getVariables())) {
				newObs.put(x, obs.get(x));
			}
		}
		
		return inferenceEngine.apply(infModel, query, newObs); // P(U|X=obs)
	}




	private void clearCache() {
		posteriorCache.clear();
		modelCache.clear();
	}


	protected TIntIntMap[] data = null;

	protected void setData(TIntIntMap[] data) {
		this.data = data;
	}

	public EquationCausalEM setStopCriteria(StopCriteria stopCriteria) {
		this.stopCriteria = stopCriteria;
		return this;
	}

	public EquationCausalEM setThreshold(double threshold) {
		this.threshold = threshold;
		return this;
	}

}
