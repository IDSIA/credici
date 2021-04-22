package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;

import java.util.List;
import java.util.stream.Collectors;

public class CausalMultiVE extends CausalInference<List<StructuralCausalModel>, GenericFactor>{

	private List<CausalVE> inf;

	boolean toInterval = true;

	public CausalMultiVE(List<StructuralCausalModel> model){
		this.model=model;
		inf = model.stream().map(m-> new CausalVE(m)).collect(Collectors.toList());

	}

	@Override
	public GenericFactor run(Query q) throws InterruptedException {
		// due to the conversion
		if(q.getTarget().length>1)
			throw new IllegalArgumentException("Only queries over a single variable are allowed.");

		// Run each query independently
		List<BayesianFactor> factors = inf.stream().map(i->i.run(q)).collect(Collectors.toList());

		// Merge the results
		VertexFactor vf = FactorUtil.mergeFactors(factors, q.getTarget()[0], !toInterval);

		if(toInterval)
			return new VertexToInterval().apply(vf, q.getTarget()[0]);
		return vf;

	}

	@Override
	public List<StructuralCausalModel> getInferenceModel(Query q) {
		return inf.stream().map(i->i.getInferenceModel(q)).collect(Collectors.toList());
	}

	public CausalMultiVE setToInterval(boolean toInterval) {
		this.toInterval = toInterval;
		return this;
	}
}
