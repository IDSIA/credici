package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.CausalInfo;
import ch.idsia.crema.model.graphical.SparseModel;

import java.util.ArrayList;
import java.util.List;

public abstract class CredalBuilder {

	// input SCM
	protected  StructuralCausalModel causalmodel;

	// output credal model
	protected SparseModel model;

	// abstract methods to implemented
	public abstract CredalBuilder build(int...exoVars) throws InterruptedException;

	// method for getting the generated model.
	public SparseModel getModel() {
		return model;
	}

	public List<Integer> getUnfeasibleNodes(){
		List<Integer> out = new ArrayList<>();
		for(int u : causalmodel.getExogenousVars()){
			if(model.getFactor(u) == null)
				out.add(u);
		}
		return out;
	}

	protected void assertTrueMarginals(){
		for(int u: causalmodel.getExogenousVars()){
			if(causalmodel.getFactor(u) == null)
				throw new IllegalArgumentException("Empirical factors should be provided if true marginals are not in the SCM");
		}
	}


	protected void assertMarkovianity(){
		if(!causalmodel.isMarkovian() && !causalmodel.isQuasiMarkovian()){
			throw new IllegalArgumentException("Wrong markovianity");
		}
	}





}
