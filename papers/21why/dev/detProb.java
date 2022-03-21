package dev;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;

public class detProb {
	public static void main(String[] args) {
		StructuralCausalModel m = new StructuralCausalModel();
		int v = m.addVariable(1,true);
		int x1 = m.addVariable(2, false);
		m.addParents(x1, v);

		m.setFactor(x1, BayesianFactor.deterministic(m.getDomain(x1), m.getDomain(v), 0));
		m.setFactor(v, new BayesianFactor(m.getDomain(v), new double[]{1}));

		System.out.println(m.getEmpiricalMap());
	}
}
