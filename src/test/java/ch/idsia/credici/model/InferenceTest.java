package ch.idsia.credici.model;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;
import jdk.jshell.spi.ExecutionControl;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

public class InferenceTest {


	@Test
	public void CredalCausalVE() throws InterruptedException, ExecutionControl.NotImplementedException {

		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m = RandomChainNonMarkovian.buildModel(4, 2, 6);

		int x = 1;
		int y = 3;

		double[] expected, actual;

		CredalCausalVE inf = new CredalCausalVE(m, m.getEmpiricalProbs());

		expected = new double[]{0.97, 0.03};
		actual = ((VertexFactor)inf.causalQuery().setTarget(y).setIntervention(x, 0).run()).getData()[0][0];
		Assert.assertArrayEquals(expected,actual,0.000001);


		expected = new double[]{0.010309278350515464};
		actual = inf.probNecessity(x,y).getData()[0][0];
		Assert.assertArrayEquals(expected,actual,0.000001);


		expected = new double[]{0.25};
		actual = inf.probSufficiency(x,y).getData()[0][0];
		Assert.assertArrayEquals(expected,actual,0.000001);


		expected = new double[]{0.01};
		actual = inf.probNecessityAndSufficiency(x,y).getData()[0][0];
		Assert.assertArrayEquals(expected,actual,0.000001);


	}
	@Test
	public void CredalCausalApproxLP() throws InterruptedException {
		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m = RandomChainNonMarkovian.buildModel(4, 2, 6);

		int x = 1;
		int y = 3;

		double[] expected, actual;

		CredalCausalApproxLP inf = new CredalCausalApproxLP(m, m.getEmpiricalProbs());

		expected = new double[]{0.97, 0.03};
		actual = ((IntervalFactor)inf.causalQuery().setTarget(y).setIntervention(x, 0).run()).getDataUpper()[0];
		Assert.assertArrayEquals(expected,actual,0.000001);



	}

	@Test
	public void CausalVE() throws ExecutionControl.NotImplementedException, InterruptedException {

		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m = RandomChainNonMarkovian.buildModel(4, 2, 6);

		int x = 1;
		int y = 3;

		double[] expected, actual;

		CausalVE inf = new CausalVE(m);

		expected = new double[]{0.9700000000000001, 0.03 };
		actual = ((BayesianFactor)inf.causalQuery().setTarget(y).setIntervention(x, 0).run()).getData();
		Assert.assertArrayEquals(expected,actual,0.000001);
/*

	expected = new double[]{0.010309278350515464};
	actual = inf.probNecessity(x,y).getData()[0][0];
	Assert.assertArrayEquals(expected,actual,0.000001);


	expected = new double[]{0.25};
	actual = inf.probSufficiency(x,y).getData()[0][0];
	Assert.assertArrayEquals(expected,actual,0.000001);
*/

		expected = new double[]{0.01};
		actual = inf.probNecessityAndSufficiency(x,y).getData();
		Assert.assertArrayEquals(expected,actual,0.000001);

	}
}
