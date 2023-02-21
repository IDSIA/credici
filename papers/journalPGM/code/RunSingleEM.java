package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class RunSingleEM extends Terminal {

	/*


	 -s 0 --maxiter 1 -w -d /Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalPGM/models/literature/triangolo/triangolo_causal.csv /Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalPGM/models/literature/triangolo/triangolo_causal_biassoft_2".uai

	 */

	@CommandLine.Parameters(description = "Model path in UAI format.")
	private String modelPath;

	@CommandLine.Option(names = {"-d", "--data"}, description = "Data path in CSV format.")
	private String dataPath;

	@CommandLine.Option(names = {"-m", "--maxiter"}, description = "Maximum EM internal iterations. Default to 500")
	private int maxIter = 500;

	@CommandLine.Option(names={"-w", "--weighted"}, description = "If activated, improved weighted EM is run")
	boolean weighted = false;

	@CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results. Default working dir.")
	String output = ".";

	//@CommandLine.Option(names={"-b", "--bias"}, description = "If activated, the input model should be consistent with the selection bias scheme.")
	boolean bias = false;

	double p1 = 1.0;

	public static void main(String[] args) {
		argStr = String.join(";", args);
		CommandLine.run(new RunSingleEM(), args);
		if(errMsg!="")
			System.exit(-1);
	}

	@Override
	protected void entryPoint() throws IOException, CsvException, InterruptedException {

		Path wdir = Paths.get(".");
		RandomUtil.setRandomSeed(seed);
		logger.info("Starting logger with seed "+seed);

		TIntIntMap[] data = null;

		// Load model
		String fullpath = wdir.resolve(modelPath).toString();
		CausalUAIParser.ignoreChecks = true;
		StructuralCausalModel model = (StructuralCausalModel)IO.readUAI(fullpath);
		logger.info("Loaded model from: "+fullpath);

		try {
			SelectionBias.findSelector(model);
			bias = true;
		}catch (Exception e){};


		// Load data
		fullpath = wdir.resolve(dataPath).toString();
		data = DataUtil.fromCSV(fullpath);
		int datasize = data.length;
		logger.info("Loaded "+datasize+" data instances from: "+fullpath);



		HashMap<Set<Integer>, BayesianFactor> empMap = DataUtil.getEmpiricalMap(model, data);
		logger.info("Empirical distribution from data: "+empMap.toString());

		model.fillExogenousWithRandomFactors(3);

		// Determine trainable
		int[] trainableVars = model.getExogenousVars();

		// Selection bias info
		int selectVar = -1;
		if(bias){
			selectVar = SelectionBias.findSelector(model);
			data = SelectionBias.applySelector(data, model, selectVar);
			int exoPaS = model.getExogenousParents(selectVar)[0];
			trainableVars = Arrays.stream(trainableVars).filter(v -> v!=exoPaS).toArray();

			int finalSelectVar = selectVar;
			long sizeObs = Arrays.stream(DataUtil.selectColumns(data, selectVar)).filter(d -> d.get(finalSelectVar) == 1).count();

			p1 = ((double)sizeObs)/data.length;
			logger.info("Applyied bias modifications for selector "+selectVar+". Data observed size: "+sizeObs+" out of "+data.length+". P(S=1)="+p1);

		}

		logger.info("Starting EM algorithm");

		logger.info("Trainable variables: "+ Arrays.toString(trainableVars));

		Watch.start();

		EMCredalBuilder builder = new EMCredalBuilder(model, data)
				.setMaxEMIter(maxIter)
				.setWeightedEM(weighted)
				.setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
				.setThreshold(0.0)
				.setNumTrajectories(1)
				.setTrainableVars(trainableVars)
				.setVerbose(!quiet)
				.build();


/*		EMCredalBuilder builder = new EMCredalBuilder(model, data)
				.setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
				.setThreshold(0.0)
				.setNumTrajectories(1)
				.setWeightedEM(true)
				.setVerbose(false)
				.setMaxEMIter(1).build();
*/
		long time = Watch.stop();
		int iter = builder.getTrajectories().get(0).size() - 1;
		StructuralCausalModel m = builder.getSelectedPoints().get(0);
		IO.writeUAI(m, "ratio_problem.uai");
		//HashMap<Set<Integer>, BayesianFactor> inducedDist = m.getEmpiricalMap();


		logger.info("Single EM run ("+iter+" iterations) finished in "+time+" ms.");
		double ratio = Double.NaN;
		try {
			ratio = model.ratioLogLikelihood(data);
			logger.info("Llk-ratio: " + ratio);
		}catch (Exception e){
			logger.warn("Cannot calculate ratio: "+e.getMessage());
		}



		String modelName = Path.of(modelPath).getFileName().toString().replace(".uai","");
		String outputModel = modelName+"_"+seed+".uai";
		String outputStats =  modelName+"_"+seed+".csv";


		// store model and statistics
		fullpath = wdir.resolve(output).resolve(outputModel).toString();
		logger.info("Saving precise model at "+fullpath);
		IO.writeUAI(m, fullpath);


		fullpath = wdir.resolve(output).resolve(outputStats).toString();
		logger.info("Saving statistics at at "+fullpath);
		/*int[][] stats = new int[][]{new int[]{(int) seed, datasize, (int)time, iter}};
		new WriterCSV(stats, fullpath).setVarNames("seed","datasize", "time", "iterations").write();
*/

		HashMap<String, String> stats = new HashMap<>();
		stats.put("seed", String.valueOf(seed));
		stats.put("datasize", String.valueOf(datasize));
		stats.put("time", String.valueOf(time));
		stats.put("iterations", String.valueOf(iter));
		String ratioStr = "";
		if(!Double.isNaN(ratio)) ratioStr = String.valueOf(ratio);
		stats.put("ratio", ratioStr);
		stats.put("p1", String.valueOf(p1));


		List<HashMap> res = new ArrayList<>();
		res.add(stats);
		DataUtil.toCSV(fullpath, res);

	}

}
