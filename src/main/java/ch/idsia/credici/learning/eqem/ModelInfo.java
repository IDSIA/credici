package ch.idsia.credici.learning.eqem;

import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.model.GraphicalModel;

public class ModelInfo<F extends Factor<F>, T extends GraphicalModel<F>> {
	public static enum Stage { 
		FIRST_EM, FIRST_FAILED, PSCM_EM, FAILED_PSCM;
		
		public boolean success() {
			return this == FIRST_EM || this == PSCM_EM;
		}
		
		public boolean failed() {
			return !success();
		}
	};
	
	boolean accepted;
	String title;
	T model;
	double loglikelihood;
	double llmax;
	int run;
	int iterations; 
	int PSCMrun; 
	int PSCMiterations;
	Exception exception; 
	Stage stage;
	Integer componentId;
	
	public static <F extends Factor<F>, T extends GraphicalModel<F>> ModelInfo<F,T> failed(T model, Stage stage, Exception e) {
		return new ModelInfo<>(model, Double.NaN, 0, 0, 0, 0, stage, e);
	}
	
	public static <F extends Factor<F>, T extends GraphicalModel<F>> ModelInfo<F,T> successFirst(T model, double ll, int run, int iterations) {
		return new ModelInfo<>(model, ll, run,  iterations,-1, -1, Stage.FIRST_EM, null);
	}
	
	public static <F extends Factor<F>, T extends GraphicalModel<F>> ModelInfo<F,T> successPSCM(T model, double ll, int run, int iterations, int pscm, int piterations) {
		return new ModelInfo<>(model, ll, run, iterations, pscm, piterations, Stage.PSCM_EM, null);
	}
	
	public ModelInfo(
			T model, double ll, int run,  int iteration, int PSCMrun, int pscmiteration, Stage stage, Exception exception){
		this.model = model;
		this.loglikelihood = ll;
		this.run = run;
		this.PSCMrun = PSCMrun;
		this.stage = stage;
		this.iterations = iteration;
		this.PSCMiterations = pscmiteration;
		this.exception = exception;
		accepted = false;
	}
	
	public ModelInfo() {
		this(null, 0, -1, -1, -1, -1, null, null);
	}
	
	public ModelInfo<F,T> loglikelihood(double ll) { this.loglikelihood = ll; return this; }
	public ModelInfo<F,T> run(int run) { this.run = run; return this; }
	public ModelInfo<F,T> PSCMrun(int pscmrun) { this.PSCMrun = pscmrun; return this; }
	public ModelInfo<F,T> model(T model) { this.model = model; return this; }
	public ModelInfo<F,T> stage(Stage stage) { this.stage = stage; return this;} 
	public ModelInfo<F,T> iterations(int iter) { this.iterations = iter; return this; }
	public ModelInfo<F,T> PSCMiterations(int pscmiter) { this.PSCMiterations = pscmiter; return this; }
	public ModelInfo<F,T> llmax(double llmax) { this.llmax = llmax; return this; }
	public ModelInfo<F,T> componentId(Integer id) { this.componentId = id; return this; }
	public ModelInfo<F,T> title(String s) { this.title = s; return this; }
	public ModelInfo<F,T> accept() { this.accepted = true; return this; } 
	public ModelInfo<F,T> reject() { this.accepted = false; return this; } 
	
	public T getModel() { return model; } 
	public double getLogLikelihood() { return loglikelihood; }
	public int getRun() { return run;}
	public int getPSCMrun() { return PSCMrun; }
	public Stage getStage() { return stage; } 
	public int getIterations() { return iterations; }
	public int getPSCMiterations() { return PSCMiterations; }
	public double getLLmax() { return llmax; }
	public Integer getComponentId() { return componentId; }
	public String getTitle() { return this.title; }
	public boolean isAccepted() { return accepted; }
	
}