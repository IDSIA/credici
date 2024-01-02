package ch.idsia.credici.model;

import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

public class UtilityTest {


	@Test
	public void DAGUtil() throws InterruptedException {

		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m = RandomChainNonMarkovian.buildModel(5, 2, 6);


		SparseDirectedAcyclicGraph causalDAG = m.getNetwork();
		SparseDirectedAcyclicGraph endoDAG = m.getEmpiricalNet().getNetwork();
		SparseDirectedAcyclicGraph exoDAG = m.getExogenousDAG();

		//DAGUtil

		int[] expected, actual;

		actual = DAGUtil.nodesDifference(causalDAG, endoDAG);
		expected = new int[]{5,6,7};
		Assert.assertArrayEquals(expected,actual);

		actual = DAGUtil.nodesIntersection(causalDAG, endoDAG);
		expected = new int[]{0, 1, 2, 3, 4 };
		Assert.assertArrayEquals(expected,actual);

		Assert.assertTrue(DAGUtil.isContained(endoDAG, causalDAG));
		Assert.assertTrue(!DAGUtil.isContained(causalDAG, endoDAG));


		actual = DAGUtil.getTopologicalOrder(causalDAG);
		expected = new int[]{ 5, 6, 7, 0, 1, 2, 3, 4 };
		Assert.assertArrayEquals(expected,actual);

		actual = DAGUtil.getTopologicalOrder(endoDAG);
		expected = new int[]{ 0, 1, 2, 3, 4 };
		Assert.assertArrayEquals(expected,actual);

		actual = DAGUtil.getTopologicalOrder(exoDAG);
		expected = new int[]{ 5, 6, 7, 0, 1, 2, 3, 4 };
		Assert.assertArrayEquals(expected,actual);


	}

	@Test
	public void factorUtil(){


		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m = RandomChainNonMarkovian.buildModel(5, 2, 6);
		m.fillWithRandomFactors(10);
		HashMap map = m.getEmpiricalMap();

		double[] actual, expected;

		actual = ((BayesianFactor) FactorUtil
				.fixEmpiricalMap(map, 2)
				.values().toArray()[2]).getData();
		expected = new double[]{0.0, 0.02, 0.54, 0.24, 0.07, 0.37, 0.39, 0.37 };
		Assert.assertArrayEquals(expected, actual, 0.000001);

		actual = FactorUtil.fixPrecission(
				(BayesianFactor) map.values().toArray()[2],
				2, false, 2,3).getData();
		Assert.assertArrayEquals(expected, actual, 0.000001);


		Assert.assertEquals(16, FactorUtil.EmpiricalMapSize(map), 0.0);


		FactorUtil.fixPrecission((BayesianFactor) map.values().toArray()[2], 2, false, 2,3);

		// print the factor
		BayesianFactor f = (BayesianFactor) map.values().toArray()[2];
		FactorUtil.print(f);

		RandomUtil.setRandomSeed(1);
		BayesianFactor f1 = BayesianFactor.random(m.getDomain(2), m.getDomain(1,3), 2, true);
		BayesianFactor f2 = BayesianFactor.random(m.getDomain(2), m.getDomain(1,3), 2, true);
		BayesianFactor f3 = BayesianFactor.random(m.getDomain(2), m.getDomain(1,3), 2, true);

		actual = new VertexToInterval().apply(
				FactorUtil.mergeFactors(List.of(f1,f2, f3), 2, true),
				2).getUpper(0);


		expected = new double[]{0.92, 0.85 };

		Assert.assertArrayEquals(expected,actual, 0.0001);
	}

	@Test
	public void factorUtilFilterTest(){
		//todo: uncomment when conservative is fixed

		/*
		StructuralCausalModel m = new StructuralCausalModel();

		int a = m.addVariable(2, false);
		int x = m.addVariable(2, false);
		int y = m.addVariable(2, false);
		int z = m.addVariable(2, false);

		int w = m.addVariable(2, true);
		int v = m.addVariable(4, true);
		int u = m.addVariable(16, true);

		m.addParent(x,a);
		m.addParent(y,x);
		m.addParent(z,y);

		m.addParent(a,w);
		m.addParent(x,v);
		m.addParent(y,u);
		m.addParent(z,u);


		SparseDirectedAcyclicGraph endoDag = DAGUtil.getSubDAG(m.getNetwork(), m.getEndogenousVars());
		m = CausalBuilder.of(endoDag, 2).setCausalDAG(m.getNetwork()).build();
		m.fillExogenousWithRandomFactors(3);



		TIntIntMap[] dataset = m.samples(100, m.getEndogenousVars());
		HashMap empProbs = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(m, dataset),5);

		SparseModel vmodel = m.toVCredal(empProbs.values());


		double[] actual;
		double[] expected;


		actual = Doubles.concat(FactorUtil.inverseFilter((VertexFactor) vmodel.getFactor(v), v, 0).getData()[0]);
		expected = new double[]{ 0.35897, 0.45455, 0.18648, 0.0, 0.09558, 0.54545};
		Assert.assertArrayEquals(expected,actual, 0.0);

		actual = Doubles.concat(FactorUtil.inverseFilter((VertexFactor) vmodel.getFactor(v), v, 1).getData()[0]);
		expected = new double[]{0.0, 0.45455, 0.18648, 0.35897, 0.09558, 0.54545};
		Assert.assertArrayEquals(expected,actual, 0.0);

		actual = Doubles.concat(FactorUtil.inverseFilter((VertexFactor) vmodel.getFactor(v), v, 3).getData()[0]);
		expected = new double[]{0.0, 0.35897, 0.45455, 0.35897, 0.0, 0.09558};
		Assert.assertArrayEquals(expected,actual, 0.0);


		FactorUtil.inverseFilter((VertexFactor) vmodel.getFactor(y), u, 0).sampleVertex();
		actual = FactorUtil.inverseFilter((VertexFactor) vmodel.getFactor(y), u, 0).sampleVertex().filter(y,0).getData();
		expected = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		Assert.assertArrayEquals(expected,actual, 0.0);


		actual = FactorUtil.inverseFilter((VertexFactor) vmodel.getFactor(y), u, 1).sampleVertex().filter(y,0).getData();
		expected = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		Assert.assertArrayEquals(expected,actual, 0.0);
		*/
	}

	@Test
	public void probabilityTest(){


		RandomUtil.setRandomSeed(1);
		StructuralCausalModel m1 = RandomChainNonMarkovian.buildModel(5, 2, 6);

		StructuralCausalModel m2 = m1.copy();
		m2.setFactor(7, new BayesianFactor(m2.getDomain(7), new double[]{.06, 0.06, 0.22, 0.02, 0.02, 0.61}));

		HashMap map1 = m1.getEmpiricalMap();
		HashMap map2 = m2.getEmpiricalMap();



		Assert.assertEquals(
				0.07478453770139666,
				Probability.likelihood(map1,map2, 1),
				0.000000000000001);


		Assert.assertEquals(
				0.07639062923848029,
				Probability.likelihood(map2,map1, 1),
				0.000000000000001);


		Assert.assertEquals(
				-2.5931441305922123,
				Probability.logLikelihood(map1,map2, 1),
				0.000000000000001);


		Assert.assertEquals(
				-2.571895244278866,
				Probability.logLikelihood(map2,map1, 1),
				0.000000000000001);

		Assert.assertEquals(
				-25.71895244278866,
				Probability.logLikelihood(map2,map1, 10),
				0.000000000000001);

		RandomUtil.setRandomSeed(1);
		BayesianFactor f1 = BayesianFactor.random(m1.getDomain(2), m1.getDomain(1,3), 2, false);
		BayesianFactor f2 = BayesianFactor.random(m1.getDomain(2), m1.getDomain(1,3), 2, false);

		Assert.assertEquals(
				0.024402559772054404,
				Probability.likelihood(f1,f2, 1),
				0.000000000000001);


		Assert.assertEquals(
				0.06383020659791545,
				Probability.likelihood(f2,f1, 1),
				0.000000000000001);


		Assert.assertEquals(
				-3.713067243494758,
				Probability.logLikelihood(f1,f2, 1),
				0.000000000000001);


		Assert.assertEquals(
				-2.7515287430295228,
				Probability.logLikelihood(f2,f1, 1),
				0.000000000000001);


	}



}
