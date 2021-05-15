package ch.idsia.credici.utility.experiments;

import java.io.*;

public class Logger {

	boolean verbose = true;
	private boolean toFile = false;


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

	public Logger setVerbose(boolean verbose) {
		this.verbose = verbose;
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
		} catch (IOException e) {
			e.printStackTrace();
			this.severe("Error closing log file: "+e.getMessage());
		}finally {
			toFile = false;

		}
	}


	private void print(String msg, String type){
		String s = "["+java.time.LocalDateTime.now()+"]["+type+"][java] "+msg;
		if(verbose)
			System.out.println(s);

		if(toFile)
			pr.println(s);

	}

	public void info(String msg){
		print(msg, "INFO");
	}

	public void severe(String msg){
		print(msg, "SEVERE");
	}
	public static void main(String[] args) {
		Logger logger = new Logger();
		logger.info("Reading model at ./papers/neurips21/models/set1/chain_twExo1_nEndo4_1.uai");
	}
}
