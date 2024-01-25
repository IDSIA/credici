package ch.idsia.credici.learning.eqem;

import ch.idsia.credici.model.StructuralCausalModel;

public class ComponentSolution {
	enum Stage { 
		FIRST_EM, FIRST_FAILED, PSCM_EM, FAILED_PSCM;
		
		boolean success() {
			return this == FIRST_EM || this == PSCM_EM;
		}
	};
	
	StructuralCausalModel model;
	double loglikelihood;
	int run;
	int PSCMrun; 
	
	Stage stage;
	
	public ComponentSolution(StructuralCausalModel model, double ll, int run, int PSCMrun, Stage stage){
		this.model = model;
		this.loglikelihood = ll;
		this.run = run;
		this.PSCMrun = PSCMrun;
		this.stage = stage;
	}
	
	public ComponentSolution() {
		this(null, 0, -1, -1, null );
	}
	
	public ComponentSolution loglikelihood(double ll) { this.loglikelihood = ll; return this; }
	public ComponentSolution run(int run) { this.run = run; return this; }
	public ComponentSolution PSCMrun(int pscmrun) { this.PSCMrun = pscmrun; return this; }
	public ComponentSolution model(StructuralCausalModel model) { this.model = model; return this; }
	public ComponentSolution stage(Stage stage) { this.stage = stage; return this;} 
	
	public StructuralCausalModel model() { return model; } 
	public double loklikelihood() { return loglikelihood; }
	public int run() { return run;}
	public int PSCMrun() { return PSCMrun; }
	public Stage stage() { return stage; } 
}