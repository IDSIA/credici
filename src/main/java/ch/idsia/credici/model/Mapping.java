package ch.idsia.credici.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.lang3.tuple.Pair;

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.io.dot.DetailedDotSerializer;
import ch.idsia.credici.model.io.dot.Info;
import ch.idsia.credici.model.transform.Do;
import ch.idsia.credici.model.transform.PNS;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Domain;
import ch.idsia.crema.model.NoSuchVariableException;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.preprocess.CutObserved;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class Mapping extends StructuralCausalModel {
	private static int INITIAL_MULTIPLIER = 3;

	private int nextId = 0;

	// all reference exogenous variables
	private TIntSet exogenous;

	// from local to global BIDI map
	private Map<StructuralCausalModel, BidiMap<Integer, Integer>> mapping;

	StructuralCausalModel model = null;

	/**
	 * Initialize the mapping with the specified exogenous variables.
	 * 
	 * @param exogenous
	 */
	public Mapping(TIntSet exogenous) {
		this.exogenous = exogenous;
	}

	/**
	 * Initialize the mapping model based on the provided one. This will however not
	 * affect the possibility of other networks to have different sets of endogenous
	 * variables.
	 * 
	 * @param network the first network
	 * @return a mapping between the provided model's ids and joined model ones
	 */
	private BidiMap<Integer, Integer> init(StructuralCausalModel network) {
		mapping = new HashMap<StructuralCausalModel, BidiMap<Integer, Integer>>();
		model = network.copy();
		BidiMap<Integer, Integer> local = new TreeBidiMap<Integer, Integer>();
		nextId = Integer.MIN_VALUE;

		Arrays.stream(model.getVariables()).forEach((int variable) -> {
			local.put(variable, variable);
			nextId = Math.max(nextId, variable + 1);
		});

		mapping.put(network, local);
		return local;
	}

	/**
	 * check whether all elements needed from net are indeed there. I.e. the
	 * exogenous of the provided network must be part of the exogenous list provided
	 * at creation of the mapping.
	 * 
	 * @param net
	 * @return
	 */
	private boolean checkModel(StructuralCausalModel net) {
		for (int exo : net.getExogenousVars()) {
			if (!exogenous.contains(exo)) {
				return false;
			}
		}
		return true;
	}

	private static int[] mapTo(int[] from, BidiMap<Integer, Integer> mapping) {
		return Arrays.stream(from).map(v -> mapping.get(v)).toArray();
	}

	private static int[] mapFrom(int[] global, BidiMap<Integer, Integer> mapping) {
		return Arrays.stream(global).map(v -> mapping.getKey(v)).toArray();
	}

	public int[] mapToGlobal(StructuralCausalModel sourceModel, int[] ids) {
		return mapTo(ids, mapping.get(sourceModel));
	}

	public int[] mapFromGlobal(StructuralCausalModel sourceModel, int[] ids) {
		return mapFrom(ids, mapping.get(sourceModel));
	}

	public int mapToGlobal(StructuralCausalModel sourceModel, int id) {
		var ret = mapTo(new int[] { id }, mapping.get(sourceModel));
		if (ret.length == 0)
			throw new NoSuchVariableException(id, model.getDomain(sourceModel.getVariables()));
		return ret[0];
	}

	public int mapFromGlobal(StructuralCausalModel sourceModel, int id) {
		var ret = mapFrom(new int[] { id }, mapping.get(sourceModel));
		if (ret.length == 0)
			throw new NoSuchVariableException(id, model.getDomain(model.getVariables()));
		return ret[0];
	}

	public BidiMap<Integer, Integer> add(StructuralCausalModel network) {

		// could try to fix the network (espcially if first network), but for now just
		// raise a concern
		if (!checkModel(network))
			throw new IllegalArgumentException("Missing Exogenous Variables");

		// first model is the initializer.
		if (model == null) {
			return init(network);
		}

		BidiMap<Integer, Integer> local = new TreeBidiMap<Integer, Integer>();

		// map exogenous to identity
		exogenous.forEach(exo -> {
			local.put(exo, exo);
			return true;
		});

		int[] variables = network.getEndogenousVars(true); // vector is a copy
		Arrays.sort(variables);

		// create variables
		for (int variable : variables) {
			local.put(variable, nextId);

			int varSize = network.getSize(variable);
			model.addVariable(nextId, varSize, network.getVariableType(variable));

			++nextId;
		}

		// add parents
		for (int variable : variables) {
			int gid = local.get(variable);
			int[] parents = network.getParents(variable);
			parents = mapTo(parents, local);

			// add global parents to global variable
			model.addParents(gid, parents);
		}

		// copy factors (do not copy exogenous factors!)
		for (int variable : variables) {
			var factor = network.getFactor(variable);
			if (factor != null) {
				int gid = local.get(variable);

				Strides newDomain = model.getFullDomain(gid);
				BayesianFactor newFactor = new BayesianFactor(newDomain, factor.isLog());

				Strides domain = factor.getDomain();
				var source_iterator = domain.getIterator();

				var target_iterator = newDomain.getReorderedIterator(mapTo(domain.getVariables(), local));

				double[] source_data = factor.getInteralData();
				double[] target_data = newFactor.getInteralData();

				while (source_iterator.hasNext()) {
					double value = source_data[source_iterator.next()];
					target_data[target_iterator.next()] = value;
				}

				model.setFactor(gid, newFactor);
			}
		}

		mapping.put(network, local);
		return local;
	}

	/**
	 * Get the joined model
	 * 
	 * @return
	 */
	public StructuralCausalModel getModel() {
		return model;
	}

	public static void main(String[] args) throws InterruptedException {
		StructuralCausalModel one = new StructuralCausalModel();
		
		int A = one.addVariable(2);
		int B = one.addVariable(2);
		int C = one.addVariable(4);
		int U = one.addVariable(10, true);
		int U2 = one.addVariable(10, true);

		one.addParents(A, C, U, U2);
		one.addParent(B, U2);
		one.addParent(C, U);
		
		var fA = BayesianFactor.random(one.getDomain(A), one.getDomain(C,U,U2), 4, true);
		one.setFactor(A, fA);
		var fB = BayesianFactor.random(one.getDomain(B), one.getDomain(U2), 4, true);
		one.setFactor(B, fB);
		var fC = BayesianFactor.random(one.getDomain(C), one.getDomain(U), 4, true);
		one.setFactor(C, fC);

		var doing = new Do<BayesianFactor, StructuralCausalModel>();
		
		var doset = new TIntIntHashMap();
		doset.put(B, 1);
		var two = doing.execute(one, doset);
		var observe = new CutObserved();
		var observeset = new TIntIntHashMap();
		observeset.put(B, 0);
		var three = observe.execute(one, observeset);
		
		var mapping = new Mapping(one.getExogenousSet());
		var m1 = mapping.add(one);
		var m2 = mapping.add(two);
		var m3 = mapping.add(three);
		
		var m = mapping.getModel();
		
		CausalVE cve = new CausalVE(one);
		BayesianFactor x = cve.probNecessityAndSufficiency(C, A);
		
		
		DetailedDotSerializer.saveModel("out.png", new Info().model(m).hideTables());
	}
}
