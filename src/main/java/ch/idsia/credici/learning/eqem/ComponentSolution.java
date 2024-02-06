package ch.idsia.credici.learning.eqem;

import ch.idsia.credici.model.StructuralCausalModel;

public class ComponentSolution {
	public static enum Stage { 
		FIRST_EM, FIRST_FAILED, PSCM_EM, FAILED_PSCM;
		
		public boolean success() {
			return this == FIRST_EM || this == PSCM_EM;
		}
		
		public boolean failed() {
			return !success();
		}
	};
	
	StructuralCausalModel model;
	double loglikelihood;
	double llmax;
	int run;
	int iterations; 
	int PSCMrun; 
	int PSCMiterations;
	Exception exception; 
	Stage stage;
	
	public static ComponentSolution failed(StructuralCausalModel model, Stage stage, Exception e) {
		return new ComponentSolution(model, Double.NaN, 0, 0, 0, 0, stage, e);
	}
	
	public static ComponentSolution successFirst(StructuralCausalModel model, double ll, int run, int iterations) {
		return new ComponentSolution(model, ll, run,  iterations,-1, -1, Stage.FIRST_EM, null);
	}
	
	public static ComponentSolution successPSCM(StructuralCausalModel model, double ll, int run, int iterations, int pscm, int piterations) {
		return new ComponentSolution(model, ll, run, iterations, pscm, piterations, Stage.PSCM_EM, null);
	}
	
	public ComponentSolution(
			StructuralCausalModel model, double ll, int run,  int iteration, int PSCMrun, int pscmiteration, Stage stage, Exception exception){
		this.model = model;
		this.loglikelihood = ll;
		this.run = run;
		this.PSCMrun = PSCMrun;
		this.stage = stage;
		this.iterations = iteration;
		this.PSCMiterations = pscmiteration;
		this.exception = exception;
	}
	
	public ComponentSolution() {
		this(null, 0, -1, -1, -1, -1, null, null);
	}
	
	public ComponentSolution loglikelihood(double ll) { this.loglikelihood = ll; return this; }
	public ComponentSolution run(int run) { this.run = run; return this; }
	public ComponentSolution PSCMrun(int pscmrun) { this.PSCMrun = pscmrun; return this; }
	public ComponentSolution model(StructuralCausalModel model) { this.model = model; return this; }
	public ComponentSolution stage(Stage stage) { this.stage = stage; return this;} 
	public ComponentSolution iterations(int iter) { this.iterations = iter; return this; }
	public ComponentSolution PSCMiterations(int pscmiter) { this.PSCMiterations = pscmiter; return this; }
	public ComponentSolution llmax(double llmax) { this.llmax = llmax; return this; }
	
	public StructuralCausalModel model() { return model; } 
	public double loklikelihood() { return loglikelihood; }
	public int run() { return run;}
	public int PSCMrun() { return PSCMrun; }
	public Stage stage() { return stage; } 
	public int iterations() { return iterations; }
	public int PSCMiterations() { return PSCMiterations; }
	public double llmax() { return llmax; }
}