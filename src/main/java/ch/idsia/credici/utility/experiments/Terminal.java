package ch.idsia.credici.utility.experiments;

import picocli.CommandLine;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;


public abstract class Terminal implements Runnable{

	// Generic arguments
	@CommandLine.Option(names={"-l", "--logfile"}, description = "Output file for the logs.")
	String logfile = null;

	@CommandLine.Option(names={"-q", "--quiet"}, description = "Controls if log messages are printed to standard output.")
	boolean quiet;


	@CommandLine.Option(names={"--debug"}, description = "Debug flag. Defaults to false")
	boolean debug = false;

	@CommandLine.Option(names = {"-s", "--seed"}, description = "Random seed. If not specified, it is randomly selected.")
	protected long seed = -1;



	protected static String errMsg = "";
	protected static String argStr;
	protected Logger logger = null;


	@Override
	public void run(){


		try {
			if(seed<0)
				seed = Timestamp.from(Instant.now()).getNanos()/1000;
			setUpIO();
			this.entryPoint();
		}catch (Exception e){
			errMsg = e.toString();
			logger.severe(errMsg);
			if(debug)
				e.printStackTrace();
		}catch (Error e){
			errMsg = e.toString();
			logger.severe(errMsg);
			if(debug)
				e.printStackTrace();
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
		if(argStr!=null)
			logger.info("args: "+argStr);


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

	abstract protected void entryPoint() throws Exception;


}
