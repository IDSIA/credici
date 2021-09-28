package ch.idsia.credici.factor;

import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.separate.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import org.apache.commons.lang3.NotImplementedException;

public class FactorBuilder {

	/**
	 * Static method that builds a deterministic factor (values can only be ones or zeros)
	 * without parent variables.
	 * @return
	 */
	public static GenericFactor getDeterministic(GenericFactor f, int var, int state) {

		Strides dom = Strides.as(var, f.getDomain().getCardinality(var));
		if(f instanceof BayesianFactor){
			return BayesianFactorBuilder.deterministic(dom, state);
		}else if(f instanceof SeparateHalfspaceFactor){
			return HalfSpaceFactorBuilder.deterministic(dom, state);
		}else if(f instanceof VertexFactor){
			return VertexFactorBuilder.deterministic(dom, state);
		}
		throw new IllegalArgumentException("Wrong input factor type");
	}
}
