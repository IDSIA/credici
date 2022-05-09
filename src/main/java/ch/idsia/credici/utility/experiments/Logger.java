package ch.idsia.credici.utility.experiments;

import org.apache.commons.logging.Log;

import java.io.*;

public class Logger {

	public enum Level {
		ALL(0),
		TRACE(1),
		DEBUG(2),
		INFO(3),
		WARN(4),
		ERROR(5),
		SEVERE(6),
		OFF(7);

		private int id;

		Level(int id){
			this.id = id;
		}

		public int getID(){
			return id;
		}

	};
	private Level level = Level.INFO;
	boolean toStdOutput = true;
	private boolean toFile = false;

	public static Logger global = null;

	private FileWriter fr = null;
	private BufferedWriter br = null;
	private PrintWriter pr = null;


	public Logger(){

	}

	public Logger setLogfile(String logfile) throws IOException {
		File f = new File(logfile);
		fr = new FileWriter(f, true);
		br = new BufferedWriter(fr);
		pr = new PrintWriter(br);
		toFile = true;

		this.info("Set up log file: "+f.getAbsolutePath());

		return this;
	}

	public Logger setLevel(Level level) {
		this.level = level;
		return this;
	}

	public Logger setToStdOutput(boolean toStdOutput) {
		this.toStdOutput = toStdOutput;
		return this;
	}
	public void closeFile(){
		this.info("Closing log file");
		try {
			if (toFile) {
				br.close();
				fr.close();
				pr.close();
			}

			if(this== global)
				global = null;

		} catch (IOException e) {
			e.printStackTrace();
			this.severe("Error closing log file: "+e.getMessage());
		}finally {
			toFile = false;

		}
		this.info("Closed log file");
	}

	public static void setGlobal(Logger logger){
		global = logger;
	}

	public static Logger getGlobal(){
		return global;
	}


	private void print(String msg, Level level){
		if(this.level.getID() <= level.getID() ) {
			String s = "[" + java.time.LocalDateTime.now() + "][" + level.toString() + "][java] " + msg;
			if (toStdOutput)
				System.out.println(s);
			if (toFile)
				pr.println(s);
		}

	}


	public void trace(String msg){print(msg, Level.TRACE);}
	public void debug(String msg){print(msg, Level.DEBUG);}
	public void info(String msg){print(msg, Level.INFO);}
	public void warn(String msg){print(msg, Level.WARN);}
	public void error(String msg){print(msg, Level.ERROR);}
	public void severe(String msg){print(msg, Level.SEVERE);}

	public Logger setVerbose(boolean active){
		if(active)
			return this.setLevel(Level.INFO);
		return this.setLevel(Level.OFF);
	}


	public static void main(String[] args) {
		Logger logger = new Logger();
		logger.info("Reading model at ./papers/neurips21/models/set1/chain_twExo1_nEndo4_1.uai");
	}
}
