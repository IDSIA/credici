package ch.idsia.credici.model;

import org.junit.Assert;
import org.junit.Test;
import tests.ChainMarkovianCase;
import tests.ChainNonMarkovianCase;

public class ExamplesTests {

	String[] args = null;

	@Test
	public void ChainMarkovianCase() throws InterruptedException {
		double[] res = ChainMarkovianCase.test(args);
		double[] expected = {0.07, 0.64, 0.29000000000000004, 0.06999999999999999, 0.64, 0.29000000000000004, 0.07000929999999998, 0.6400035999999999, 0.2900070999999999, 0.06998930000000025, 0.6400164000720001, 0.290012900142};
		Assert.assertArrayEquals(expected, res, 0.0001);
	}

	@Test
	public void ChainNonMarkovianCase() throws InterruptedException {
		double[] res = ChainNonMarkovianCase.test(args);
		double[] expected = {0.2857142857142857, 0.7142857142857143, 0.28571428571428575, 0.7142857142857142, 0.28575397509301564, 0.7142008086996112, 0.28579919130038883, 0.7142460249069844};
		Assert.assertArrayEquals(expected, res, 0.0001);
	}



}
