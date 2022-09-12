package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;


public class CalculatePNS extends Terminal {

	/*
	--cause 9 --effect 0 --descr triangolo_ ./papers/journalEM/output/triangolo/1000/
	 */

	@CommandLine.Parameters(description = "Folder with a set of model or a single model path in UAI format.")
	private String modelPath;

	@CommandLine.Option(names = {"-c", "--cause"}, description = "Cause variable. Default to 0.")
	private int cause = 1;

	@CommandLine.Option(names = {"-e", "--effect"}, description = "Effect variable. Default to 1.")
	private int effect = 0;

	@CommandLine.Option(names = {"-d", "--descr"}, description = "Label describing the network.")
	private String d = "";

	public static void main(String[] args) {
		argStr = String.join(";", args);
		CommandLine.run(new CalculatePNS(), args);
		if(errMsg!="")
			System.exit(-1);
	}

	@Override
	protected void entryPoint() throws IOException, ExecutionControl.NotImplementedException, InterruptedException {

		Path wdir = Paths.get(".");


		String fullModelPath = wdir.resolve(modelPath).toAbsolutePath().toString();

		logger.info("full path:"+fullModelPath);
		String label = "triangolo_biashard";

		String outputFolder = null;
		Logger logger = new Logger();
		List<HashMap> results = new ArrayList<>();
		CausalUAIParser.ignoreChecks = true;

		String[] paths = null;
		if(new File(fullModelPath).isDirectory()){
			paths = (String[]) Files.walk(Paths.get(fullModelPath))
					.map(p -> p.toString())
					.filter(p -> p.endsWith(".uai"))
					.toArray(String[]::new);
			outputFolder = fullModelPath;
		}else if(fullModelPath.endsWith(".uai")){
			paths = new String[]{fullModelPath};
			outputFolder = new File(fullModelPath).getParent().toString();
		}else{
			throw new IllegalArgumentException("Wrong model path");
		}



		logger.info("Reading "+paths.length+" models");

		int i=0;

		for(String f : paths) {
			logger.info("Reading "+f);
			StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(f);

			//						0		1			2				3		4			5				6			7		   8		  9				10				11
			//String[] labels = {"Death", "Symptoms", "PPreference", "FAwareness", "Age", "Practitioner", "FSystem", "Triangolo", "Hospital", "PAwareness", "Karnofsky", "FPreference"
			// where Y is "Death" (0) and X is Awareness_Patient (9) or Awareness_Famility (3) or Triangolo (7)

			CausalVE inf = new CausalVE(model);
			double pns = inf.probNecessityAndSufficiency(cause, effect).getData()[0];
			logger.info("PNS: "+pns);
			HashMap r = new HashMap();
			r.put("model", Stream.of(f.split("/")).reduce((first, last)->last).get());
			r.put("PNS", pns);
			r.put("cause", cause);
			r.put("effect", effect);

			results.add(r);
			//i++;
			//if(i>5) break;

		}

		String fullpath = String.valueOf(Path.of(outputFolder, label+"results_cause"+cause+"_effect"+effect+".csv"));
		logger.info("Saving results at "+fullpath);
		DataUtil.toCSV(fullpath, results);



	}

}
