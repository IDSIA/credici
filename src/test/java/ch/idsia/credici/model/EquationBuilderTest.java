package ch.idsia.credici.model;

import org.junit.Test;

public class EquationBuilderTest {


	@Test
	public void conservative(){
		//todo: uncomment when conservative is fixed

		/*
		// Define the model
		StructuralCausalModel m = new StructuralCausalModel();

		int x = m.addVariable(2, false);
		int y = m.addVariable(2, false);
		int z = m.addVariable(2, false);
		int a = m.addVariable(2, false);

		int u = m.addVariable(16, true);
		int w = m.addVariable(2, true);
		int v = m.addVariable(4, true);

		m.addParents(a, w);
		m.addParents(x, a,v);
		m.addParents(y,x,u);
		m.addParents(z,y,u);

		// For each exogenous variable, calculate the SEs of the children
		for(int exoVar : m.getExogenousVars()) {

			// Calculate the joint SE
			BayesianFactor f = BayesianFactor.combineAll(
					EquationBuilder.of(m).withAllAssignmentsQM(exoVar).values()
			);

			double data[] = f.marginalize(exoVar).getData();
			double unique = Arrays.stream(data).distinct().count();

			// The result of marginalizing the exogenous from the joint SE must be a factor with the same value
			// for each position
			Assert.assertEquals(1, unique, 0.0);
		}
	*/
	}
}
