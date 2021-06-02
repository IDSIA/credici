package edu.neurips.causalem.model.builder;

import edu.neurips.causalem.model.StructuralCausalModel;
import edu.neurips.causalem.model.info.CausalInfo;
import ch.idsia.crema.model.graphical.SparseModel;

import java.util.ArrayList;
import java.util.List;

public abstract class CredalBuilder {

	// input SCM
	protected StructuralCausalModel causalmodel;

	// output credal model
	protected SparseModel model;

	// abstract methods to implemented
	abstract public CredalBuilder build() throws InterruptedException;

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
		if(!CausalInfo.of(causalmodel).isMarkovian() && !CausalInfo.of(causalmodel).isQuasiMarkovian()){
			throw new IllegalArgumentException("Wrong markovianity");
		}
	}





}
