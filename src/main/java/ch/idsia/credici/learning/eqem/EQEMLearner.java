package ch.idsia.credici.learning.eqem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import ch.idsia.credici.learning.eqem.ComponentSolution.Stage;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class EQEMLearner {
	
	
	
	private Config settings = new Config(10, 1000, 10, 1000, true);

	private Function<String, BiConsumer<String, Info>> loggerGenerator = EQEMLearner::noCallbackGenerator;
	private BiConsumer<String, Info> modelLogger;
	
	private StructuralCausalModel model;
	private StructuralCausalModel prior;
	private Randomizer random;
	private DoubleTable data;

	
	public EQEMLearner(StructuralCausalModel prior, DoubleTable data, TIntIntMap exosizes) {
		this.random = new Randomizer(0);
		this.prior = prior;
		this.model = initModel(prior, exosizes);
		this.data = data;
	}

	public void setConfig(Config config) {
		this.settings = config;
	}
	
	public CComponents run() throws InterruptedException {
		modelLogger = loggerGenerator.apply("EQEM");
		modelLogger.accept("prior", new Info().model(this.prior).title("Prior model"));
		modelLogger.accept("model", new Info().model(this.model).title("Starting model"));
		
		CComponents cc = new CComponents();

		List<Pair<StructuralCausalModel, DoubleTable>> components = cc.apply(model, data);
		List<Pair<ComponentEM, Double>> em = new ArrayList<>();
		
		for (var pair : components) {
			StructuralCausalModel component_model = pair.getLeft();
			DoubleTable component_data = pair.getRight();
			
			ComponentEM cem = new ComponentEM(pair.getKey(), pair.getValue(), settings, 0);
			cem.setModelLogger(loggerGenerator.apply("CC" + em.size()));
			
			EmpiricalNetwork en = new EmpiricalNetwork();
			BayesianNetwork network = en.apply(component_model, component_data);

			double ll2 = en.loglikelihood(network, component_data);
			em.add(Pair.of(cem, ll2));
		}
		
		System.out.println(settings);
		
		for (int r = 0 ; r < settings.numRuns(); ++r) {
			for (var pair : em) {
				AtomicInteger accepted = new AtomicInteger(0);
				AtomicInteger rejected = new AtomicInteger(0);

				Map<Stage, Integer> counts = Collections.synchronizedMap(new HashMap<Stage, Integer>());
				ComponentEM cem = pair.getLeft();
				final double llmax = pair.getRight();
				
				TDoubleList diffs = new TDoubleArrayList();
				
				cem.run(r, (sol) -> {
					if (sol.stage.success()) {
						diffs.add(llmax - sol.loglikelihood);
						if (llmax - sol.loglikelihood < 0.0000001) {
							cc.addResult(sol.model);	
							accepted.incrementAndGet();
						} else {
							rejected.incrementAndGet();
						}
					}

					counts.compute(sol.stage, (stage, cnt)->cnt == null ? 1: cnt+1);
				});
				double m = Double.NaN;
				if (!diffs.isEmpty()) m = diffs.max();
				
				System.out.println(//m + " " +
						cem.getId() + counts + " "  + r + " " + accepted.get() + " / " + (accepted.get() + rejected.get()));
			}
		}
		
		return cc;
	}

	/**
	 * Initialize the model for a new run
	 * @param prior
	 * @param exosizes
	 * @return
	 */
	private StructuralCausalModel initModel(StructuralCausalModel prior, TIntIntMap exosizes) {
		StructuralCausalModel model = prior.copy();
		// new StructuralCausalModel(prior.getName());

//		for (int variable : prior.getVariables()) {
//			int size = prior.getSize(variable);
//			if (exosizes.containsKey(variable)) {
//				size = exosizes.get(variable);
//			}
//
//			boolean exo = prior.isExogenous(variable);
//			model.addVariable(variable, size, exo);
//		}
//
//		for (int variable : prior.getVariables()) {
//			int[] parents = prior.getParents(variable);
//			model.addParents(variable, parents);
//		}

		// create random factors for all variables
		for (int variable : model.getVariables()) {
			int[] domain = ArraysUtil.append(new int[] { variable }, model.getParents(variable));
			Arrays.sort(domain);

			Strides dom = model.getDomain(domain);
			BayesianFactor factor = new BayesianFactor(dom);
			random.randomizeInplace(factor, variable);
			model.setFactor(variable, factor);
		}

		return model;
	}

	public static void main(String[] args) {

		int N = 5000;
//		int numRun = 1000; // EM internal iterations
//		int maxIter = 1000;

		int n = 4;
		int endoVarSize = 2;
		int exoVarSize = 5;

		RandomUtil.setRandomSeed(1000);
		StructuralCausalModel causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);

		TIntIntMap[] data = IntStream.range(0, N).mapToObj(i -> causalModel.sample(causalModel.getEndogenousVars()))
				.toArray(TIntIntMap[]::new);

		
		int[] exo = causalModel.getExogenousVars();
		Strides dom = causalModel.getDomain(exo);
		int[] sz = dom.getSizes();

		DoubleTable dataTable = new DoubleTable(data);
		DetailedDotSerializer.saveModel("input.png", new Info().model(causalModel).data(dataTable));

		EQEMLearner learner = new EQEMLearner(causalModel, dataTable, new TIntIntHashMap(exo, sz));
		try {
			learner.run();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	

	
	/**
	 * Set a callback function called at each step of the learner execution
	 * The function is passed into each component.
	 */
	public void setDebugLoggerGenerator(Function<String, BiConsumer<String, Info>> generator) {
		this.loggerGenerator = generator;
	}
	
	
	private static void noCallback(String model, Info data) {	
	}
	
	private static BiConsumer<String, Info> noCallbackGenerator(String name) {
		return EQEMLearner::noCallback;
	}

}
