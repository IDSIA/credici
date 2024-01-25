package ch.idsia.credici.utility.sample;

import java.util.stream.IntStream;

import org.apache.commons.math3.random.UniformRandomGenerator;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.rng.simple.RandomSource;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class Sampler {
	private UniformRandomProvider source;
	public Sampler(long seed) {
		 source = RandomSource.JDK.create(seed);
	}
	
	public Sampler() {
		source = RandomSource.JDK.create();
	}
	
	public int sampleState(int variable, TIntIntMap conditioning, BayesianFactor f) {

		Strides s = f.getDomain();
		
		int[] parents = s.remove(variable).getVariables();
		int[] states = IntStream.of(parents).map(conditioning::get).toArray();

		// alternative way of getting the column
//		final int offset = s.getPartialOffset(parents, states);
//		int size = model.getSize(variable);
//		final int stride = s.getStride(variable);
//		double[] probs = IntStream.range(0, size).map(i->offset + i*stride).mapToDouble(f::getValueAt).toArray();
		
		double[] probs = f.filter(new TIntIntHashMap(parents, states)).getData();
		double value = source.nextDouble();
		
		for (int state = 0; state < probs.length; state ++) {
			value -= probs[state];
			if (value < 0) return state;
		}
		return probs.length - 1;
	}
	
	
	public DoubleTable sample(StructuralCausalModel model, int N, int... vars) {
		int[] order = DAGUtil.getTopologicalOrder(model.getNetwork());
		DoubleTable result = new DoubleTable(vars);
		for (int n = 0; n < N; ++n) {
			TIntIntMap running = new TIntIntHashMap();
			for (int variable : order) {
				BayesianFactor f = model.getFactor(variable);
				int state = sampleState(variable, running, f);
				running.put(variable, state);
			}
			result.add(running);
		}
		return result;
	}
}
