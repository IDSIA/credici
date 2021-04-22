package ch.idsia.credici.utility;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;

import java.util.List;
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

}
