package ch.idsia.credici.learning.eqem;

/**
 * @param numRuns                 the number of global random restarts (Positive
 *                                Integer)
 * @param numIterations           maximum number of iterations to fix the
 *                                distributions (Positive Integer)
 * @param numPSCMRuns             number of run once the endogenous distribution
 *                                is fixed. Number of FSCM per run. (default ==
 *                                1)
 * @param numPSCMIterations       number of EM iterations per run once the
 *                                endogenous iterations are fixed.
 * @param deterministicEndogenous whether endogenous distributions should be
 *                                degenerated to deterministic.
 */
public class Config {
//	StructuralCausalModel priorModel; 
//	DoubleTable data;
	private boolean randomize;

	private int numRuns;
	private int maxRuns;

	private int numIterations;
	private int numPSCMRuns;
	private int numPSCMIterations;
	private boolean deterministicEndogenous;

	private long seed;
	private long nextseed;
	private double alpha;

	/**
	 * Maximum difference in loglikelihood to accept a solution. Note that
	 * difference in log likelihood is ratio of likelihood
	 */
	private double llEPS;

	/**
	 * Should endogenous variables be freed?
	 */
	private boolean freeEndogenous;

	public Config() {
//		data = null;
//		priorModel = null;
		numRuns = maxRuns = 100;
		numIterations = 1000;
		numPSCMRuns = 100;
		numPSCMIterations = 1000;
		deterministicEndogenous = false;
		nextseed = seed = System.nanoTime();
		llEPS = 0.000001;
		freeEndogenous = true;
		alpha = 1;
	}

	public Config(int numRuns, int numIterations, int numPSCMRuns, int numPSCMIterations,
			boolean deterministicEndogenous) {
		this();
		this.numPSCMRuns = numPSCMRuns;
		this.numRuns = this.maxRuns = numRuns;
		this.numIterations = numIterations;
		this.numPSCMIterations = numPSCMIterations;
		this.deterministicEndogenous = deterministicEndogenous;
	}
//	public Config priorModel(StructuralCausalModel model) { this.priorModel = model; return this; }

	public Config numRun(int numRuns) {
		this.numRuns = numRuns;
		if (this.maxRuns < numRuns)
			this.maxRuns = numRuns;
		return this;
	}

	public Config maxRun(int maxRuns) {
		this.maxRuns = maxRuns;
		if (this.numRuns > maxRuns)
			this.numRuns = maxRuns;
		return this;
	}

	public Config numIterations(int numIterations) {
		this.numIterations = numIterations;
		return this;
	}

	public Config numPSCMRuns(int numPSCMRuns) {
		this.numPSCMRuns = numPSCMRuns;
		return this;
	}

	public Config numPSCMInterations(int numPSCMIterations) {
		this.numPSCMIterations = numPSCMIterations;
		return this;
	}

	public Config deterministic(boolean state) {
		this.deterministicEndogenous = state;
		return this;
	}

	public Config seed(long seed) {
		this.seed = seed;
		this.nextseed = seed;
		return this;
	}

	public Config llEPS(double eps) {
		this.llEPS = eps;
		return this;
	}

	public Config freeEndogenous(boolean free) {
		this.freeEndogenous = free;
		return this;
	}

	public Config randomize(boolean state) {
		this.randomize = state;
		return this;
	}

	public Config alpha(double alpha) {
		this.alpha = alpha;
		return this;
	}

//	public StructuralCausalModel priorModel() { return priorModel; }
	public int numRuns() {
		return numRuns;
	}

	public int maxRuns() {
		return maxRuns;
	}

	public int numIterations() {
		return numIterations;
	}

	public int numPSCMRuns() {
		return numPSCMRuns;
	}

	/**
	 * The number of PSCM iterations. Run after the initial EM. the PSCM iteration
	 * only attempt to optimize the Exogenous variables
	 * 
	 * @return
	 */
	public int numPSCMIterations() {
		return numPSCMIterations;
	}

	/**
	 * Should we attempt to make the final solution deterministic?
	 * 
	 * @return
	 */
	public boolean deterministic() {
		return deterministicEndogenous;
	}

	public long seed() {
		return seed;
	}

	public long nextSeed() {
		return nextseed++;
	}

	public double llEPS() {
		return llEPS;
	}

	public boolean freeEndogenous() {
		return freeEndogenous;
	}

	public double alpha() {
		return alpha;
	}

	public boolean randomize() {
		return randomize;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{").append("runs=").append(numRuns).append(", iter=").append(numIterations).append(", PSCM=")
				.append(numPSCMRuns).append(", PSCMiter=").append(numPSCMIterations).append(", det=")
				.append(deterministicEndogenous).append("}");
		return sb.toString();
	}

	public String toCSVString() {
		return "";
	}
}
