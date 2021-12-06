import ch.idsia.credici.IO;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import picocli.CommandLine;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Sampler implements Runnable{

	/*
	-o ./misc_ig/data-party.csv -d 100 -s 1234 ./models/party-empirical.uai
	 */

	// Generic arguments
	@CommandLine.Option(names={"-l", "--logfile"}, description = "Output file for the logs.")
	String logfile = null;

	@CommandLine.Option(names={"-q", "--quiet"}, description = "Controls if log messages are printed to standard output.")
	boolean quiet;

	// Specific parameters

	@CommandLine.Parameters(description = "Model path in UAI format.")
	private String modelPath;

	@CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results.")
	String outputPath = "output.csv";

	@CommandLine.Option(names = {"-s", "--seed"}, description = "Random seed. Default 0")
	private long seed = 0;


	@CommandLine.Option(names = {"-d", "--datasize"}, description = "Size of the sampled data. Default 1000")
	private int size = 1000;

	private static String errMsg = "";
	private static String argStr;
	Logger logger = null;


	public void sampling() throws IOException {

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




	/////

	public static void main(String[] args) {
		argStr = String.join(";", args);
		CommandLine.run(new Sampler(), args);
		if(errMsg!="")
			System.exit(-1);
	}

	@Override
	public void run(){
		try {
			setUpIO();
			sampling();
		}catch (Exception e){
			errMsg = e.toString();
			logger.severe(errMsg);
			//e.printStackTrace();

		}catch (Error e){
			errMsg = e.toString();
			logger.severe(errMsg);
		}finally {
			// todo: actions that should always be done
			if(logger!=null)
				logger.closeFile();
		}
	}

	protected void setUpIO() throws IOException {
		disableWarning();
		// Set up the verbose and output files
		logger = new Logger().setVerbose(!quiet);
		if(logfile!=null)
			logger.setLogfile(logfile);
		logger.info("Set up logging");
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
