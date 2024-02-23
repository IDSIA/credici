package ch.idsia.credici.learning.eqem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Supplier;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.StructuralCausalModel.VarType;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class EQEMLearner {
	
	private Config settings;

	private Function<Integer, Consumer<Supplier<Info>>> loggerGenerator = EQEMLearner::noCallbackGenerator;

	private StructuralCausalModel model;
	private StructuralCausalModel prior;
	private Randomizer random;
	private DoubleTable data;
	private TIntSet freeVariables;

	public EQEMLearner(StructuralCausalModel prior, DoubleTable data, TIntIntMap exosizes, boolean log, Config settings) {
		this(prior, data, exosizes, new TIntHashSet(prior.getVariables()), log, settings);
	}

	public EQEMLearner(StructuralCausalModel prior, DoubleTable data, TIntIntMap exosizes, TIntSet free, boolean log, Config settings) {
		this.settings = settings != null ? settings : new Config();
		this.random = new Randomizer();
		this.prior = prior;
		this.freeVariables = new TIntHashSet(free);
		this.model = initModel(prior, exosizes, log);
		this.data = data;
	}


	public CComponents run() throws InterruptedException {
		return run(null);
	}

	/**
	 * Run the optimization and return the {@link CComponents} object that can then be used to 
	 * generate FSCM Models.
	 * 
	 * @param callback a function called for every model generation attempt
	 * @return
	 * @throws InterruptedException
	 */
	public CComponents run(final Consumer<ModelInfo> callback) throws InterruptedException {
		
		var	modelLogger = loggerGenerator.apply(null);
		modelLogger.accept(()->new Info().model(this.prior).title("Prior model"));
		modelLogger.accept(()->new Info().model(this.model).title("Starting model"));
		
		CComponents cc = new CComponents();

		List<Pair<StructuralCausalModel, DoubleTable>> components = cc.apply(model, data);
		List<Pair<ComponentEM, Double>> em = new ArrayList<>();
		
		for (var pair : components) {
			StructuralCausalModel component_model = pair.getLeft();
			DoubleTable component_data = pair.getRight();
		
			EmpiricalNetwork en = new EmpiricalNetwork();
			BayesianNetwork network = en.apply(component_model, component_data);
			double ll2 = en.loglikelihood(network, component_data);
		
			System.out.println("LL*: " + ll2);
			
			ComponentEM cem = new ComponentEM(pair.getKey(), pair.getValue(), ll2, settings);
			cem.setModelLogger(loggerGenerator.apply(cem.getId()));

			em.add(Pair.of(cem, ll2));
		}
		
		boolean didsomething = true; 
		for (int r = 0 ; r < settings.maxRuns() && didsomething; ++r) {
			
			didsomething = false; // we are hopefull that we will not do anuthing
			for (var pair : em) {
				ComponentEM cem = pair.getLeft();
				final Integer id = cem.getId();
		
				int num_models = cc.getResults(id).size(); 
				if (num_models >=  settings.numRuns()) continue;
				
				didsomething = true; // actually doing somehting
				
				final double llmax = pair.getRight();
				final double EPS = settings.llEPS();
				//				
				cem.run(r, (sol) -> {
					sol.componentId(id);
					if (sol.stage.success()) {
						double delta = llmax - sol.loglikelihood;
						if (delta <= EPS) {
							sol.accept();
							cc.addResult(sol.getModel());
							if (delta < -EPS) System.out.println("NBONONONO");
						}  else { 
							sol.reject();
						}
					}
					
					if (callback != null) {
						sol.llmax(llmax);

						callback.accept(sol);
					}
				});
				

			}
		}

		return cc;

	}

	/**
	 * Initialize the model for a new run
	 * 
	 * @param prior
	 * @param exosizes
	 * @return
	 */
	private StructuralCausalModel initModel(StructuralCausalModel prior, TIntIntMap exosizes, boolean log) {

		// ignore exosizes if not freeing endogenous
		if (!settings.freeEndogenous()) {
			exosizes = new TIntIntHashMap();
			freeVariables.removeAll(prior.getEndogenousVars());
		}
		
		
		StructuralCausalModel model = new StructuralCausalModel(prior.getName());
		model.copyData(prior);
		for (int variable : prior.getVariables()) {
			int size = prior.getSize(variable);
			if (exosizes != null && exosizes.containsKey(variable)) {
				size = exosizes.get(variable);
			}

			VarType type = prior.getVariableType(variable);
			model.addVariable(variable, size, type);
		}

		for (int variable : prior.getVariables()) {
			int[] parents = prior.getParents(variable);
			model.addParents(variable, parents);
		}

		// create random factors for all variables

		// default randomize all variables

		for (int variable : model.getVariables()) {
			if (freeVariables.contains(variable)) {
				Strides dom = model.getFullDomain(variable);
//				Arrays.sort(domain);
//
//				Strides dom = model.getDomain(domain);
//				if (prior.getFactor(variable) == null) {
				BayesianFactor factor = new BayesianFactor(dom, log);
				random.randomizeInplace(factor, variable);
				model.setFactor(variable, factor);
//				} else {
//					model.setFactor(variable, prior.getFactor(variable));
//				}

			} else {
				var f = prior.getFactor(variable);
				// make sure that if log is required we actually log
				if (log && !f.isLog()) {
					double[] dta = f.getInteralData().clone();
					for (int i = 0; i < dta.length; ++i) {
						dta[i] = Math.log(dta[i]);
					}
					f = new BayesianFactor(f.getDomain(), dta, true);
				} else {
					f = f.copy(); // make a copy
				}
				model.setFactor(variable, f);
			}

		}

		return model;
	}

	/**
	 * Set a callback function called at each step of the learner execution The
	 * function is passed into each component.
	 */
	public void setDebugLoggerGenerator(Function<Integer, Consumer<Supplier<Info>>> generator) {
		this.loggerGenerator = generator;
	}

	private static void noCallback(Supplier<Info> data) {
	}

	private static Consumer<Supplier<Info>> noCallbackGenerator(Integer name) {
		return EQEMLearner::noCallback;
	}

}
