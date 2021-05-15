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

import ch.idsia.credici.utility.experiments.Watch;


import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqMask;
import static ch.idsia.credici.utility.EncodingUtil.getSequentialMask;

public class Experiments implements Runnable{
/*
--executions 20 --datasize 1000 --selectpol LAST ./papers/neurips21/models/set1/chain_twExo0_nEndo4_5.uai
--executions 20 --datasize 1000 --selectpol LAST ./papers/neurips21/models/set1/chain_twExo1_nEndo4_6.uai


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

	@CommandLine.Option(names = {"-s", "--selectpol"}, description = "Selection Policy: ${COMPLETION-CANDIDATES}")
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
			//e.printStackTrace();

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

		if(outputFolder != null) {

			outputFile = new File(outputFolder + "/" + output.get("file") + "_x" + executions + "_d" + dataSize + "_m" + maxiter + ".py");
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
		double[][] pnsEM = new double[masks.size()][];
		int[] innerPoints = new int[masks.size()];
		int[] iterEM = new int[masks.size()];
		long[] timeQuery = new long[masks.size()];
		double[] llkratio = new double[masks.size()];



		for(int i=0; i<masks.size(); i++) {

			Watch.start();

			boolean[] m = (boolean[]) masks.get(i);
			builder.setMask(m).selectAndMerge();
			CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
			pnsEM[i] = calculatePNS(inf);
			time = Watch.stop();

			innerPoints[i] = builder.getConvergingTrajectories().size();
			timeQuery[i] = time;

			iterEM[i] = builder.getTrajectories().get(i).size()-2;
			llkratio[i] = builder.ratioLk(builder.getSelectedPoints().get(i));

			logger.info("Approx PSN with "+(i+1)+" " +
					"points: \t"+ Arrays.toString(pnsEM[i])+"\t (ratio="+llkratio[i]+", "+iterEM[i]+" iter. "+time+" ms.)");
		}


		logger.info("Done "+executions+" counterfactual queries in "+Arrays.stream(timeQuery).sum()+" ms.");

		output.put("timeQuery", timeQuery);
		output.put("pnsEM", pnsEM);
		output.put("innerPoints", innerPoints);
		output.put("iterEM", iterEM);
		output.put("llkratio", llkratio);

		if(infGroundTruth == InferenceMethod.saturation)
			output.put("groundtruth", pnsEM[masks.size()-1]);

	}

	private void runExact() throws InterruptedException, ExecutionControl.NotImplementedException {
		// True results with PGM method
		
		if(List.of(InferenceMethod.cve, InferenceMethod.approxlp).contains(infGroundTruth)) {
			Watch.start();
			SparseModel vmodel = null;
			logger.info("Running exact method: "+infGroundTruth);

			CausalInference inf = null;

			if(infGroundTruth==InferenceMethod.cve)
				inf = new CredalCausalVE(model, empData.values());
			else
				inf = new CredalCausalApproxLP(model, empData.values());


			double[] pnsExact = calculatePNS(inf);

			long time = Watch.stop();

			logger.info("Exact PSN \t: \t" + Arrays.toString(pnsExact) + " in " + time + " ms.");

			// Save to output
			output.put("pnsExact", pnsExact);
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

		// Experiment global stats

		stats = new HashMap<>();
		stats.put("exoTW", model.getExogenousTreewidth());
		stats.put("nExo",  model.getExogenousVars().length);
		stats.put("nEndo", model.getEndogenousVars().length);
		stats.put("markovian", CausalInfo.of(model).isMarkovian());

		output.put("stats", stats);

		//String strStats = stats.keySet().stream().map(k -> k+": "+stats.get(k)).collect( Collectors.joining( "," ) );
		logger.info("Model statistics: "+stats+"");

	}




	private static void run_(String modelPath, int executions, int maxIter, int dataSize) throws InterruptedException, IOException {
		//Sample data
		StructuralCausalModel model = (StructuralCausalModel) IO.read(modelPath);

		System.out.println(model);
		System.out.println("Exo tw: "+model.getExogenousTreewidth());
		System.out.println("Exogenous DAG:");
		System.out.println(model.getExogenousDAG());

		System.out.println("True empirical:");
		HashMap empTrue = 	model.getEmpiricalMap();
		System.out.println(empTrue);

		System.out.println("Data empirical:");
		TIntIntMap[] data = model.samples(dataSize, model.getEndogenousVars());
		HashMap empData = DataUtil.getEmpiricalMap(model, data);
		System.out.println(empData);


		int[] X = model.getEndogenousVars();


		RandomUtil.setRandomSeed(0);
		EMCredalBuilder builder = EMCredalBuilder.of(model, data)
				.setMaxEMIter(maxIter)
				.setNumTrajectories(executions)
				//.setNumDecimalsRound(-1)
				.buildTrajectories();

		System.out.println("built trajectories");


		// todo set masks
		//builder.setNumDecimalsRound(10);
		//builder.selectAndMerge()


		//System.out.println("\tIs Inner approximation = " + builder.isInnerApproximation());
		//System.out.println("\tConverging Trajectories = " +builder.getConvergingTrajectories().size());
		//System.out.println("\tSelected points = " + builder.getSelectedPoints().size());

		System.out.println("trajectories sizes:");
		System.out.println(Arrays.toString(builder.getTrajectories().stream().map(t -> t.size()-1).toArray()));


		List masks = getRandomSeqMask(executions);
		//int i=10;
		double[][] results = new double[masks.size()][];
		for(int i=0; i<masks.size(); i++) {
			System.out.print(".");
			boolean[] m = (boolean[]) masks.get(i);
			builder.setMask(m).selectAndMerge();
			CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());

			VertexFactor pn = (VertexFactor) inf.probNecessity(X[0], X[X.length - 1]);
			VertexFactor ps = (VertexFactor) inf.probSufficiency(X[0], X[X.length - 1]);
			double[] pn_bounds = null;
			double[] ps_bounds = null;

			if(pn.getData()[0].length>1) {
				pn_bounds = Doubles.concat(pn.getData()[0][0], pn.getData()[0][1]);
				Arrays.sort(pn_bounds);
			}else{
				pn_bounds = Doubles.concat(pn.getData()[0][0], pn.getData()[0][0]);
			}
			if(ps.getData()[0].length>1) {
				ps_bounds = Doubles.concat(ps.getData()[0][0], ps.getData()[0][1]);
				Arrays.sort(ps_bounds);
			}else{
				ps_bounds = Doubles.concat(ps.getData()[0][0], ps.getData()[0][0]);
			}

			results[i] = Doubles.concat(pn_bounds, ps_bounds);
		}


		String s[] = modelPath.split("/");

/*
		new WriterCSV(results, wdir+resFolder+s[s.length-1]+".csv" )
				.setVarNames("pn_low", "pn_up","ps_low", "ps_up")
				.withIndex(true)
				.write();*/
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

