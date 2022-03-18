package ch.idsia.credici.model.not_working;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.model.graphical.DAGModel;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import jdk.jshell.spi.ExecutionControl;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

//todo: EMbuilder error in small value
public class BuilderTest {


	@Test
	public void ExactCredalBuilderV() throws InterruptedException, ExecutionControl.NotImplementedException {

		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m = RandomChainNonMarkovian.buildModel(5, 2, 6);

		int x = 1;
		int y = 3;

		BayesianFactor[] empProbs = m.getEmpiricalProbs();

		// Build the credal model from
		DAGModel<VertexFactor> vm = ExactCredalBuilder.of(m)
				.setToVertex()
				.setEmpirical(empProbs)
				.build().getModel();

		// Check the extructure
		Assert.assertFalse(CausalInfo.of(vm).isMarkovian());
		Assert.assertArrayEquals(
				new int[]{ 5, 6, 7 },
				CausalInfo.of(vm).getExogenousVars()
		);

		double[] expected, actual;

		// Check the credal sets of each P(U)
		expected = new double[]{0.0, 0.03, 0.86, 0.10999999999999999, 0.0, 0.0, 0.86, 0.03, 0.0, 0.10999999999999999, 0.0, 0.0, 0.86, 0.0, 0.0, 0.10999999999999999, 0.03, 0.0, 0.0, 0.03, 0.0, 0.10999999999999999, 0.0, 0.86, 0.0, 0.0, 0.0, 0.10999999999999999, 0.03, 0.86, 0.0, 0.0, 0.86, 0.10999999999999999, 0.03, 0.0};
		actual = Doubles.concat(((VertexFactor) vm.getFactor(5)).getData()[0]);
		Doubles.sortDescending(actual);
		Doubles.sortDescending(expected);
		Assert.assertArrayEquals(expected,actual,0.000001);

		expected = new double[]{0.76, 0.0, 0.2, 0.01, 0.03, 0.0, 0.76, 0.0, 0.0, 0.01, 0.03, 0.2, 0.0, 0.76, 0.2, 0.01, 0.03, 0.0, 0.0, 0.76, 0.0, 0.01, 0.03, 0.2};
		actual = Doubles.concat(((VertexFactor) vm.getFactor(6)).getData()[0]);
		Doubles.sortDescending(actual);
		Doubles.sortDescending(expected);
		Assert.assertArrayEquals(expected,actual,0.000001);

		expected = new double[]{0.02, 0.0, 0.29, 0.0, 0.0, 0.69, 0.0, 0.0, 0.29, 0.0, 0.02, 0.69, 0.0, 0.29000000000000004, 0.0, 0.0, 0.31, 0.4, 0.31, 0.29000000000000004, 0.0, 0.0, 0.0, 0.4, 0.0, 0.0, 0.0, 0.29, 0.02, 0.69, 0.02, 0.0, 0.0, 0.29, 0.0, 0.69};
		actual = Doubles.concat(((VertexFactor) vm.getFactor(7)).getData()[0]);
		Doubles.sortDescending(actual);
		Doubles.sortDescending(expected);
		Assert.assertArrayEquals(expected,actual,0.000001);

	}


	@Test
	public void EMbuilder() throws ExecutionControl.NotImplementedException, InterruptedException {

		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m = RandomChainNonMarkovian.buildModel(5, 2, 6);

		int x = 0;
		int y = 4;



		List points =
				EMCredalBuilder.of(m)
						.build()
						.getSelectedPoints();


		double[] expected, actual;

		CausalMultiVE inf = new CausalMultiVE(points);

		expected = new double[]{ 1.9556621461757713E-5, 0.0017321884419042996 };
		VertexFactor pns = ((VertexFactor)inf.probNecessityAndSufficiency(x,y));
		actual = new double[]{pns.getData()[0][0][0], pns.getData()[0][1][0]};
		Doubles.sortDescending(actual);
		Doubles.sortDescending(expected);

		Assert.assertArrayEquals(expected,actual,0.000001);


	}

}
