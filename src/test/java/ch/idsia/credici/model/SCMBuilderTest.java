package ch.idsia.credici.model;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class SCMBuilderTest {

	BayesianNetwork bnet;


	@Before
	public void init(){
		bnet = new BayesianNetwork();
		int y = bnet.addVariable(2);
		int x = bnet.addVariable(2);

		bnet.setFactor(y, new BayesianFactor(bnet.getDomain(y), new double[]{0.3,0.7}));
		bnet.setFactor(x, new BayesianFactor(bnet.getDomain(x,y), new double[]{0.6,0.5, 0.5,0.5}));

	}

	@Test
	public void buildMarkovianEqless() {
		StructuralCausalModel model =  SCMBuilder.of(bnet).build();

		assertArrayEquals(
				model.getEndogenousVars(),
				new int[]{0,1}
		);

		assertArrayEquals(
				model.getExogenousVars(),
				new int[]{2,3}
		);

		assertArrayEquals(
				model.getParents(1),
				new int[]{0,3}
		);




	}




}
