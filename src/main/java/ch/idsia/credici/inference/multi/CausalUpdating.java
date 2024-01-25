package ch.idsia.credici.inference.multi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.util.MathArrays;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.set.IntervalSet;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.CutObserved;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A causal updating implementation that can run many inferences at once.
 * 
 * @param <T>
 */
public class CausalUpdating<T extends GraphicalModel<BayesianFactor>> {

	protected List<T> models;
	protected int worldsSize;
	protected T queryModel;
	protected VariableMapping<T> mapping; 
	protected Supplier<T> modelCreator;
	
	public CausalUpdating(Supplier<T> modelCreator) {
		this.models = new ArrayList<T>();
		this.mapping = new DenseMapping<T>();
		this.modelCreator = modelCreator;
	}

	public void setModels(Collection<T> models) {
		this.models = new ArrayList<T>(models);
	}

	public void addModel(T model) {
		checkDag(model);
		models.add(model);
	}

	protected void checkDag(T model) {
		if (models.isEmpty())
			return;

		// first model as reference
		T reference = models.get(0);

		int[] ref_vars = reference.getVariables();
		int[] model_vars = model.getVariables();
		if (ref_vars.length != model_vars.length)
			throw new IllegalStateException("Different Number of vars");

		Arrays.sort(ref_vars);
		Arrays.sort(model_vars);
		if (!Arrays.equals(ref_vars, model_vars))
			throw new IllegalStateException("Different variables");

		for (int variable : model.getVariables()) {
			int ref_size = reference.getSize(variable);
			int model_size = model.getSize(variable);
			if (ref_size != model_size) throw new IllegalStateException("Variable " + variable + " differs in size (" + model_size + " vs " + ref_size + ")");
			
			int[] ref_parents = reference.getParents(variable);
			int[] model_parents = model.getParents(variable);
			if (ref_parents.length != model_parents.length) throw new IllegalStateException("Different parents");
			
			Arrays.sort(ref_parents);
			Arrays.sort(model_parents);
			if (!Arrays.equals(ref_parents, model_parents)) throw new IllegalStateException("Different Parenting");
		}
	}

	

	
//	public void buildInferenceModel(List<QueryPart> query) {
//		int num_worlds = query.size();
//		// get arbitrary reference model
//		T reference = models.get(0);
//		T fullModel = null;
//		
//		for (var subquery : query) {
//			@SuppressWarnings("unchecked")
//			T subworld = (T) reference.copy();
//			var cutter = new CutObserved();
////			subquery.observing;
////			cutter.executeInplace(subworld, )
//		}
//			
//	}
//	
	public IntervalSet run() {
		return null;
	}
}
