package ch.idsia.credici.learning.eqem;


/**
 * @param numRuns the number of global random restarts (Positive Integer)
 * @param numIterations maximum number of iterations to fix the distributions (Positive Integer)
 * @param numPSCMRuns number of run once the endogenous distribution is fixed. Number of FSCM per run. (default == 1)
 * @param numPSCMIterations number of EM iterations per run once the endogenous iterations are fixed.
 * @param deterministicEndogenous whether endogenous distributions should be degenerated to deterministic.
 */
public class Config {
	int numRuns;
	int numIterations;
	int numPSCMRuns;
	int numPSCMIterations;
	boolean deterministicEndogenous;
	
	public Config() {
		numRuns = 100;
		numIterations = 1000;
		numPSCMRuns = 100;
		numPSCMIterations = 1000;
		deterministicEndogenous = false;
	}
	
	public Config(int numRuns, int numIterations, int numPSCMRuns, int numPSCMIterations, boolean deterministicEndogenous) {
		this.numPSCMRuns = numPSCMRuns;
		this.numRuns = numRuns;
		this.numIterations = numIterations;
		this.numPSCMIterations = numPSCMIterations;
		this.deterministicEndogenous = deterministicEndogenous;
	}
	
	public Config numRun(int numRuns) { this.numRuns = numRuns; return this; }
	public Config numIterations(int numIterations) { this.numIterations = numIterations; return this; } 
	public Config numPSCMRuns(int numPSCMRuns) { this.numPSCMRuns = numPSCMRuns; return this; }
	public Config numPSCMInterations(int numPSCMIterations) { this.numPSCMIterations = numPSCMIterations; return this; }
	public Config deterministic(boolean state) { this.deterministicEndogenous = state; return this; } 
	
	public int numRuns() { return numRuns; }
	public int numIterations() { return numIterations; } 
	public int numPSCMRuns() { return numPSCMRuns; }
	public int numPSCMIterations() { return numPSCMIterations; }
	public boolean deterministic() { return deterministicEndogenous; } 
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{")
		.append("runs=").append(numRuns)
		.append(", iter=").append(numIterations)
		.append(", PSCM=").append(numPSCMRuns)
		.append(", PSCMiter=").append(numPSCMIterations)
		.append(", det=").append(deterministicEndogenous)
		.append("}");
		return sb.toString();
	}
}
