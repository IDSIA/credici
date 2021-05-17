package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.credici.utility.experiments.Python;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import ch.idsia.credici.utility.experiments.Watch;


import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqMask;
import static ch.idsia.credici.utility.EncodingUtil.getSequentialMask;

public class Experiments implements Runnable{
/*
--executions 20 --datasize 1000 --selectpol LAST ./papers/neurips21/models/set1/chain_twExo0_nEndo4_5.uai
--executions 20 --datasize 1000 --selectpol LAST ./papers/neurips21/models/set1/chain_twExo1_nEndo4_6.uai

case with ratio==0 but good results:
--executions 20 --datasize 500 --policy LAST --seed 1234 ./papers/neurips21/models/set1/chain_twExo1_nEndo6_3.uai

case with ratios of 0.0 and of 0.99, unfeasible:
--executions 20 --datasize 500 --policy LAST ./papers/neurips21/models/set1/chain_twExo1_nEndo6_4.uai

case with ratios of 0.99, unfeasible:
--executions 20 --datasize 500 --policy LAST ./papers/neurips21/models/set1/chain_twExo1_nEndo6_6.uai



*/

	enum InferenceMethod {approxlp, cve, saturation}


	/* command line arguments */

	@CommandLine.Parameters(description = "Model path in UAI format.")
	private String modelPath;


	@CommandLine.Option(names = {"-x", "--executions"}, description = "Number of EM executions. Default 10")
	private int executions = 10;

	@CommandLine.Option(names = {"-d", "--datasize"}, description = "Size of the sampled data. Default 1000")
	private int dataSize = 1000;

	@CommandLine.Option(names = {"-m", "--maxiter"}, description = "Maximum EM iterations per execution. Default 200")
	private int maxiter = 200;

	@CommandLine.Option(names = {"-s", "--seed"}, description = "Random seed. Default 0")
	private long seed = 0;

	@CommandLine.Option(names = {"-p", "--policy"}, description = "Selection Policy: ${COMPLETION-CANDIDATES}")
	private EMCredalBuilder.SelectionPolicy method = EMCredalBuilder.SelectionPolicy.LAST;

	@CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results.")
	String outputFolder = null;

	@CommandLine.Option(names={"-l", "--logfile"}, description = "Output file for the logs.")
	String logfile = null;

	@CommandLine.Option(names={"-q", "--quiet"}, description = "Controls if log messages are printed to standard output.")
	boolean quiet;


	@CommandLine.Option(names={"-g", "--groundtruth"}, description = "Inference method for ground truth")
	InferenceMethod infGroundTruth = InferenceMethod.cve;



	//////

	private static String errMsg = "";
	private static String argStr;
	private static ch.idsia.credici.utility.experiments.Logger logger;
	private static int numDecimalsRound = 5;

	StructuralCausalModel model;
	TIntIntMap[] data;
	HashMap empData;
	int empSize;

	int cause;
	int effect;




	/*

	double[] pnsExact = null;
	double[][] pnsEM = null;
	int[] innerPoints = null;

*/


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
			experiments();
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

		String expStr = "_x" + executions +"_p"+method+ "_d" + dataSize + "_m" + maxiter +"_s"+seed;
		logger.info(expStr);

		if(outputFolder != null) {
			outputFile = new File(outputFolder + "/" +output.get("folder")+"_"+output.get("file") + expStr + ".py");
			outPrinter = new PrintWriter(new BufferedWriter(new FileWriter(outputFile, false)));
		}


	}


	private void experiments() throws IOException, ExecutionControl.NotImplementedException, InterruptedException {
		init();
		runExact();
		runApprox();
		
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

	private void runApprox() throws InterruptedException, ExecutionControl.NotImplementedException {

		logger.info("Building model with EM");
		Watch.start();

		EMCredalBuilder builder = EMCredalBuilder.of(model, data)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setNumDecimalsRound(numDecimalsRound)
				.buildTrajectories();

		long time = Watch.stop();
		output.put("timeBuild", time);

		logger.info("Finished "+executions+" EM executions in "+time+" ms.");

		List masks = getSequentialMask(executions);
		//int i=10;
		double[] pnsEM_l= new double[masks.size()];
		double[] pnsEM_u= new double[masks.size()];

		int[] innerPoints = new int[masks.size()];
		int[] iterEM = new int[masks.size()];
		long[] timeQuery = new long[masks.size()];
		double[] llkratio = new double[masks.size()];

		double[] klqp = new double[masks.size()];
		double[] klpq = new double[masks.size()];
		double[] klsym = new double[masks.size()];




		for(int i=0; i<masks.size(); i++) {



			Watch.start();

			boolean[] m = (boolean[]) masks.get(i);
			builder.setMask(m).selectAndMerge();
			CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
			double[] pnsEM = calculatePNS(inf);
			pnsEM_l[i] = pnsEM[0];
			pnsEM_u[i] = pnsEM[1];
			time = Watch.stop();

			innerPoints[i] = builder.getConvergingTrajectories().size();
			timeQuery[i] = time;

			iterEM[i] = builder.getTrajectories().get(i).size()-1;
			llkratio[i] = builder.ratioLk(builder.getSelectedPoints().get(i));
			klqp[i] = builder.klQP(builder.getSelectedPoints().get(i), false)/empSize;
			klpq[i] = builder.klPQ(builder.getSelectedPoints().get(i), false)/empSize;
			klsym[i] = builder.klsym(builder.getSelectedPoints().get(i), true)/empSize;



			logger.info("Approx PSN with "+(i+1)+" " +
					"points: \t"+ Arrays.toString(pnsEM)+"\t (ratio="+llkratio[i]+", " + "klqp="+klqp[i]+", "
					+"klpq="+klpq[i]+", "+"klsym="+klsym[i]+", "+
					" "+iterEM[i]+" iter. "+time+" ms.)");
		}


		logger.info("Done "+executions+" counterfactual queries in "+Arrays.stream(timeQuery).sum()+" ms.");

		output.put("timeQuery", timeQuery);
		output.put("pnsEM_l", pnsEM_l);
		output.put("pnsEM_u", pnsEM_u);
		output.put("innerPoints", innerPoints);
		output.put("iterEM", iterEM);
		output.put("llkratio", llkratio);
		output.put("klpq", klpq);
		output.put("klqp", klqp);
		output.put("klsym", klsym);

		if(infGroundTruth == InferenceMethod.saturation) {
			output.put("pnsExact_l", pnsEM_l[masks.size() - 1]);
			output.put("pnsExact_u", pnsEM_u[masks.size() - 1]);

		}

	}

	private void runExact() throws InterruptedException, ExecutionControl.NotImplementedException {
		// True results with PGM method
		
		if(List.of(InferenceMethod.cve, InferenceMethod.approxlp).contains(infGroundTruth)) {
			Watch.start();
			SparseModel vmodel = null;
			logger.info("Running exact method: "+infGroundTruth);

			CausalInference inf = null;
			double[] pnsExact = new double[]{-1,-1};
			try {
				if (infGroundTruth == InferenceMethod.cve)
					inf = new CredalCausalVE(model, empData.values());
				else
					inf = new CredalCausalApproxLP(model, empData.values());


				pnsExact = calculatePNS(inf);
			}catch(Exception e){
				logger.severe("Exact method cannot be applied: Unfeasible solution");
			}

			long time = Watch.stop();

			logger.info("Exact PSN \t: \t" + Arrays.toString(pnsExact) + " in " + time + " ms.");

			// Save to output
			output.put("pnsExact_l", pnsExact[0]);
			output.put("pnsExact_u", pnsExact[1]);
			output.put("timeExact", time);
		}
		output.put("groundtruth", infGroundTruth.name());
		
	}


	private double[] calculatePNS(CausalInference inf) throws InterruptedException, ExecutionControl.NotImplementedException {
		VertexFactor p = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);
		double[] v =  Doubles.concat(p.getData()[0]);
		Arrays.sort(v);
		return new double[]{v[0], v[v.length-1]};

	}

	private void init() throws IOException {

		logger.info("Reading model at "+modelPath);
		model = (StructuralCausalModel) IO.read(modelPath);
		logger.info("Loaded SCM: "+model.getNetwork());

		int X[] = model.getEndogenousVars();
		cause = X[0];
		effect = X[X.length-1];

		HashMap empTrue = 	model.getEmpiricalMap();
		logger.info("True empirical distribution: "+empTrue);

		// Sample data
		logger.info("Sampling "+dataSize+" instances");
		data = model.samples(dataSize, model.getEndogenousVars());
		empData = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(model, data), numDecimalsRound);
		logger.info("Data empirical distribution: "+empData);

		empSize = FactorUtil.EmpiricalMapSize(empTrue);
		empTrue.put("empSize", empSize);
		// Experiment global stats

		stats = new HashMap<>();
		int tw = model.getExogenousTreewidth();
		stats.put("exoTW", tw);
		stats.put("nExo",  model.getExogenousVars().length);
		stats.put("nEndo", model.getEndogenousVars().length);
		stats.put("markovian", CausalInfo.of(model).isMarkovian());
		stats.put("seed", seed);


		logger.info("Model statistics: "+stats+"");

		// Change ground truth if needed
		if(tw>1)
			this.infGroundTruth = InferenceMethod.saturation;



		for(String k : stats.keySet())
			output.put(k, stats.get(k));

		//String strStats = stats.keySet().stream().map(k -> k+": "+stats.get(k)).collect( Collectors.joining( "," ) );

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

}

