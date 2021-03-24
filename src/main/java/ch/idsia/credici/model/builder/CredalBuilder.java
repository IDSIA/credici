package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.crema.model.graphical.SparseModel;
import gnu.trove.map.hash.THashMap;

public abstract class CredalBuilder {

	// input SCM
	protected  StructuralCausalModel causalmodel;

	// output credal model
	protected SparseModel model;

	// abstract methods to implemented
	abstract public CredalBuilder build() throws InterruptedException;

	// method for getting the generated model.
	public SparseModel getModel() {
		return model;
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
