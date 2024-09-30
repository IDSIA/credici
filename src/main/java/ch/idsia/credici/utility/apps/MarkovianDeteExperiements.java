package ch.idsia.credici.utility.apps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;

import cern.colt.Arrays;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.learning.eqem.EQEMLearner;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.PearlNetwork;
import ch.idsia.credici.model.builder.RandomMarkovian;
import ch.idsia.credici.model.io.agrum.Serialize;
import ch.idsia.credici.model.io.dot.DetailedDotSerializer;
import ch.idsia.credici.model.io.dot.Info;
import ch.idsia.credici.model.io.netstring.NetStringSerialize;
import ch.idsia.credici.model.transform.Canonical;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.model.transform.PNS;
import ch.idsia.credici.utility.Probability;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.sample.Sampler;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "experiments", mixinStandardHelpOptions = true, version = "v1.0.0", header = "Encrypt FILE(s), or standard input, to standard output or to the output file.")
public class MarkovianDeteExperiements {
	enum Network {
		canonical, pearl, random
	};

	enum Method {
		emcc, emccCanonical, dete, relax, deteLimited, relaxLimited, ccve, ccveCanonical
	}

	@Option(names = { "--alpha" }, required = true, description = "Alpha for dirichlet", defaultValue = "0.01")
	private double alpha;

	@Option(names = { "--plot" }, required = true, description = "Plot networks", defaultValue = "false")
	private boolean plot;

	@Option(names = {
			"--pns-count" }, required = true, description = "Number of FSCM sampled to compute PNS", defaultValue = "10000")
	private Integer numPns;

	@Option(names = { "-o", "--output" }, required = true, description = "Output folder", defaultValue = ".")
	private File outfolder;

	@Option(names = { "-l", "--lleps" }, description = "Max ll", type = Double.class, defaultValue = "1e-9")
	private double ll = 1e-9;

	@Option(names = { "-r", "--runs" }, description = "Max runs", type = Integer.class, defaultValue = "500")
	private int maxrun = 500;

	@Option(names = { "-n", "--nodes" }, description = "number of nodes", type = Integer.class, defaultValue = "5")
	private int nodes = 8;

	@Option(names = { "-i", "--indegree" }, description = "max indegree", type = Integer.class, defaultValue = "4")
	private int indegree = 4;

	@Option(names = { "-s", "--seed" }, description = "random seed", type = Integer.class)
	private Integer seed;

	@Option(names = { "--maxsize" }, description = "exogenous limiting size", type = Integer.class, defaultValue = "16")
	private Integer limiting = 8;

	@Option(names = { "--limiting" }, description = "exogenous limiting size", split = ",")
	private int[] limitingSizes;

	@Option(names = { "-m",
			"--sample-size" }, description = "Sample sizes", type = Integer.class, defaultValue = "5000")
	private Integer sampleSize;

	@Option(names = { "--network" }, description = "network", type = Network.class, defaultValue = "canonical")
	Network networkType;

	@Option(names = { "--method" }, description = "Methods to be run", split = ",")
	private Set<Method> method;

	@Option(names = { "--iter" }, description = "random seed", defaultValue = "100000")
	private int iter;

	public static void main(String[] args) throws InterruptedException, IOException {
		MarkovianDeteExperiements exp = new MarkovianDeteExperiements();
		CommandLine cl = new CommandLine(exp);
		var res = cl.parseArgs(args);
		exp.experiment();
	}

	public void experiment() throws IOException, InterruptedException {
		if (outfolder != null && !outfolder.isDirectory()) {
			throw new IllegalArgumentException("output must be a folder");
		}

		if (seed == null) {
			seed = (int) System.nanoTime();
		}
		Random gen = new Random(seed);

		RandomMarkovian rm = new RandomMarkovian();
		rm.setSeed(gen.nextLong());
		rm.setNumberMaxArcs(indegree * nodes * 2);
		rm.setNumberNodes(nodes);
		rm.setNumberMaxInDegree(indegree);

		StructuralCausalModel model = null;
		int cause = 0;
		int effect = 0;

		DoubleTable data = null;
		String name = "";
		File file = null;

		if (networkType == Network.canonical) {

			file = new File(outfolder, seed + ".csv");
			name = "Canonical" + seed;
			model = rm.generate(nodes, -1, false);

			// find some random cause effect

			var b = rm.getEndogenousBoundaries(model);
			int[] r = b.getLeft();
			int[] l = b.getRight();

			int rindex = gen.nextInt(r.length);
			int lindex = gen.nextInt(l.length);

			cause = r[rindex];
			effect = l[lindex];

			// initial model depends on parameters
			model = Canonical.LOG.apply(model, gen.nextLong());

			// sample some data
			Sampler s = new Sampler(gen.nextLong());
			data = s.sample(model, sampleSize, model.getEndogenousVars());
			if (plot)
				DetailedDotSerializer.saveModel(new File(outfolder, seed + "_test1.png"),
						new Info().model(model).hideTables().data(data));

		} else if (networkType == Network.pearl) {
			file = new File(outfolder, "pearl.csv");
			name = "Canonical Pearl";
			PearlNetwork g = new PearlNetwork();

			model = g.createCanonicalModel();
			data = g.createData();

			cause = PearlNetwork.treatment;
			effect = PearlNetwork.recovery;

			seed = 0;

		} else if (networkType == Network.random) {
			Randomizer rnd = new Randomizer(gen.nextLong());

			model = rm.generate(nodes, -1, false);
			var b = rm.getEndogenousBoundaries(model);
			int[] r = b.getLeft();
			int[] l = b.getRight();

			int rindex = gen.nextInt(r.length);
			int lindex = gen.nextInt(l.length);

			cause = r[rindex];
			effect = l[lindex];

			model = rnd.makeRandomMarkovian(model);
			Sampler s = new Sampler(gen.nextLong());
			data = s.sample(model, sampleSize, model.getEndogenousVars());
			DetailedDotSerializer.saveModel(new File(outfolder, seed + "_random.png"),
					new Info().model(model).hideTables().data(data));

			file = new File(outfolder, seed + "_random.csv");
		}

		System.out.println(networkType + " source network with " + model.getEndogenousVars().length + " + "
				+ model.getExogenousVars().length);

		Writer out = new FileWriter(file);
		printer = new CSVPrinter(out, CSVFormat.RFC4180);
//		System.out.println(model);
//		System.out.println(data);
		if (plot)
			DetailedDotSerializer.saveModel(seed + "_source.png", new Info().model(model));

		canonical_experiment(model, data, cause, effect, gen, name);

		out.close();
	}

	private CSVPrinter printer;

	/*
	 * // very loosely 10**i/3 for integer i's TIntIntMap sizes = new
	 * TIntIntHashMap(); for (int v : model.getExogenousVars()) { int ch =
	 * model.getEndogenousChildren(v)[0]; int[] p = model.getEndegenousParents(ch);
	 * 
	 * int pc = model.getDomain(p).getCombinations(); int chs = model.getSize(ch);
	 * 
	 * int max = (int) Math.pow(chs, pc);
	 * 
	 * int states = Math.max(chs, Math.min(max, limit_states)); sizes.put(v,
	 * states); }
	 */

	public void log(List<Object> items, String name, Result results) throws IOException {
		var row = new ArrayList<Object>();
		row.add(name);
		row.addAll(items);
		row.add("ok");
		row.add("");
		row.addAll(results.toCols());

		printer.printRecord(row);
		printer.flush();
	}

	public void log(List<Object> items, String name, Throwable error) throws IOException {
		var row = new ArrayList<Object>();
		row.add(name);
		row.addAll(items);
		row.add("error");
		row.add(error.getMessage());

		printer.printRecord(row);
		printer.flush();
	}

	/*
	 * Planned experiments: - benchmark: start with M=Canonical FSCM & sample(M) =>
	 * PNS(M), M'=PSCM(M) => EMCC(M) & CCVE(M), M"=RMEQ(M') => Relax(M") & Dete(M")
	 * - benchmark: start with M=Canonical FSCM & sample(M) => PNS(M), M'=PSCM(M) =>
	 * EMCC(M) & CCVE(M), M"=Bound|U|(RMEQ(M'), 64) => Relax(M") & Dete(M") -
	 * benchmark: start with M=RandomEQ Bound|U|(Canonical FSCM) & sample(M) =>
	 * PNS(M), M'=Canonical(M) => EMCC(M) & CCVE(M) /!\ M-compat,
	 * M"=RMEQ(M') => Relax(M") & Dete(M") - benchmark: start with M=RandomEQ
	 * Bound|U|(Canonical FSCM) & sample(M) => PNS(M), M'=Canonical(M) => EMCC(M) &
	 * CCVE(M) /!\ M-compat, M"=Bound|U|(RMEQ(M'),64) => Relax(M") & Dete(M") <li>
	 * benchmark: start with M=RandomEQ Bound|U|(Canonical FSCM) & sample(M) =>
	 * PNS(M), M'=PSCM(M) => EMCC(M) & CCVE(M) /!\ M-compat,
	 * M"=RMEQ(M') => Relax(M") & Dete(M") <li> benchmark: start with M=RandomEQ
	 * Bound|U|(Canonical FSCM) & sample(M) => PNS(M), M'=PSCM(M) => EMCC(M) &
	 * CCVE(M) /!\ M-compat, M"=Bound|U|(RMEQ(M'), 32) => Relax(M") & Dete(M")
	 */

	/*
	 * - benchmark: start with M=Canonical FSCM & sample(M) => PNS(M), M'=PSCM(M) =>
	 * EMCC(M) & CCVE(M), M"=RMEQ(M') => Relax(M") & Dete(M") - benchmark: start
	 * with M=Canonical FSCM & sample(M) => PNS(M), M'=PSCM(M) => EMCC(M) & CCVE(M),
	 * M"=Bound|U|(RMEQ(M'), 64) => Relax(M") & Dete(M")
	 */
	public void canonical_experiment(StructuralCausalModel model, DoubleTable data, int cause, int effect, Random gen,
			String name) throws InterruptedException, IOException {

		StructuralCausalModel m = (networkType == Network.canonical) ? model
				: Canonical.LOG.apply(model, gen.nextLong());

		DetailedDotSerializer.saveModel("pearl.png", new Info().model(m).title("pearl"));

//		int iter = 10000;
		ArrayList<Object> header = new ArrayList<Object>();

		ArrayList<Object> row = new ArrayList<Object>();

		Randomizer rr = new Randomizer(gen.nextLong());

		// get FSCM value
		PNS pnstest = new PNS();
		StructuralCausalModel pnsnet = pnstest.pnsmodel(m, cause, effect);
		double point_pns = pnstest.execute(m, cause, effect);

		// create a string of the network
		NetStringSerialize ns = new NetStringSerialize();
		String netStr = ns.apply(m);

		row.add(name);
		header.add("seed");
		row.add(netStr);
		header.add("network");

		List<String> exosizes = new ArrayList<String>();
		for (var x : m.getExogenousVars()) {
			exosizes.add(x + "=(" + m.getSize(x) + ")");
		}
		row.add(exosizes.stream().collect(Collectors.joining(",")));
		header.add("exo sizes");

		row.add(cause);
		header.add("cause");
		row.add(effect);
		header.add("effect");
		row.add(pnsnet.getExogenousVars().length);
		header.add("PNS exovars");
		row.add(pnsnet.getVariablesCount());
		header.add("PNS vars");
		row.add(point_pns);
		header.add("PNS(FSCM)");

		row.add(ll);
		header.add("LL");
		row.add(maxrun);
		header.add("Max Run");

		// row.add(sizes);

		var settings = new Config().llEPS(ll).numIterations(iter * 2).numPSCMRuns(0).maxRun(1) // enough to test m-compatibility
				.deterministic(true).freeEndogenous(false);

		var mmx = pns(m, settings, cause, effect, data, new TIntIntHashMap());
		header.add("M-compatibility");
		if (mmx.length() == 0) {
			System.out.println("failed M-compatibility");
			row.add("m-incompatible");
		} else {
			System.out.println("M-compatibile");
			row.add("m-compatible");
		}

		row.add(0.001);
		header.add("endo alpha");

		header.add("status");
		header.add("message");
		header.add("lower");

		// row.addAll(mmx.toCols());
		long seeeding = gen.nextLong();

		if (method.contains(Method.emcc)) {
			System.out.println("Source network EMCC");

			settings = new Config().llEPS(ll).numIterations(iter).numPSCMRuns(0).alpha(alpha).numRun(maxrun)
					.seed(seeeding).freeEndogenous(false).deterministic(false);
			try {
				var mmEMCC = pns(model, settings, cause, effect, data, new TIntIntHashMap());
				log(row, "EMCC", mmEMCC);
				System.out.println("E: " + mmEMCC.componentSize + " " + Arrays.toString(mmEMCC.minmax()));
			} catch (Throwable err) {
				log(row, "EMCC", err);
				System.out.println("E: failed");
			}
		}

		if (method.contains(Method.emccCanonical)) {
			System.out.println("Canonical EMCC");

			settings = new Config().llEPS(ll).numIterations(iter).numPSCMRuns(0).alpha(alpha).numRun(maxrun)
					.seed(seeeding).freeEndogenous(false).deterministic(false);
			try {
				// run on canonical network
				var mmEMCC = pns(m, settings, cause, effect, data, new TIntIntHashMap());
				log(row, "CanoEMCC", mmEMCC);
				System.out.println("E: " + mmEMCC.componentSize + " " + Arrays.toString(mmEMCC.minmax()));
			} catch (Throwable err) {
				log(row, "EMCC", err);
				System.out.println("E: failed");
			}
		}

		if (method.contains(Method.relax)) {
			System.out.println("Relax");
			settings = new Config().llEPS(ll).numIterations(iter).numPSCMRuns(0).alpha(alpha).numRun(maxrun)
					.seed(seeeding).freeEndogenous(true).deterministic(false);
			try {
				var mmRelax = pns(m, settings, cause, effect, data, new TIntIntHashMap());
				log(row, "Relax", mmRelax);
				System.out.println("R: " + mmRelax.componentSize + " " + Arrays.toString(mmRelax.minmax()));
			} catch (Throwable err) {
				log(row, "Relax", err);
				System.out.println("R: failed");
			}
		}

		if (method.contains(Method.dete)) {
			System.out.println("Dete");
			settings = new Config().llEPS(ll).numIterations(iter).numPSCMRuns(0).alpha(alpha).numRun(maxrun)
					.seed(seeeding).freeEndogenous(true).deterministic(true);
			try {
				var mmDete = pns(m, settings, cause, effect, data, new TIntIntHashMap());
				log(row, "Dete", mmDete);
				System.out.println("D: " + mmDete.componentSize + " " + Arrays.toString(mmDete.minmax()));
			} catch (Throwable err) {
				log(row, "Dete", err);
				System.out.println("D: failed");
			}
		}

		if (method.contains(Method.ccve)) {
			System.out.println("Source model CCVE");
			try {
				var mmccve = ccve(model, data, cause, effect);
				log(row, "CCVE", mmccve);
				System.out.println("C: " + mmccve.componentSize + " " + Arrays.toString(mmccve.minmax()));

				mmccve = ccve(m, data, cause, effect);
				System.out.println("C: " + mmccve.componentSize + " " + Arrays.toString(mmccve.minmax()));
			} catch (Throwable x) {
				log(row, "CCVE", x);
				row.add("CCVE failed " + x.getMessage());
			}
		}

		if (method.contains(Method.ccveCanonical)) {
			System.out.println("Canonical CCVE");
			try {
				var mmccve = ccve(m, data, cause, effect);
				log(row, "CanoCCVE", mmccve);
				System.out.println("C: " + mmccve.componentSize + " " + Arrays.toString(mmccve.minmax()));
			} catch (Throwable x) {
				log(row, "CanoCCVE", x);
				row.add("CanoCCVE failed " + x.getMessage());
			}
		}
		
		StructuralCausalModel m2 = rr.makeRandom(m, limiting, limitingSizes);

		if (method.contains(Method.relaxLimited)) {
			System.out.println("Relax Limited");
			settings = new Config().llEPS(ll).numIterations(iter).numPSCMRuns(0).alpha(alpha).numRun(maxrun)
					.seed(seeeding).freeEndogenous(true).deterministic(false);
			try {
				System.out.println(data);
				var mmRelaxLimited = pns(m2, settings, cause, effect, data, new TIntIntHashMap());
				log(row, "RelaxLimited", mmRelaxLimited);
				System.out.println(
						"RLimit: " + mmRelaxLimited.componentSize + " " + Arrays.toString(mmRelaxLimited.minmax()));
			} catch (Throwable err) {
				log(row, "RelaxLimited", err);
				System.out.println("RLimit: failed");
			}
		}

		Random r;

		if (method.contains(Method.deteLimited)) {
			System.out.println("Dete Limited");
			settings = new Config().llEPS(ll).numIterations(iter).numPSCMRuns(0).alpha(alpha).numRun(maxrun)
					.seed(seeeding).freeEndogenous(true).deterministic(true);
			try {
				var mmDeteLimited = pns(m2, settings, cause, effect, data, new TIntIntHashMap());
				log(row, "DeteLimited", mmDeteLimited);
				System.out.println(
						"DLimited: " + mmDeteLimited.componentSize + " " + Arrays.toString(mmDeteLimited.minmax()));
			} catch (Throwable err) {
				log(row, "DeteLimited", err);
				System.out.println("DLimited: failed");
			}
		}
		System.out.println();
	}

	public Result ccve(StructuralCausalModel scm, DoubleTable data, int cause, int effect) {
		long start = System.nanoTime();
		// scm = log2standard(scm);

		CredalCausalVE ccve = new CredalCausalVE(scm, data.toMap(false), scm.getExogenousVars());
		long learn = System.nanoTime() - start;
		long simplify = 0;
		try {
			VertexFactor intfac = ccve.probNecessityAndSufficiency(cause, effect, 1, 0);
			long inference = System.nanoTime() - start - learn - simplify;
			return new Result(new double[] { intfac.getData()[0][0][0], intfac.getData()[0][1][0] }, null, null, learn,
					simplify, inference);
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

	private Result pns(StructuralCausalModel model, Config config, int cause, int effect, DoubleTable datatable,
			TIntIntMap sizes) throws InterruptedException {
		long start = System.nanoTime();
		for (var exo : model.getExogenousVars()) {
			System.out.print(exo + "=" + model.getSize(exo) + ", ");
		}
		System.out.println();

		EQEMLearner learner = new EQEMLearner(model, datatable, sizes, true, config);

		// learner.setDebugLoggerGenerator(new PDFLoggerGenerator("./run"));
		var cc = learner.run();
		long learn = System.nanoTime() - start;

		var m = cc.getResults().entrySet()
				.stream().<Map.Entry<Integer, Integer>>map(a -> Map.entry(a.getKey(), a.getValue().size()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, HashMap::new));

		var before = cc.combinations();
		cc.simplify();
		long simplify = System.nanoTime() - start - learn;

		var after = cc.combinations();
		System.out.println(before + " -> " + after);

		var m2 = cc.getResults().entrySet()
				.stream().<Map.Entry<Integer, Integer>>map(a -> Map.entry(a.getKey(), a.getValue().size()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, HashMap::new));

		boolean hasMany = cc.hasAtLeastCombinations(numPns);
		var solutions = (hasMany) ? cc.sobolIterator() : cc.exaustiveIterator();

		TDoubleList pnss = new TDoubleArrayList(numPns);
		StructuralCausalModel solution = null;
		PNS ppns = new PNS();

		for (int i = 0; i < numPns && solutions.hasNext(); ++i) {
			solution = solutions.next(); // log2standard(solutions.next());

			double pnsval = ppns.executeOther(solution, cause, 1, 0, effect, 1, 0);
			if (pnsval > 0.1) {
				IntFunction<String> names = (index) -> new String[] { "Z", "X", "Y", "UZ", "UX", "UY", "X2", "Y2" }[index];

				System.out.println("here " + pnsval + " " + i);
				EmpiricalNetwork en = new EmpiricalNetwork();
				var x = en.apply(solution, datatable);
				var ll2 = en.loglikelihood(x, datatable);
				var lll = Probability.maxLogLikelihood(model, datatable.toMap(false));
				var xxx = Probability.LL(solution, datatable.toMap(false));
				System.out.println(ll2 + " " + lll + " " + xxx);
				
				var scm = ppns.pnsmodel(solution, cause, 1, 0, effect, 1, 0, false);
				ppns.pnsmodel(solution, cause, effect);
				
				try(var pw = new PrintWriter(System.out)){
					new Serialize(PearlNetwork.getNaming()).serialize(solution, pw);
					pw.flush();
					
					new Serialize((n)-> new String[] {"Z", "X", "Y", "U_z", "U_x", "U_y", "Zp", "Xp", "Yp"}[n]).serialize(scm, pw);
					pw.flush();
				} 
				
				DetailedDotSerializer.saveModel("FSCMPearl.png", new Info().model(solution).data(datatable));
				DetailedDotSerializer.saveModel("PNSPearl.png", new Info().model(scm).data(datatable));
				Serialize s = new Serialize(names);

				try (var out = new PrintWriter(System.out)) {
					s.serialize(solution, out);
					out.println("-----------");
					s.serialize(scm, out);
				}
			}
			pnss.add(pnsval);

		}

		System.out.println(cause + " " + effect + " " + pnss.size());
//		System.out.println(pnss);

		long inference = System.nanoTime() - start - learn - simplify;
		// DetailedDotSerializer.saveModel("./run/solution.png", new
		// Info().model(solution).data(datatable));
		return new Result(pnss.toArray(), m, m2, learn, simplify, inference);
	}

	void toTableHtml(BayesianFactor factor, int subject) {
		
		NumberFormat nf = new DecimalFormat("#.#####");
		
		StringBuilder builder = new StringBuilder();
		var domain  = factor.getDomain();
		int size = domain.getCardinality(subject);
		
		int[] vars = domain.getVariables();
		int[] conditioning = ArrayUtils.removeElement(vars, subject);
		
		
		
		int cols = domain.getCombinations()/size;
		int repeats = 1;
		
		builder.append("<table>\n<tr><th rowspan='").append(vars.length - 1).append("'>").append(subject).append("</th>");
		
		for (int parent : ArraysUtil.reverse(conditioning)) {
			
			builder.append("<th>").append(parent).append("</th>");
			
			int psize = domain.getCardinality(parent);
			int span = cols / psize;
			for (int r = 0; r < repeats; ++r) {
				for (int s = 0; s < psize; s++) {
					builder.append("<th colspan='").append(span).append("'>").append(s).append("</th>");
				}
			}
			repeats *= psize;
			cols /= psize;
			builder.append("</tr>\n<tr>");
		}
		if (conditioning.length == 0) {
			builder.append("<th></th></tr>");
		}
		
		vars = ArrayUtils.add(conditioning, subject); // append item (so first round will go through subjec==0) 
		var iter = factor.getDomain().getReorderedIterator(vars);
		int row=0; 
		
		builder.append("<td colspan='2'>").append(row).append("</td>");
		while (iter.hasNext()) {
			int[] position = iter.getPositions().clone();
			if (position[position.length-1] != row) {
				row = position[position.length-1];
				builder.append("</tr>\n<tr>");
				builder.append("<td colspan='2'>").append(row).append("</td>");
			}
			int index = iter.next();
			builder.append("<td>").append(nf.format(factor.getValueAt(index))).append("</td>");
		}
		builder.append("</tr></table>");
		System.out.println(builder);
	}
	
	void toAgrum(BayesianFactor factor, int subject) {
		
		NumberFormat nf = new DecimalFormat("#.#####");
		
		StringBuilder builder = new StringBuilder();
		var domain  = factor.getDomain();
		int size = domain.getCardinality(subject);
		
		int[] vars = domain.getVariables();
		int[] conditioning = ArrayUtils.removeElement(vars, subject);
		
		
		
		int cols = domain.getCombinations()/size;
		int repeats = 1;
		
		
		
		for (int parent : ArraysUtil.reverse(conditioning)) {
			
			builder.append("<th>").append(parent).append("</th>");
			
			int psize = domain.getCardinality(parent);
			int span = cols / psize;
			for (int r = 0; r < repeats; ++r) {
				for (int s = 0; s < psize; s++) {
					builder.append("<th colspan='").append(span).append("'>").append(s).append("</th>");
				}
			}
			repeats *= psize;
			cols /= psize;
			builder.append("</tr>\n<tr>");
		}
		if (conditioning.length == 0) {
			builder.append("<th></th></tr>");
		}
		
		vars = ArrayUtils.add(conditioning, subject); // append item (so first round will go through subjec==0) 
		var iter = factor.getDomain().getReorderedIterator(vars);
		int row=0; 
		
		builder.append("<td colspan='2'>").append(row).append("</td>");
		while (iter.hasNext()) {
			int[] position = iter.getPositions().clone();
			if (position[position.length-1] != row) {
				row = position[position.length-1];
				builder.append("</tr>\n<tr>");
				builder.append("<td colspan='2'>").append(row).append("</td>");
			}
			int index = iter.next();
			builder.append("<td>").append(nf.format(factor.getValueAt(index))).append("</td>");
		}
		builder.append("</tr></table>");
		System.out.println(builder);
	}
	/**
	 * A summary of the results given by an algorithm. Not all methods will fill all
	 * the fields. However, pnss must be present if the methods was successfull.
	 * 
	 */
	record Result(double[] pnss, Map<Integer, Integer> componentSizeBefore, Map<Integer, Integer> componentSize,
			long learn, long simplify, long inference) {

		/**
		 * Gets min and max PNS
		 * 
		 * @return
		 */
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

		/**
		 * Number of pnss results
		 * 
		 * @return
		 */
		int length() {
			return pnss.length;
		}

		/**
		 * Convert a results record in a list of fields
		 * 
		 * @return
		 */
		List<Object> toCols() {// String base, List<Object> header) {
			ArrayList<Object> args = new ArrayList<Object>();
			if (length() > 0) {
				double[] mm = minmax();
				args.add(mm[0]);
				args.add(mm[1]);
			} else {
				args.add(Double.NaN);
				args.add(Double.NaN);
			}
//			header.add(base + " lower");
//			header.add(base + " upper");

			args.add(componentSizeBefore); // header.add(base + " before CC sizes");
			args.add(componentSize); // header.add(base + " CC sizes");
			args.add(learn); // header.add(base + " learn");
			args.add(simplify); // header.add(base + " simplify");
			args.add(inference); // header.add(base + " inference");
			return args;
		}
	};
}
