package ch.idsia.credici.utility;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class DataUtil {

	public static BayesianFactor getCounts(TIntIntMap[] data, Strides dom) {


		// sort the variables in the domain
		dom = dom.sort();

		BayesianFactor counts = new BayesianFactor(dom);
		int[] vars = dom.getVariables();

		for (int i = 0; i < dom.getCombinations(); i++) {

			int[] states = dom.statesOf(i);

			TIntIntMap assignament = new TIntIntHashMap();
			for (int j = 0; j < vars.length; j++)
				assignament.put(vars[j], states[j]);

			counts.setValueAt(Stream.of(data)
					.filter(d -> IntStream.of(vars).allMatch(v -> d.get(v) == assignament.get(v)))
					.count(), i);
		}
		return counts;
	}


	public static BayesianFactor getJointProb(TIntIntMap[] data, Strides dom) {
    	return getCounts(data,dom).scalarMultiply(1.0/data.length);
	}


	public static BayesianFactor getCondProb(TIntIntMap[] data, Strides left, Strides right){
		if(ArraysUtil.intersection(left.getVariables(), right.getVariables()).length > 0)
				throw new IllegalArgumentException("Overlapping domains");
		BayesianFactor joint = getCounts(data, left.concat(right));
		BayesianFactor jointRight = getCounts(data, right);
		return joint.divide(jointRight);
	}



	public static HashMap<Set<Integer>, BayesianFactor> getEmpiricalMap(StructuralCausalModel model, TIntIntMap[] data ){

		HashMap<Set<Integer>, BayesianFactor> empirical = new HashMap<>();

		for(int u: model.getExogenousVars()){
			int[] right = model.getEndogenousChildren(u);
			int[] left = model.getEndegenousParents(right);
			BayesianFactor p = null;
			if(left.length>0)
				p = DataUtil.getCondProb(data,model.getDomain(right),model.getDomain(left));
			else
				p = DataUtil.getJointProb(data,model.getDomain(right));
			empirical.put(Arrays.stream(right).boxed().collect(Collectors.toSet()), p);
		}

	 	return empirical;
	}

}
