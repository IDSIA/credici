package ch.idsia.credici.model.eqmc;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.factor.EquationOps;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.learning.eqem.EQEMLearner;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.StructuralCausalModel.VarType;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder.SelectionPolicy;
import ch.idsia.credici.model.predefined.RandomChainMarkovian;
import ch.idsia.credici.model.transform.Canonical;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.logger.PDFLoggerGenerator;
import ch.idsia.credici.utility.sample.Sampler;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class Experiments {

	public static void main(String[] args) throws InterruptedException {
		markovian_chain(args);
	}


	public static void markovian_chain(String[] args) throws InterruptedException {
		var random = new Random();
		int l = random.nextInt(2, 7);
		System.out.print(l + ",");
			StructuralCausalModel model = Canonical.LOG.apply(RandomChainMarkovian.buildModel(l, 2, -1));
			DetailedDotSerializer.saveModel("chain.png", new Info().model(model));
			int[] roots = Arrays.stream(model.getEndogenousVars())
					.filter(a -> model.getEndogenousParents(a).length == 0).toArray();
			int cause = roots[0];
			int[] leaves = model.getLeaves();
			int effect = leaves[0];

			Sampler sample = new Sampler();
			var data = sample.sample(model, 5000, model.getEndogenousVars());

			var cano = Canonical.LOG.apply(model);
			var ccve = runccve(cano, data, cause, effect);
			System.out.println(array2string(ccve) + ",");
			var emcc = runemcc(cano, data, cause, effect, 100);
			System.out.println(array2string(emcc) + ",");

			Config config_emcc = new Config().deterministic(false).freeEndogenous(false).numRun(5000).maxRun(30000).numIterations(10000).numPSCMRuns(0).numPSCMInterations(10000);
			var emmc = Experiments.runrelax(cano, data, cause, effect, config_emcc, null, 10000, "scmem.png");
			System.out.println("EMCC: " + Experiments.array2string(emmc));

			
			Config config = new Config().deterministic(false).numRun(4000).numIterations(10000).numPSCMRuns(0);
			var relax = runrelax(model, data, cause, effect, config, 0, 1000, null);
			System.out.println(array2string(relax) + ",");

			config = new Config().deterministic(true).numRun(100).maxRun(400).numIterations(10000).numPSCMRuns(0);
			var dete = runrelax(model, data, cause, effect, config, 0, 1000, null);
			System.out.print(array2string(dete));
			System.out.println();

	}

	public static StructuralCausalModel canonical(StructuralCausalModel cm) {
		return Canonical.LOG.apply(cm);
	}

	public static double[] runrelax(StructuralCausalModel model, DoubleTable data, int cause, int effect, Config config,
			int delta_size, int networks, String file) throws InterruptedException {

		StringBuilder asize = new StringBuilder();
		StringBuilder csize = new StringBuilder();

		TIntIntMap sizes = new TIntIntHashMap();
		boolean first = true;
		for (int exo : model.getExogenousVars()) {
			int child = model.getEndogenousChildren(exo)[0];

			int[] parents = model.getEndegenousParents(child);
			int comb = model.getDomain(parents).getCombinations();
			int canonical = (int) Math.max(2, (Math.pow(model.getSize(child), comb) - delta_size));
			if (!first) {
				asize.append(", ");
				csize.append(", ");
			}
			first = false;
			csize.append(exo).append("=").append(canonical);

			sizes.put(exo, canonical);
		}
		System.out.print("\"{" + csize.toString() + "}\",");
		DetailedDotSerializer.saveModel("model.png", new Info().model(model));
		return runrelax(model, data, cause, effect, config, sizes, networks, file);
	}

	public static double[] runrelax(StructuralCausalModel model, DoubleTable data, int cause, int effect, Config config,
			TIntIntMap sizes, int networks, String file) throws InterruptedException {

		Map<Integer, Integer> winner = new HashMap<>();
		Map<Integer, Integer> failed = new HashMap<>();
		Map<Integer, Integer> rejected = new HashMap<>();
		BiFunction<Integer, Integer, Integer> increment = (k, v) -> {
			return v == null ? 1 : v + 1;
		};

		EQEMLearner relax = new EQEMLearner(model, data, sizes, true, config);
		//relax.setDebugLoggerGenerator(new PDFLoggerGenerator("run", config.deterministic() ? "dete" : "bn"));
		
		var cc = relax.run((sol) -> {

			if (sol.getStage().success()) {
				if (sol.getLLmax() - sol.getLogLikelihood() < config.llEPS()) {
					winner.compute(sol.getComponentId(), increment);
				} else {
					rejected.compute(sol.getComponentId(), increment);
				}
			} else {
				failed.compute(sol.getComponentId(), increment);
			}
		});

		System.out.print("\"" + winner + "\",");
		System.out.print("\"" + rejected + "\",");
		System.out.print("\"" + failed + "\",");

		System.out.print("\"{" + cc.getResults().entrySet().stream().map(e -> {
			return e.getKey() + "=" + e.getValue().size();
		}).collect(Collectors.joining(", ")) + "}\",");

		cc.simplify();

		System.out.print("\"{" + cc.getResults().entrySet().stream().map(e -> {
			return e.getKey() + "=" + e.getValue().size();
		}).collect(Collectors.joining(", ")) + "}\",");

		var iter = cc.sobolIterator();
		double[] range = new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
		for (int i = 0; i < networks && iter.hasNext(); ++i) {
			StructuralCausalModel m = log2standard(iter.next());
			if (i == 0 && file != null) {
				DetailedDotSerializer.saveModel(file, new Info().model(m).data(data));
			}
			CausalVE cve = new CausalVE(m);
			var bf = cve.probNecessityAndSufficiency(cause, effect);
			double v = bf.getData()[0];
			range = minmax(range, v);
		}

		return range;
	}

	public static double[] runemcc(StructuralCausalModel model, DoubleTable data, int cause, int effect, int runs)
			throws InterruptedException {

		var emcc = EMCredalBuilder.of(model, data.toMap(false)).setMaxEMIter(1000).setNumTrajectories(runs)
				.setWeightedEM(true).setVerbose(true).setSelPolicy(SelectionPolicy.LAST).build();
		CausalMultiVE cvme = new CausalMultiVE(emcc.getSelectedPoints());

		var factor = (VertexFactor) cvme.probNecessityAndSufficiency(cause, effect);
		return getPNS(factor);
	}

	public static double[] runccve(StructuralCausalModel model, DoubleTable data, int cause, int effect)
			throws InterruptedException {
		CredalCausalVE ccve = new CredalCausalVE(model);
		var x = ccve.probNecessityAndSufficiency(cause, effect);
		return getPNS(x);
	}

	public static StructuralCausalModel log2standard(StructuralCausalModel input) {
		StructuralCausalModel m = input.copy();
		for (var variable : m.getVariables()) {
			var factor = m.getFactor(variable);
			if (factor.isLog()) {
				double[] data = factor.getData();
				var f2 = new BayesianFactor(factor.getDomain(), data, false);
				m.setFactor(variable, f2);
			}
		}
		return m;
	}

	public static String array2string(double[] interval) {
		var df = DecimalFormat.getInstance();
		return Arrays.stream(interval).mapToObj(df::format).collect(Collectors.joining(","));
	}

	public static double[] minmax(double[] range, double value) {
		range[0] = Math.min(range[0], value);
		range[1] = Math.max(range[1], value);
		return range;
	}

	public static double[] getPNS(VertexFactor v) {

		double[][] vertices = v.getVertices();
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (double[] val : vertices) {
			min = Math.min(min, val[0]);
			max = Math.max(max, val[0]);
		}
		return new double[] { min, max };
	}
}
