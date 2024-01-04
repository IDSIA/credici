package ch.idsia.credici.learning.eqem;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.table.Table;
import ch.idsia.crema.factor.bayesian.BayesianFactor;

public class EQEMLearner {
	private StructuralCausalModel model;
	private Randomizer random;
	private Table data; 
	
	public EQEMLearner(StructuralCausalModel prior) {
		this.random = new Randomizer(0);
		this.model = prior.copy();
		
		for (int variable : this.model.getVariables()) {
			BayesianFactor bf = this.model.getFactor(variable);
			random.randomizeInplace(bf, variable);
		}
	}
	
	public void run() {
		CComponents cc = new CComponents();
		List<Pair<StructuralCausalModel, Table>> components = cc.apply(model, data);
		
	}
}
