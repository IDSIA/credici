package ch.idsia.credici.utility.experiments;

import com.opencsv.exceptions.CsvException;
import picocli.CommandLine;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;


public abstract class Terminal implements Runnable{

	// Generic arguments
	@CommandLine.Option(names={"-l", "--logfile"}, description = "Output file for the logs.")
	String logfile = null;

	@CommandLine.Option(names={"-q", "--quiet"}, description = "Controls if log messages are printed to standard output.")
	boolean quiet;


	protected static String errMsg = "";
	protected static String argStr;
	protected Logger logger = null;


	@Override
	public void run(){


		try {
			setUpIO();
			this.entryPoint();

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

	abstract protected void entryPoint() throws Exception;


}
