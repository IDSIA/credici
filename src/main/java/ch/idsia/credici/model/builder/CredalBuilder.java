package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactorUtilities;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.graphical.DAGModel;
import ch.idsia.crema.model.graphical.GraphicalModel;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.hull.ConvexHull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class CredalBuilder {

	// input SCM
	protected  StructuralCausalModel causalmodel;

	// output credal model
	protected DAGModel model;

	// abstract methods to implemented
	abstract public CredalBuilder build() throws InterruptedException;

	// method for getting the generated model.
	public DAGModel getModel() {
		return model;
	}

	public List<Integer> getUnfeasibleNodes(){
		List<Integer> out = new ArrayList<>();
		for(int u : causalmodel.getExogenousVars()){
			if(model.getFactor(u) == null)
				out.add(u);
		}
		return out;
	}

	protected void assertTrueMarginals(){
		for(int u: causalmodel.getExogenousVars()){
			if(causalmodel.getFactor(u) == null)
				throw new IllegalArgumentException("Empirical factors should be provided if true marginals are not in the SCM");
		}
	}


	protected void assertMarkovianity(){
		if(!CausalInfo.of(causalmodel).isMarkovian() && !CausalInfo.of(causalmodel).isQuasiMarkovian()){
			throw new IllegalArgumentException("Wrong markovianity");
		}
	}


	/**
	 * Build a credal network from a set of precise models
	 *
	 * @param models
	 * @return
	 */
	public static GraphicalModel<VertexFactor> fromPreciseModels(boolean convexHull, DAGModel<BayesianFactor>... models) {
		for (int i = 1; i < models.length; i++) {
			if (!ArraysUtil.equals(models[0].getVariables(), models[i].getVariables(), true, true))
				throw new IllegalArgumentException("Inconsistent domains");
		}

		GraphicalModel<VertexFactor> vmodel = new DAGModel<>();

		for (int v : models[0].getVariables())
			vmodel.addVariable(v);
		for (int v : vmodel.getVariables()) {
			vmodel.addParents(v, models[0].getParents(v));

			VertexFactor f = VertexFactorUtilities.mergeVertices(Stream.of(models)
					.map(m -> new BayesianToVertex().apply(m.getFactor(v), v))
					.toArray(VertexFactor[]::new));
			if (convexHull)
				f = f.convexHull(ConvexHull.DEFAULT);

			vmodel.setFactor(v, f);
		}
		return vmodel;
	}
}

