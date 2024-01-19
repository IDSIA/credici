package ch.idsia.credici.learning.eqem;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.dot.DetailedDotSerializer;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class EQEMLearner {
	private int numRuns = 10;
	private StructuralCausalModel model;
	private Randomizer random;
	private DoubleTable data;

	public EQEMLearner(StructuralCausalModel prior, DoubleTable data, TIntIntMap exosizes) {
		this.random = new Randomizer(0);
		this.model = initModel(prior, exosizes);
		this.data = data;
	}

	public void run() throws InterruptedException {
		CComponents cc = new CComponents();

		List<Pair<StructuralCausalModel, DoubleTable>> components = cc.apply(model, data);
		for (int r = 0 ; r < numRuns; ++r) {
			for (var pair : components) {
				
				StructuralCausalModel component_model = pair.getLeft();
				DoubleTable component_data = pair.getRight();
				
				ComponentEM cem = new ComponentEM(pair.getKey(), pair.getValue(), 0);
				
				EmpiricalNetwork en = new EmpiricalNetwork();
				BayesianNetwork network = en.apply(component_model, component_data);
				DetailedDotSerializer.saveModel(network, component_data, "emp.png");		

				double ll2 = en.loglikelihood(network, component_data);
				
				cem.run(cc::addResult);
			}
		}
	}

	private StructuralCausalModel initModel(StructuralCausalModel prior, TIntIntMap exosizes) {
		StructuralCausalModel model = new StructuralCausalModel(prior.getName());

		for (int variable : prior.getVariables()) {
			int size = prior.getSize(variable);
			if (exosizes.containsKey(variable)) {
				size = exosizes.get(variable);
			}

			boolean exo = prior.isExogenous(variable);
			model.addVariable(variable, size, exo);
		}

		for (int variable : prior.getVariables()) {
			int[] parents = prior.getParents(variable);
			model.addParents(variable, parents);
		}

		// create random factors
		for (int variable : prior.getVariables()) {
			// int[] domain = prior.getFactor(variable).getDomain().getVariables();

			int[] domain = ArraysUtil.append(new int[] { variable }, model.getParents(variable));
			Arrays.sort(domain);

			Strides dom = model.getDomain(domain);
			BayesianFactor factor = new BayesianFactor(dom);
			random.randomizeInplace(factor, variable);
			model.setFactor(variable, factor);
		}

		return model;
	}

	public static void main(String[] args) {

		int N = 5000;
		int numRun = 1000; // EM internal iterations
		int maxIter = 1000;

		int n = 4;
		int endoVarSize = 2;
		int exoVarSize = 5;

		RandomUtil.setRandomSeed(1000);
		StructuralCausalModel causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);

		TIntIntMap[] data = IntStream.range(0, N).mapToObj(i -> causalModel.sample(causalModel.getEndogenousVars()))
				.toArray(TIntIntMap[]::new);

		
		int[] exo = causalModel.getExogenousVars();
		Strides dom = causalModel.getDomain(exo);
		int[] sz = dom.getSizes();

		DoubleTable dataTable = new DoubleTable(data);
		DetailedDotSerializer.saveModel(causalModel, dataTable, "input.png");

		EQEMLearner learner = new EQEMLearner(causalModel, dataTable, new TIntIntHashMap(exo, sz));
		try {
			learner.run();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
