package ch.idsia.credici.model;

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import org.junit.Assert;
import org.junit.Test;

public class EmpiricalMapTest {


	@Test
	public void joinTest() throws InterruptedException, ExecutionControl.NotImplementedException {

		// Build markovian model
		String endoArcs = "(0,1),(1,2)";
		SparseDirectedAcyclicGraph endoDAG = DAGUtil.build(endoArcs);

		String exoArcs = "(3,0),(4,1),(5,2)";
		SparseDirectedAcyclicGraph causalDAG = DAGUtil.build(endoArcs + exoArcs);
		StructuralCausalModel model = CausalBuilder.of(endoDAG, 2).setCausalDAG(causalDAG).build();


		// Select cofounders
		int[][] pairsX = new int[][]{new int[]{0,2}};
		model = Cofounding.mergeExoParents(model, pairsX);

		RandomUtil.setRandomSeed(0);
		model.fillExogenousWithRandomFactors(2);

		//  /       \
		// Z -> Y -> X
		//      |
		int z=0, y=1, x=2;


		VariableElimination inf = new FactorVariableElimination(model.getVariables());
		inf.setFactors(model.getFactors());

		double[] actual = null, expected = null;

		// True join
		BayesianFactor pjoin = (BayesianFactor) inf.conditionalQuery(new int[]{z,y,x});
		actual = pjoin.getData();
		expected = new double[]{ 0.12960000000000002, 0.21460000000000004, 0.016800000000000002, 0.13000000000000003, 0.0432, 0.3478, 0.0504, 0.06760000000000001 };
		Assert.assertArrayEquals(expected,actual,0.000001);


		// Factorisation
		BayesianFactor pz = (BayesianFactor) inf.run(z);
		BayesianFactor pxy_z = BayesianFactor.combineAll((BayesianFactor) inf.conditionalQuery(x, y, z), (BayesianFactor) inf.conditionalQuery(y,z));
		actual = pz.combine(pxy_z).getData();
		expected = new double[]{0.1296, 0.21459999999999999, 0.0168, 0.12999999999999998, 0.043199999999999995, 0.3477999999999999, 0.050399999999999986, 0.06759999999999998};
		Assert.assertArrayEquals(expected,actual,0.000001);

		// Empirical map from model
		actual = BayesianFactor.combineAll(model.getEmpiricalMap().values()).getData();
		expected = new double[]{ 0.1296, 0.21459999999999999, 0.016800000000000002, 0.13, 0.043199999999999995, 0.3478, 0.0504, 0.06760000000000001};
		Assert.assertArrayEquals(expected,actual,0.000001);

		// empirical from data
		RandomUtil.setRandomSeed(0);
		TIntIntMap[] data = model.samples(1000, model.getEndogenousVars());
		actual = BayesianFactor.combineAll(DataUtil.getEmpiricalMap(model, data).values()).getData();
		expected = new double[] { 0.113, 0.21800000000000003, 0.011, 0.137, 0.052000000000000005, 0.36500000000000005, 0.049999999999999996, 0.054000000000000006};
		Assert.assertArrayEquals(expected,actual,0.000001);

	}
}
