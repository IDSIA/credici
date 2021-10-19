import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.credici.utility.experiments.Python;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.InvokerWithTimeout;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ch.idsia.credici.utility.experiments.Watch;


import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqIntMask;
import static ch.idsia.credici.utility.EncodingUtil.getSequentialMask;

public class Experiments implements Runnable{

	/*
	--executions 20 --datasize 1000 ./papers/clear22/models/s1a/chain_twExo1_nEndo5_0.uai
	 */


	/* command line arguments */

	@CommandLine.Parameters(description = "Model path in UAI format.")
	private String modelPath;

	@CommandLine.Option(names = {"-x", "--executions"}, description = "Number of EM executions. Default 10")
	private int executions = 30;

	@CommandLine.Option(names = {"-d", "--datasize"}, description = "Size of the sampled data. Default 1000")
	private int dataSize = 1000;

	@CommandLine.Option(names = {"-f", "--datafile"}, description = "CSV file with the data. Default null")
	private String datafile = null;

	@CommandLine.Option(names = {"-m", "--maxiter"}, description = "Maximum EM iterations per execution. Default 200")
	private int maxiter = 200;

	@CommandLine.Option(names = {"-s", "--seed"}, description = "Random seed. Default 0")
	private long seed = 0;

	@CommandLine.Option(names = {"-t", "--timeout"}, description = "Timeout in seconds. Default 6000s.")
	private long timeout = 6000;

	@CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results.")
	String outputFolder = null;

	@CommandLine.Option(names={"-l", "--logfile"}, description = "Output file for the logs.")
	String logfile = null;

	@CommandLine.Option(names={"-q", "--quiet"}, description = "Controls if log messages are printed to standard output.")
	boolean quiet;

	@CommandLine.Option(names={"--simpleOutput"}, description = "If activated, log-likelihood and kl statistics are not computed.")
	boolean simpleOutput = false;



	@CommandLine.Option(names = {"-X", "--cause"}, description = "Cause endogenous variable. Default is 0.")
	private int cause = 0;

	@CommandLine.Option(names = {"-Y", "--effect"}, description = "Effect endogenous variable. Default is the one with the higher id.")
	private int effect = -1;

	//////

	public static Experiments exp;
	private static String errMsg = "";
	private static String argStr;
	private static Logger logger;
	private static int numDecimalsRound = 5;
	private static int numSelectorParents = 3;


	StructuralCausalModel model;
	TIntIntMap[] data;
	HashMap empData;
	int empSize;


	HashMap<String, Object> stats;
	HashMap<String, Object> output = new HashMap<>();


	PrintWriter outPrinter = null;
	File outputFile = null;

	
	public static void main(String[] args) {
		argStr = String.join(";", args);
		CommandLine.run(new Experiments(), args);
		if(errMsg!="")
			System.exit(-1);

	}

	public static void disableWarning() {
		try {
			Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Unsafe u = (Unsafe) theUnsafe.get(null);

			Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
			Field logger = cls.getDeclaredField("logger");
			u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
		} catch (Exception e) {
			// ignore
		}
	}


	@Override
	public void run(){

		RandomUtil.setRandomSeed(seed);

		// Get model filename and folder
		String[] split =  modelPath.split("/");
		output.put("file", split[split.length-1]);
		output.put("folder", split[split.length-2]);


		try {
			setUpIO();
			logger.info("Input args: " + argStr);
			InvokerWithTimeout<double[]> invoker = new InvokerWithTimeout<>();
			exp = this;
			invoker.run(Experiments::pipline, timeout);
		}catch (Exception e){
			errMsg = e.toString();
			logger.severe(errMsg);
			e.printStackTrace();

		}catch (Error e){
			errMsg = e.toString();
			logger.severe(errMsg);
		}finally {
			processResults();
			logger.closeFile();

		}
	}


	private void setUpIO() throws IOException {
		disableWarning();
		// Set up the verbose and output files
		logger = new Logger().setVerbose(!quiet);
		if(logfile!=null)
			logger.setLogfile(logfile);

		String expStr = "_x" + executions +"_m" + maxiter +"_s"+seed;
		logger.info(expStr);

		if(outputFolder != null) {
			outputFile = new File(outputFolder + "/" +output.get("folder")+"_"+output.get("file") + expStr + ".py");
			outPrinter = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, false)));
		}


	}


	public static double[] pipline() throws IOException, ExecutionControl.NotImplementedException, InterruptedException, CsvException {
		exp.init();
		exp.runExact();
		exp.runApprox();
		return null;
		
	}


	private void runExact() throws InterruptedException, ExecutionControl.NotImplementedException {

		logger.info("Learning non-biased model");
		Watch.start();
		EMCredalBuilder builder = EMCredalBuilder.of(model, data)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.build();


		CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
		double[] res = runQueries(inf);
		long t = Watch.stop();

		logger.info("Ellapsed time:"+t+" ms.");
		logger.info("Queries results: "+Arrays.toString(res));

		// Save to output
		output.put("pnsExact_l", res[0]);
		output.put("pnsExact_u", res[1]);
		output.put("aceExact_l", res[2]);
		output.put("aceExact_u", res[3]);
		output.put("timeExact", t);


	}

	private void runApprox() throws InterruptedException, ExecutionControl.NotImplementedException {


		// Fix the parents of S
		int[] endoVars = model.getEndogenousVars();
		int[] parents = new int[numSelectorParents];
		parents[0] = endoVars[0];
		parents[numSelectorParents - 1] = endoVars[endoVars.length - 1];
		int idx[] = CollectionTools.shuffle(IntStream.range(1, endoVars.length - 1).toArray());
		for (int i = 0; i < numSelectorParents - 2; i++)
			parents[i + 1] = endoVars[idx[i]];


		int parentComb = model.getDomain(parents).getCombinations();

		List<int[]> assigList = getRandomSeqIntMask(parentComb, true);

		// Pick only some of the assigments
		List<int[]> finalAssigList = assigList;
		assigList =
				//DoubleStream.of(0., 2./5, 4./5,  1.)
				IntStream.of(0,2,6,8)
				//.mapToInt(i -> (int) (Math.floor(finalAssigList.size()-1) * i))
				.mapToObj(i -> finalAssigList.get(i)).collect(Collectors.toList());


		int numAssig = assigList.size();
		logger.info("Learning models with bias selection: "+numAssig+ " incremental partitions");
		logger.info("=============");


		// data structure for results
		double[] pnsEM_l= new double[numAssig];
		double[] pnsEM_u= new double[numAssig];
		double[] aceEM_l= new double[numAssig];
		double[] aceEM_u= new double[numAssig];
		double[] timeEM= new double[numAssig];
		double[] convPoints= new double[numAssig];
		double[] probAvailable= new double[numAssig];



		int i = 0;

		for (int[] assignments : assigList) {

			StructuralCausalModel modelBiased = SelectionBias.addSelector(model, parents, assignments);

			int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];
			TIntIntMap[] dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);

			int n1 = (int) Stream.of(dataBiased).filter(d -> d.get(selectVar) == 1).count();
			double pS1 = (1.0 * n1) / dataBiased.length;

			logger.info("Proportion observed: " + pS1);
			Watch.start();

			EMCredalBuilder builder = EMCredalBuilder.of(modelBiased, dataBiased)
					.setMaxEMIter(maxiter)
					.setNumTrajectories(executions)
					.setWeightedEM(true)
					.build();


			CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
			double[] res = this.runQueries(inf);
			long t = Watch.stop();

			logger.info("Ellapsed time:"+t+" ms.");

			int npoints = inf.pointsForConvergingPNS(0.90, cause, effect);
			logger.info("Convergence with " + npoints + " runs (>90%)");
			logger.info("Queries results: "+Arrays.toString(res));
			logger.info("=============");

			//store results
			timeEM[i] = t;
			pnsEM_l[i] = res[0];
			pnsEM_u[i] = res[1];
			aceEM_l[i] = res[2];
			aceEM_u[i] = res[3];
			convPoints[i] = npoints;
			probAvailable[i] = pS1;
			i++;

		}

		output.put("timeEM", timeEM);
		output.put("pnsEM_l", pnsEM_l);
		output.put("pnsEM_u", pnsEM_u);
		output.put("aceEM_l", aceEM_l);
		output.put("aceEM_u", aceEM_u);
		output.put("convPoints", convPoints);
		output.put("propAvailable", probAvailable);


		output.put("datasize", dataSize);
		output.put("maxiter", maxiter);
		output.put("executions", executions);
		output.put("partitions", numAssig);


	}
	private double[] runQueries(CausalInference inf) throws ExecutionControl.NotImplementedException, InterruptedException {

		// runQueries(CausalInference inf)
		List res = new ArrayList();
		VertexFactor p = null;
		double[] v = null;

		// PNS
		p = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);
		v =  Doubles.concat(p.getData()[0]);
		Arrays.sort(v);
		for(double val : v) res.add(val);
		if(v.length==1) res.add(v[0]);
/*
		// Causal query
		p = ((VertexFactor) inf.causalQuery().setIntervention(cause, 0).setTarget(effect).run()).filter(effect,0);
		v =  Doubles.concat(p.getData()[0]);
		Arrays.sort(v);
		for(double val : v) res.add(val);
		if(v.length==1) res.add(v[0]);

 */

		// ACE
		IntervalFactor ace = (IntervalFactor) inf.averageCausalEffects(cause, effect, 1, 1, 0);
		res.add(ace.getDataLower()[0][0]);
		res.add(ace.getDataUpper()[0][0]);

		// return
		return res.stream().mapToDouble(d -> (double)d).toArray();
	}


	private void processResults() {

		output.put("errorMsg",errMsg);
		output.put("error", !errMsg.isEmpty());

		String res = "results="+Python.mapToDict(output)+"\n";

		if(outPrinter != null) {
			outPrinter.print(res);
			logger.info("Saving results to "+outputFile.getAbsolutePath());
			outPrinter.close();
		}

		//P
		System.out.println(res);

	}

	private void init() throws IOException, CsvException {

		logger.info("Reading model at "+modelPath);
		model = (StructuralCausalModel) IO.read(modelPath);
		logger.info("Loaded SCM: "+model.getNetwork());
		logger.info("Endo C-components: "+Arrays.toString(model.endoConnectComponents().stream().map(c -> Arrays.toString(c)).toArray()));
		logger.info("Exo C-components: "+Arrays.toString(model.exoConnectComponents().stream().map(c -> Arrays.toString(c)).toArray()));


		int X[] = model.getEndogenousVars();
		if(cause < 0)
			cause = X[0];
		if(effect < 0)
			effect = X[X.length-1];

		logger.info("Cause: "+cause+", Effect: "+effect);

		if(!simpleOutput) {
			HashMap empTrue = model.getEmpiricalMap();
			empSize = FactorUtil.EmpiricalMapSize(empTrue);
			empTrue.put("empSize", empSize);
			logger.info("True empirical distribution: " + empTrue);
		}

		// Sample data

		if(datafile  == null) {
			logger.info("Sampling " + dataSize + " instances");
			data = model.samples(dataSize, model.getEndogenousVars());
		}else{
			data = DataUtil.fromCSV(datafile);
			logger.info("Loaded data with "+data.length+" from " + datafile + "");

		}

		if(!simpleOutput) {
			empData = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(model, data), numDecimalsRound);
			logger.info("Data empirical distribution: " + empData);
		}




		// Experiment global stats

		stats = new HashMap<>();
		int tw = model.getExogenousTreewidth();
		stats.put("exoTW", tw);
		stats.put("nExo",  model.getExogenousVars().length);
		stats.put("nEndo", model.getEndogenousVars().length);
		stats.put("markovian", CausalInfo.of(model).isMarkovian());
		stats.put("seed", seed);


		logger.info("Model statistics: "+stats+"");



		for(String k : stats.keySet())
			output.put(k, stats.get(k));

		//String strStats = stats.keySet().stream().map(k -> k+": "+stats.get(k)).collect( Collectors.joining( "," ) );

	}



}

