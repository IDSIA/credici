package ch.idsia.credici.utility;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import com.google.common.primitives.Ints;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FactorUtil {

	public static VertexFactor mergeFactors(List<BayesianFactor> factors, int leftvar, boolean applyConvexhull){
		VertexFactor vf = VertexFactor.mergeVertices(
				factors.stream().map(f -> new BayesianToVertex().apply(f, leftvar)).toArray(VertexFactor[]::new)
		);
		if(applyConvexhull)
			vf = vf.convexHull(true);

		return vf;
	}

	public static HashMap<Set<Integer>, BayesianFactor> fixEmpiricalMap(
			HashMap<Set<Integer>,
			BayesianFactor> emp, int numDecimals){

		HashMap out = new HashMap();

		for(Set<Integer> s : emp.keySet()){
			out.put(s, ((BayesianFactor)emp.get(s)).fixPrecission(numDecimals, Ints.toArray(s)));
		}
		return out;

	}

}
