package ch.idsia.credici.model.eqmc;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.learning.eqem.EQEMLearner;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.RandomMarkovian;
import ch.idsia.credici.model.transform.Canonical;
import ch.idsia.credici.model.transform.PNS;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.sample.Sampler;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "experiments", mixinStandardHelpOptions = true, version = "v1.0.0", header = "Encrypt FILE(s), or standard input, to standard output or to the output file.")
public class MarkovianDeteExperiements {

	@Option(names = { "-o", "--output" }, description = "Output file ")
	private File outfile;

	@Option(names = { "-l", "--lleps" }, description = "Max ll", type = Double.class, defaultValue = "1e-9")
	private double ll = 1e-9;

	@Option(names = { "-r", "--runs" }, description = "Max runs", type = Integer.class, defaultValue = "500")
	private int maxrun = 500;

	@Option(names = { "-n", "--nodes" }, description = "number of nodes", type = Integer.class, defaultValue = "10")
	private int nodes = 10;

	@Option(names = { "-i", "--indegree" }, description = "max indegree", type = Integer.class, defaultValue = "4")
	private int indegree = 4;

	@Option(names = { "-x",
			"--executions" }, description = "number of executions", type = Integer.class, defaultValue = "500")
	private int repetitions = 500;

	@Option(names = { "-s", "--seed" }, description = "random seed", type = Integer.class)
	private Integer seed;

	public static void main(String[] args) throws InterruptedException {
		MarkovianDeteExperiements exp = new MarkovianDeteExperiements();
		CommandLine cl = new CommandLine(exp);
		var res = cl.parseArgs(args);
		exp.experiment();
	}

	RandomMarkovian rm;
	Random gen = new Random();
	Randomizer rz;
	
	public void experiment() {
		if (seed == null) {
			seed =  (int) System.nanoTime();
		}
		gen = new Random(seed);
		rm = new RandomMarkovian();
		rm.setSeed(gen.nextLong());
		rm.setNumberMaxArcs(indegree * nodes);
		rm.setNumberNodes(nodes);
		rm.setNumberMaxOutDegree(indegree);

		rz = new Randomizer(gen.nextLong());

		for (int i = 0; i < repetitions; ++i) {
			try {
				compute();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	
	
	
	
	public void compute() throws InterruptedException {
		
		// slow 12
		// nope: 123,
		// for (int seed : new int[] { 9959, 12412, 412, 32, 423 }) {

		rm.setSeed(gen.nextLong());
		
		StructuralCausalModel model = rm.generate(nodes, -1, true);

		var b = rm.getEndogenousBoundaries(model);
		int[] r = b.getLeft();
		int[] l = b.getRight();

		int rindex = gen.nextInt(r.length);
		int lindex = gen.nextInt(l.length);

		int cause = r[rindex];
		int effect = l[lindex];

		
		StructuralCausalModel reduced_model = model.copy();

		
		Sampler s = new Sampler(seed++);
		var data = s.sample(reduced_model, 1000, reduced_model.getEndogenousVars());

		DetailedDotSerializer.saveModel("test.png", new Info().model(model));

		for (int limit_states = 5; limit_states < 50; limit_states += 20) {
			// int runs = 50;

			// very loosely 10**i/3 for integer i's
			TIntIntMap sizes = new TIntIntHashMap();
			for (int v : model.getExogenousVars()) {
				int ch = model.getEndogenousChildren(v)[0];
				int[] p = model.getEndegenousParents(ch);

				int pc = model.getDomain(p).getCombinations();
				int chs = model.getSize(ch);

				int max = (int) Math.pow(chs, pc);

				int states = Math.max(chs, Math.min(max, limit_states));
				sizes.put(v, states);
			}

			var settings = new Config().seed(seed).llEPS(ll).numRun(maxrun).numIterations(100000).numPSCMRuns(0)
					.deterministic(true).alpha(0.001);
			var mm1 = pns(model, settings, cause, effect, data, sizes);

			settings = settings.deterministic(false);
			var mm2 = pns(model, settings, cause, effect, data, sizes);

			System.out.print("PNS " + sizes + "(" + limit_states + "," + ll + ", " + maxrun + "): ");
			if (mm2.length() > 0) {
				System.out.print(Arrays.toString(mm2.minmax()));
			} else {
				System.out.print("Impossible");
			}

			System.out.print(" - ");

			if (mm1.length() > 0) {
				System.out.print(Arrays.toString(mm1.minmax()));
			} else {
				System.out.print("Impossible");
			}

			System.out.print("  ");
			System.out.print(mm2.componentSize);
			System.out.print("  ");
			System.out.print(mm1.componentSize);

			StructuralCausalModel canonical = Canonical.LOG.apply(model, seed++);
			double[] cmm = ccve(canonical, data, cause, effect).minmax();
			System.out.println(Arrays.toString(cmm));
			
			System.out.println();
		}

	}

	public Result ccve(StructuralCausalModel scm, DoubleTable data, int cause, int effect) {
		CredalCausalVE ccve = new CredalCausalVE(scm, data.toMap(false), scm.getExogenousVars());
		try {
			VertexFactor intfac = ccve.probNecessityAndSufficiency(cause, effect);
			return new Result(new double[] { intfac.getData()[0][0][0], intfac.getData()[0][1][0] }, null);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	public StructuralCausalModel log2standard(StructuralCausalModel input) {
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

	private Result pns(StructuralCausalModel model, Config c, int v1, int v2, DoubleTable datatable, TIntIntMap sizes)
			throws InterruptedException {
		EQEMLearner learner = new EQEMLearner(model, datatable, sizes, true, c);

		// learner.setDebugLoggerGenerator(new PDFLoggerGenerator("./run"));
		var cc = learner.run();

		var m = cc.getResults().entrySet()
				.stream().<Map.Entry<Integer, Integer>>map(a -> Map.entry(a.getKey(), a.getValue().size()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, HashMap::new));

//		long before = cc.combinations();
		cc.simplify();

		long after = cc.combinations();
		// System.out.println(before + " -> " + after);

		var solutions = (after < 200000) ? cc.exaustiveIterator() : cc.sobolIterator();

		TDoubleList pnss = new TDoubleArrayList(20000);
		StructuralCausalModel solution = null;
		PNS ppns = new PNS();

		for (int i = 0; i < 50000 && solutions.hasNext(); ++i) {
			solution = log2standard(solutions.next());
			double pnsval = ppns.execute(solution, v1, v2);
			pnss.add(pnsval);
		}

		// DetailedDotSerializer.saveModel("./run/solution.png", new
		// Info().model(solution).data(datatable));
		return new Result(pnss.toArray(), m);
	}

	record Result(double[] pnss, Map<Integer, Integer> componentSize) {
		double[] minmax() {
			double min = pnss[0];
			double max = pnss[0];

			for (int v = 1; v < pnss.length; ++v) {
				double val = pnss[v];
				if (val < min)
					min = val;
				else if (val > max)
					max = val;
			}
			return new double[] { min, max };
		}

		int length() {
			return pnss.length;
		}
	};
}
