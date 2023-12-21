package ch.idsia.credici.learning;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.CausalInfo;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.learning.DiscreteEM;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.tuple.Pair;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.time.StopWatch;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EquationCausalEM extends DiscreteEM<EquationCausalEM> {

	private HashMap<String, BayesianFactor> posteriorCache = new HashMap<>();

	private boolean usePosteriorCache = true;

	private TIntObjectHashMap<BayesianFactor> replacedFactors = null;

	private HashMap<String, StructuralCausalModel> modelCache = new HashMap<>();

	StopCriteria stopCriteria = StopCriteria.KL;

	protected double klthreshold = Double.NaN;

	private double threshold = 0.0;

	public enum StopCriteria {
		KL, L1, LLratio
	}

	public EquationCausalEM(StructuralCausalModel model,
			JoinInference<BayesianFactor, BayesianFactor> inferenceEngine) {
		this.inferenceEngine = inferenceEngine;
		this.priorModel = model;
		this.trainableVars = CausalInfo.of((StructuralCausalModel) priorModel).getExogenousVars();
	}

	public EquationCausalEM(GraphicalModel<BayesianFactor> model, int[] elimSeq) {
		this.inferenceEngine = getDefaultInference(model, elimSeq);
		this.priorModel = model;
		this.trainableVars = CausalInfo.of((StructuralCausalModel) priorModel).getExogenousVars();
	}

	public EquationCausalEM(GraphicalModel<BayesianFactor> model) {
		this(model, (new MinFillOrdering()).apply(model));
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

	protected void stepPrivate(@SuppressWarnings("rawtypes") Collection stepArgs) throws InterruptedException {
		// E-stage
		@SuppressWarnings("unchecked")
		TIntObjectMap<BayesianFactor> counts = expectation((Pair<TIntIntMap, Long>[]) stepArgs.toArray(Pair[]::new));

		// M-stage
		maximization(counts);
	}

	protected Pair<TIntIntMap,Long>[] expectation(Pair<TIntIntMap, Long>[] dataWeighted)
			throws InterruptedException {

	
		
//		TIntObjectMap<BayesianFactor> counts = new TIntObjectHashMap<>();
//		for (int variable : posteriorModel.getVariables()) {
//			counts.put(variable, new BayesianFactor(posteriorModel.getFactor(variable).getDomain(), false));
//		}

		clearPosteriorCache();
		// devo generare la congiunta delle P(U|row)
		// poi devo estendere la tabella con la P(U)
		for (Pair<TIntIntMap, Long> p : dataWeighted) {
			TIntIntMap observation = p.getLeft();
			long w = p.getRight();

			// expecting ccomponents

			
			StructuralCausalModel scm = (StructuralCausalModel) posteriorModel;
			int[] e = scm.getExogenousVars();
			
	
			
			// Case with missing data
			BayesianFactor phidden_obs = posteriorInference(e, observation);
			phidden_obs = phidden_obs.scalarMultiply(w);
				
			
		}

		return null;
	}

	private int[] getCC()
	void maximization(TIntObjectMap<BayesianFactor> counts) {

		replacedFactors = new TIntObjectHashMap<BayesianFactor>();
		updated = false;

		for (int var : trainableVars) {
			BayesianFactor countVar = counts.get(var);

			BayesianFactor f = countVar.divide(countVar.marginalize(var));

			// Store the previous factor and set the new one
			replacedFactors.put(var, posteriorModel.getFactor(var));
			posteriorModel.setFactor(var, f);
		}
		
		// for all other variables we have the counts as they are in the table. 
		// however as the distributions are conditional the counts must be spread.
		
		
		// Determine which trainable variables should not be trained anymore
		if (stopAtConvergence)
			for (int[] exoCC : getTrainableExoCC())
				if (hasConverged(exoCC))
					trainableVars = ArraysUtil.difference(trainableVars, exoCC);

	}

	private List<int[]> getTrainableExoCC() {
		return ((StructuralCausalModel) posteriorModel).exoConnectComponents().stream()
				.filter(c -> Arrays.stream(c).allMatch(u -> ArraysUtil.contains(u, trainableVars)))
				.collect(Collectors.toList());
	}

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

	
	BayesianFactor posteriorInference(int[] query, TIntIntMap obs) throws InterruptedException {
		// considerations:
		// - since we are using the weighted counts there is no two same observation sets. No no chance to cache on that
		// - we expect to be working on since ccomponent, so not filtering of vars needed
		
		StructuralCausalModel infModel = (StructuralCausalModel) new CutObserved().execute(posteriorModel, obs);
		infModel = new RemoveBarren().execute(infModel, query, obs);

		TIntIntMap newObs = new TIntIntHashMap();
		for (int x : obs.keys())
			if (ArraysUtil.contains(x, infModel.getVariables()))
				newObs.put(x, obs.get(x));
		return inferenceEngine.apply(infModel, query, newObs); // P(U|X=obs)
	}




	void clearPosteriorCache() {
		posteriorCache.clear();
	}

	public EquationCausalEM usePosteriorCache(boolean active) {
		this.usePosteriorCache = active;
		return this;
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
