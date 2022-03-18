package ch.idsia.credici.model.not_working;

import org.junit.Assert;
import org.junit.Test;
import ch.idsia.credici.model.modelsfortest.ChainMarkovianCase;
import ch.idsia.credici.model.modelsfortest.ChainNonMarkovianCase;

import java.io.IOException;

public class ExamplesTests {

	String[] args = null;

	@Test
	public void ChainMarkovianCase() throws InterruptedException {
		//todo: review ApproxLP
		double[] res = ChainMarkovianCase.test(args);
		double[] expected = {0.07, 0.64, 0.29000000000000004, 0.06999999999999999, 0.64, 0.29000000000000004, 0.07000929999999998, 0.6400035999999999, 0.2900070999999999, 0.06998930000000025, 0.6400164000720001, 0.290012900142};
		Assert.assertArrayEquals(expected, res, 0.0001);
	}

	@Test
	public void ChainNonMarkovianCase() throws InterruptedException, IOException {

		// todo: wrong numbers
		double[] res = ChainNonMarkovianCase.test(args);
		double[] expected = {0.09615384615384616, 0.9038461538461539, 0.09615384615384617, 0.9038461538461539, 0.09615384672553011, 0.9038461502681214, 0.09615384973187857, 0.9038461532744699};
		Assert.assertArrayEquals(expected, res, 0.0001);
	}



}
