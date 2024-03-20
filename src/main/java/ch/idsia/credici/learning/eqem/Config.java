package ch.idsia.credici.learning.eqem;

/**
 * @param numRuns the number of global random restarts (Positive Integer)
 * @param numIterations maximum number of iterations to fix the distributions (Positive Integer)
 * @param numPSCMRuns number of run once the endogenous distribution is fixed. Number of FSCM per run. (default == 1)
 * @param numPSCMIterations number of EM iterations per run once the endogenous iterations are fixed.
 * @param deterministicEndogenous whether endogenous distributions should be degenerated to deterministic.
 */
public class Config {
//	StructuralCausalModel priorModel; 
//	DoubleTable data;
	
	int numRuns;
	int maxRuns;
	
	int numIterations;
	int numPSCMRuns;
	int numPSCMIterations;
	boolean deterministicEndogenous;
	long seed;
	double llEPS; 
	
	boolean freeEndogenous;
	
	public Config() {
//		data = null;
//		priorModel = null;
		numRuns = maxRuns = 100;
		numIterations = 1000;
		numPSCMRuns = 100;
		numPSCMIterations = 1000;
		deterministicEndogenous = false;
		seed = System.currentTimeMillis();
		llEPS = 0.00001;
		freeEndogenous = true;
	}
	
	public Config(int numRuns, int numIterations, int numPSCMRuns, int numPSCMIterations, boolean deterministicEndogenous) {
		this();
		this.numPSCMRuns = numPSCMRuns;
		this.numRuns = this.maxRuns = numRuns;
		this.numIterations = numIterations;
		this.numPSCMIterations = numPSCMIterations;
		this.deterministicEndogenous = deterministicEndogenous;
	}
//	public Config priorModel(StructuralCausalModel model) { this.priorModel = model; return this; }
	
	public Config numRun(int numRuns) { this.numRuns = numRuns; if (this.maxRuns < numRuns) this.maxRuns = numRuns; return this; }
	public Config maxRun(int maxRuns) { this.maxRuns = maxRuns; if (this.numRuns > maxRuns) this.numRuns = maxRuns; return this; }
	
	public Config numIterations(int numIterations) { this.numIterations = numIterations; return this; } 
	public Config numPSCMRuns(int numPSCMRuns) { this.numPSCMRuns = numPSCMRuns; return this; }
	public Config numPSCMInterations(int numPSCMIterations) { this.numPSCMIterations = numPSCMIterations; return this; }
	public Config deterministic(boolean state) { this.deterministicEndogenous = state; return this; } 
	public Config seed(long seed) { this.seed = seed; return this; } 
	public Config llEPS(double eps) { this.llEPS = eps; return this; }
	public Config freeEndogenous(boolean free) { this.freeEndogenous = free; return this; }
	
//	public StructuralCausalModel priorModel() { return priorModel; }
	public int numRuns() { return numRuns; }
	public int maxRuns() { return maxRuns; }
	public int numIterations() { return numIterations; } 
	public int numPSCMRuns() { return numPSCMRuns; }
	public int numPSCMIterations() { return numPSCMIterations; }
	public boolean deterministic() { return deterministicEndogenous; } 
	public long seed() { return seed; } 
	public double llEPS() { return llEPS; }
	public boolean freeEndogenous() { return freeEndogenous; }
	
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
	
	public String toCSVString() {
		return "";
	}
}
