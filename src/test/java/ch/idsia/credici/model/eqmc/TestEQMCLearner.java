package ch.idsia.credici.model.eqmc;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.MathArrays;

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.learning.eqem.EQEMLearner;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.dot.DetailedDotSerializer;
import ch.idsia.credici.model.io.dot.Info;
import ch.idsia.credici.model.transform.Canonical;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.model.transform.PNS;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class TestEQMCLearner {

	public static void main(String[] args) throws InterruptedException {
		StructuralCausalModel model = new StructuralCausalModel("Anto");
		int v1 = model.addVariable(0, 2);
		int v2 = model.addVariable(1, 2);
		int u1 = model.addVariable(2, 2, true);
		int u2 = model.addVariable(3, 2, true);

		model.addParent(v1, u1);
		model.addParent(v2, u2);
		model.addParent(v2, v1);

		var fv1 = new BayesianFactor(model.getFullDomain(v1), true);
		fv1.setData(new double[] { 0.61379873, 0.38620127, 0.35023106, 0.64976894 });
		model.setFactor(v1, fv1);

		var fv2 = new BayesianFactor(model.getFullDomain(v2), true);
		fv2.setData(new double[] { 0.42408526, 0.57591474, 0.37956283, 0.62043717, 0.18885828, 0.81114172, 0.03924539,
				0.96075461 });
		model.setFactor(v2, fv2);

		var fu1 = new BayesianFactor(model.getFullDomain(u1), true);
		fu1.setData(new double[] { 0.36666223, 0.63333777 });
		model.setFactor(u1, fu1);

		var fu2 = new BayesianFactor(model.getFullDomain(u2), true);
		fu2.setData(new double[] { 3.78164892e-04, 9.99621835e-01 });
		model.setFactor(u2, fu2);

		DoubleTable datatable = new DoubleTable(new int[] { v1, v2 });
		datatable.add(new int[] { 0, 0 }, 20.0);
		datatable.add(new int[] { 1, 0 }, 30.0);
		datatable.add(new int[] { 0, 1 }, 40.0);
		datatable.add(new int[] { 1, 1 }, 50.0);

		EmpiricalNetwork en = new EmpiricalNetwork();
		BayesianNetwork network = en.apply(model, datatable);

		double ll = en.loglikelihood(network, datatable);
		System.out.println("LL: " + ll);

		StructuralCausalModel camo = Canonical.LOG.apply(model);
		CredalCausalVE ccve = new CredalCausalVE(camo, datatable.toMap(false), u1, u2);
		System.out.print("CCVE: ");
		var pns = ccve.probNecessityAndSufficiency(v1, v2);
		for (double[] v : pns.getData()[0]) {
			System.out.print(v[0] + ",");
		}

		TIntIntMap sizes = new TIntIntHashMap();
		sizes.put(u1, camo.getSize(u1));
		sizes.put(u2, camo.getSize(u2));

		Config settings = new Config().maxRun(10000).numIterations(100000).numPSCMRuns(0).deterministic(false);
		double[] mm = pns(model, settings, v1, v2, datatable, sizes);
		System.out.println("PNS dete: " + Arrays.toString(minmax(mm)));

		settings = new Config().maxRun(10000).numIterations(100000).numPSCMRuns(10).deterministic(true);
		mm = pns(model, settings, v1, v2, datatable, sizes);
		System.out.println("PNS dete: " + Arrays.toString(minmax(mm)));
	}

	static double[] minmax(double[] data) {
		double min = data[0];
		double max = data[0];

		for (int v = 1; v < data.length; ++v) {
			double val = data[v];
			if (val < min)
				min = val;
			else if (val > max)
				max = val;
		}
		return new double[] { min, max };
	}

	private static double[] pns(StructuralCausalModel model, Config c, int v1, int v2, DoubleTable datatable,
			TIntIntMap sizes) throws InterruptedException {
		EQEMLearner learner = new EQEMLearner(model, datatable, sizes, true, c);

		// learner.setDebugLoggerGenerator(new PDFLoggerGenerator("./run"));
		var cc = learner.run();
		long before = cc.combinations().longValue();
		cc.simplify();
		long after = cc.combinations().longValue();
		System.out.println(before + " -> " + after);

		var solutions = (after < 40000) ? cc.exaustiveIterator() : cc.sobolIterator();

		TDoubleList pnss = new TDoubleArrayList(100);
		StructuralCausalModel solution = null;
		PNS pns_algo = new PNS();
		
		for (int i = 0; i < 175000 && solutions.hasNext(); ++i) {
			solution = Experiments.log2standard(solutions.next());
			double pnsval = pns_algo.execute(solution, v1, v2);
			
//			CausalVE cve = new CausalVE(solution);
//			var pns = cve.probNecessityAndSufficiency(v1, v2);
//			double pnsval = pns.getData()[0];
			pnss.add(pnsval);
		}
		// DetailedDotSerializer.saveModel("./run/solution.png", new
		// Info().model(solution).data(datatable));
		return pnss.toArray();
	}

}