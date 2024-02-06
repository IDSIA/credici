package ch.idsia.credici.model.eqmc;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.learning.eqem.EQEMLearner;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder.SelectionPolicy;
import ch.idsia.credici.model.predefined.RandomChainMarkovian;
import ch.idsia.credici.utility.sample.Sampler;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.javasoft.util.IterableIterable;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class Experiments {
	
	public static void main(String[] args) throws InterruptedException {
		markovian_chain(args);
	}
	
	
	public static void markovian_chain(String[] args) throws InterruptedException {

		StructuralCausalModel model = RandomChainMarkovian.buildModel(5, 2, -1);

		int[] roots = Arrays.stream(model.getEndogenousVars()).filter(a -> model.getEndogenousParents(a).length == 0).toArray();
		int cause = roots[0];
		int[] leaves = model.getLeaves();
		int effect = leaves[0];
		
		Sampler sample = new Sampler();
		var data = sample.sample(model, 5000, model.getEndogenousVars());
		
		var ccve = runccve(model, data, cause, effect);
		
		var emcc = runemcc(model, data, cause, effect, 1000);
		
		Config config = new Config().deterministic(false).numRun(400).numIterations(10000).numPSCMRuns(0);
		var relax = runrelax(model, data, cause, effect, config, 1, 100000);
			
		config = new Config().deterministic(true).numRun(100).numIterations(10000).numPSCMRuns(10).numPSCMInterations(1000);
		var dete = runrelax(model, data, cause, effect, config, 1, 100000);
		System.out.println(array2string(ccve) +","+array2string(emcc));
	}
	
	
	private static double[] runrelax(StructuralCausalModel model, DoubleTable data, int cause, int effect, Config config, int delta_size, int networks) throws InterruptedException {
		
		TIntIntMap sizes = new TIntIntHashMap();
		for (int exo : model.getExogenousVars()) {
			int size = model.getSize(exo);
			int child = model.getEndogenousChildren(exo)[0];
			
			int[] parents = model.getEndegenousParents(child);
			int comb = model.getDomain(parents).getCombinations();
			int canonical = (int) Math.max(2, (Math.pow(model.getSize(child), comb) - delta_size));
			System.out.println("size" + canonical +  " " + size);
			sizes.put(exo, canonical);
		}
	
		EQEMLearner relax = new EQEMLearner(model, data, sizes, true);
		
		relax.setConfig(config);
		var cc = relax.run();
		var iter = cc.sobolIterator();
		double[] range = new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
		for (int i = 0; i < networks && iter.hasNext(); ++i) {
			StructuralCausalModel m = log2standard(iter.next());
			CausalVE cve= new CausalVE(m);
			var bf = cve.probNecessityAndSufficiency(cause, effect);
			double v = bf.getData()[0];
			range = minmax(range, v);
		}
		
		return range;
	}
	
	private static double[] runemcc(StructuralCausalModel model, DoubleTable data, int cause, int effect, int runs) throws InterruptedException {

		var emcc = EMCredalBuilder.of(model, data.toMap(false)).setMaxEMIter(runs).setWeightedEM(true).setSelPolicy(SelectionPolicy.LAST).build();
		CausalMultiVE cvme = new CausalMultiVE(emcc.getSelectedPoints());
		
		var factor = (VertexFactor) cvme.probNecessityAndSufficiency(cause, effect);
		return getPNS(factor);
	}


	static double[] runccve(StructuralCausalModel model, DoubleTable data, int cause, int effect) throws InterruptedException {
		CredalCausalVE ccve = new CredalCausalVE(model);
		var x = ccve.probNecessityAndSufficiency(cause, effect);
		return getPNS(x);
	}
	
	static StructuralCausalModel log2standard(StructuralCausalModel input) {
		StructuralCausalModel m = input.copy();
		for (var variable : m.getVariables()) {
			var factor = m.getFactor(variable);
			if (factor.isLog()) {
				double[] data = factor.getData();
				var f2 = new BayesianFactor(factor.getDomain(), data, false);
				m.setFactor(variable, f2);
			}
		}
		return m;
	}
	
	static String array2string(double[] interval) {
		return Arrays.stream(interval).mapToObj(Double::toString).collect(Collectors.joining(","));
	}
	
	static double[] minmax(double[] range, double value) {
		range[0] = Math.min(range[0], value);
		range[1] = Math.max(range[1], value);
		return range;
	}
	
	static double[] getPNS(VertexFactor v) {

		double[][] vertices = v.getVertices();
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (double[] val:vertices) {
			min = Math.min(min, val[0]);
			max = Math.max(max, val[0]);
		}
		return new double[] {min, max};
	}
}
