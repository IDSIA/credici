package ch.idsia.credici.inference.approxlp;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.search.impl.GreedyWithRandomRestart;


/**
 * Perform inference using ApproxLP
 */
public class ApproxLP1  {

	public static double EPS = 1e-9;

	private Map<String, Object> init = null;

	protected int evidenceNode = -1;

	public ApproxLP1() {
	}


	public ApproxLP1(int evidenceNode) {
		setEvidenceNode(evidenceNode);
	}

	public ApproxLP1(Map<String, ?> params) {
		initialize(params);
	}


	public ApproxLP1(Map<String, ?> params, int evidenceNode) {
		initialize(params);
		setEvidenceNode(evidenceNode);
	}

	/**
	 * @param params configuration map for the inference engine
	 */
	public void initialize(Map<String, ?> params) {
		if (params == null)
			this.init = new HashMap<>();
		else
			this.init = new HashMap<>(params);
	}

	/**
	 * @param evidenceNode the node found with a {@link ch.idsia.crema.preprocess.BinarizeEvidence} pre-processing
	 */
	public void setEvidenceNode(int evidenceNode) {
		this.evidenceNode = evidenceNode;
	}

	
	/**
	 * Preconditions:
	 * <ul>
	 * <li>single evidence node
	 * <li>Factors must be one of
	 * <ul>
	 * 		<li>ExtensiveLinearFactors,
	 * 		<li>BayesianFactor or
	 * 		<li>SeparateLinearFactor
	 * </ul>
	 * </ul>
	 * <p>
	 *     Use the method {@link #setEvidenceNode(int)} to set the variable that is to be considered the summarization
	 *     of the evidence (-1 if no evidence).
	 * <p>
	 *
	 * @param model the inference model
	 * @param query the variable whose intervals we are interested in
	 * @return
	 */
	public IntervalFactor query(GraphicalModel model, int query, int evidence) throws InterruptedException {
		int states = model.getSize(query);

		double[] lowers = new double[states];
		double[] uppers = new double[states];

		for (int state = 0; state < states; ++state) {
			Manager lower;
			Manager upper;

			if (evidence == -1) {
				// without evidence we are looking for a marginal
				EPS = 0.0;
				lower = new Marginal(model, GoalType.MINIMIZE, query, state);
				upper = new Marginal(model, GoalType.MAXIMIZE, query, state);
			} else {
				EPS = 0.000000001;
				lower = new Posterior(model, GoalType.MINIMIZE, query, state, evidence);
				upper = new Posterior(model, GoalType.MAXIMIZE, query, state, evidence);
			}

			lowers[state] = runSearcher(model, lower);
			uppers[state] = runSearcher(model, upper);

		}

		IntervalFactor result = new IntervalFactor(model.getDomain(query), model.getDomain(), new double[][] { lowers },
				new double[][] { uppers });
		result.updateReachability();

		return result;
	}

	private double runSearcher(GraphicalModel model, Manager objective) {
		try {
			Neighbourhood neighbourhood = new Neighbourhood(model);

			GreedyWithRandomRestart<Move, Solution> searcher = new GreedyWithRandomRestart<>();
			searcher.setNeighbourhoodFunction(neighbourhood);
			searcher.setObjectiveFunction(objective);

			HashMap<String, Object> opt = new HashMap<>();
			opt.put(GreedyWithRandomRestart.MAX_RESTARTS, "10");
			opt.put(GreedyWithRandomRestart.MAX_PLATEAU, "3");

			if (init != null)
				opt.putAll(init);
			searcher.initialize(neighbourhood.random(), opt);

			return searcher.run();
		} catch (InterruptedException e) {
			// TODO: maybe return NaN?
			throw new IllegalStateException(e);
		}
	}

}
