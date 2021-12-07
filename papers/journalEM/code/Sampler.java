package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Sampler extends Terminal {

	/*
	-o ./papers/journalEM/data/data-party.csv -d 100 -s 1234 ./papers/journalEM/models/party_empirical.uai
	 */

	@CommandLine.Parameters(description = "Model path in UAI format.")
	private String modelPath;

	@CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results.")
	String outputPath = "output.csv";


	@CommandLine.Option(names = {"-d", "--datasize"}, description = "Size of the sampled data. Default 1000")
	private int size = 1000;

	public static void main(String[] args) {
		argStr = String.join(";", args);
		CommandLine.run(new Sampler(), args);
		if(errMsg!="")
			System.exit(-1);
	}

	@Override
	protected void entryPoint() throws IOException {

		Path wdir = Paths.get(".");

		RandomUtil.setRandomSeed(seed);
		logger.info("Starting logger with seed "+seed);

		TIntIntMap[] data = null;

		String fullpath = wdir.resolve(modelPath).toString();
		Object m = IO.readUAI(fullpath);
		logger.info("Loading model from: "+fullpath);
		logger.info("Generating "+size+" samples");
		if(m instanceof BayesianNetwork){
			logger.info("Model is a BayesianNetwork");
			BayesianNetwork bnet = (BayesianNetwork) m;
			data = bnet.samples(size);
		}else{
			throw new IllegalArgumentException("Wrong input network type");
		}

		fullpath = wdir.resolve(outputPath).toString();
		logger.info("Saving data at:" +fullpath);
		DataUtil.toCSV(fullpath, data);
	}

}
