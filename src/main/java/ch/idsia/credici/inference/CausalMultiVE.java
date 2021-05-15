package ch.idsia.credici.inference;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Strides;
import jdk.jshell.spi.ExecutionControl;

import java.util.List;
import java.util.stream.Collectors;

public class CausalMultiVE extends CausalInference<List<StructuralCausalModel>, GenericFactor>{

	private List<CausalVE> inf;

	boolean toInterval = false;

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
		VertexFactor vf = FactorUtil.mergeFactors(factors, inf.get(0).target[0], !toInterval);

		if(toInterval)
			return new VertexToInterval().apply(vf, q.getTarget()[0]);
		return vf;

	}

	@Override
	public List<StructuralCausalModel> getInferenceModel(Query q, boolean simplify) {
		return inf.stream().map(i->i.getInferenceModel(q, simplify)).collect(Collectors.toList());
	}

	public CausalMultiVE setToInterval(boolean toInterval) {
		this.toInterval = toInterval;
		return this;
	}

	public List<CausalVE> getInferenceList() {
		return inf;
	}


	@Override
	public GenericFactor probNecessityAndSufficiency(int cause, int effect, int trueState, int falseState) throws InterruptedException, ExecutionControl.NotImplementedException {

		double max = Double.NEGATIVE_INFINITY;
		double min = Double.POSITIVE_INFINITY;

		for (CausalVE i : this.getInferenceList()) {
			double psn = i.probNecessityAndSufficiency(cause, effect, trueState, falseState).getValue(0);
			if (psn > max) max = psn;
			if (psn < min) min = psn;
		}

		double[][][] vals = new double[1][2][1];
		vals[0][0][0] = min;
		vals[0][1][0] = max;

		return new VertexFactor(Strides.empty(), Strides.empty(), vals);
	}

}
