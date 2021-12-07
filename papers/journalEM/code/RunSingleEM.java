package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.data.WriterCSV;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;


public class RunSingleEM extends Terminal {

	/*
		-o ./papers/journalEM/data/data-party.csv -d 100 -s 1234 ./papers/journalEM/models/party_empirical.uai

	 -s 0 --maxiter 100 -w --output ./papers/journalEM/ouput/party/ -d ./papers/journalEM/data/data-party.csv ./papers/journalEM/models/party_causal.uai

	 -d 500 --seed 0 -o /Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalEM/data/triangolo_data_d500.csv /Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/papers/journalEM/models/triangolo_empirical.uai

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
		StructuralCausalModel model = (StructuralCausalModel)IO.readUAI(fullpath);
		logger.info("Loaded model from: "+fullpath);

		// Load data
		fullpath = wdir.resolve(dataPath).toString();
		data = DataUtil.fromCSV(fullpath);
		int datasize = data.length;
		logger.info("Loaded "+datasize+" data instances from: "+fullpath);

		HashMap empMap = DataUtil.getEmpiricalMap(model, data);
		logger.info("Empirical distribution from data: "+empMap.toString());

		model.fillExogenousWithRandomFactors(3);

		logger.info("Starting EM algorithm");
		Watch.start();

		EMCredalBuilder builder = new EMCredalBuilder(model, data)
				.setMaxEMIter(maxIter)
				.setWeightedEM(weighted)
				.setNumTrajectories(1)
				.setVerbose(false)
				.build();

		long time = Watch.stop();
		int iter = builder.getTrajectories().get(0).size() - 1;
		StructuralCausalModel m = builder.getSelectedPoints().get(0);
		HashMap inducedDist = m.getEmpiricalMap();

		logger.info("Single EM run ("+iter+" iterations) finished in "+time+" ms.");
		logger.info("Induced distribution from data: "+inducedDist.toString());

		String modelName = Path.of(modelPath).getFileName().toString().replace(".uai","");
		String outputModel = modelName+"_"+seed+".uai";
		String outputStats =  modelName+"_"+seed+".csv";


		// store model and statistics
		fullpath = wdir.resolve(output).resolve(outputModel).toString();
		logger.info("Saving precise model at "+fullpath);
		IO.writeUAI(m, fullpath);


		fullpath = wdir.resolve(output).resolve(outputStats).toString();
		logger.info("Saving statistics at at "+fullpath);
		int[][] stats = new int[][]{new int[]{datasize, (int)time, iter}};
		new WriterCSV(stats, fullpath).setVarNames("datasize", "time", "iterations").write();





	}

}
