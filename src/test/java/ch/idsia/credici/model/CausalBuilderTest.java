package ch.idsia.credici.model;

import ch.idsia.credici.factor.BayesianFactorBuilder;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.crema.factor.bayesian.BayesianFactor;

import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.assertArrayEquals;

public class CausalBuilderTest {

	BayesianNetwork bnet;
	int x, y;


	@Before
	public void init(){
		bnet = new BayesianNetwork();
		y = bnet.addVariable(2);
		x = bnet.addVariable(2);

		bnet.setFactor(y, BayesianFactorBuilder.as(bnet.getDomain(y), new double[]{0.3,0.7}));
		bnet.setFactor(x, BayesianFactorBuilder.as(bnet.getDomain(x,y), new double[]{0.6,0.5, 0.5,0.5}));

	}

	@Test
	public void buildMarkovianEqless() {
		StructuralCausalModel model =  CausalBuilder.of(bnet).build();

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


	@Test
	public void builWithCausalDAG() {

		StructuralCausalModel model =  CausalBuilder.of(bnet).build();


		DirectedAcyclicGraph causalDAG = new DirectedAcyclicGraph(DefaultEdge.class);
		causalDAG.addVertex(y);
		causalDAG.addVertex(x);

		int u = 3;
		causalDAG.addVertex(u);

		causalDAG.addEdge(y,x);
		causalDAG.addEdge(u,x);
		causalDAG.addEdge(u,y);


		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m =
				CausalBuilder.of(bnet)
						.setCausalDAG(causalDAG)
						.setFillRandomEquations(true)
						.setFillRandomExogenousFactors(4)
						.setExoVarSizes(new int[]{4}).build();


		assertArrayEquals(
				((BayesianFactor)m.getEmpiricalMap().values().toArray()[0]).getData(),
				new double[]{0.806, 0.0, 0.0, 0.194},
				0.001);


	}

	@Test
	public void buildEquations() {


		StructuralCausalModel model =  CausalBuilder.of(bnet).build();
		int ux = model.getExogenousParents(x)[0];
		int uy = model.getExogenousParents(y)[0];


	/*			u0	u1	u2	u4
			x0	0	0	0	1
		y0	x1	1	1	1	0
			---------------------
			x0	1	0	1	0
		y1	x1	0	1	0	1

	 */

		BayesianFactor eq = null;
		eq = EquationBuilder.of(model).fromVector(x, 1,0, 1,1, 1,0, 0,1);

		assertArrayEquals(
				eq.filter(x,0).filter(y, 0).getData(),
				new double[]{0.0, 0.0, 0.0, 1.0},
				0.000001
		);

		assertArrayEquals(
				eq.filter(x,0).filter(y, 1).getData(),
				new double[]{1.0, 0.0, 1.0, 0.0},
				0.000001
		);



	}


}
