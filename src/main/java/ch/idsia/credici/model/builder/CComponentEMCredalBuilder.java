package ch.idsia.credici.model.builder;

import ch.idsia.credici.Table;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.BayesianCausalEM;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.learning.WeightedCausalEM;
import ch.idsia.credici.learning.inference.ComponentInference;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.model.transform.CComponents;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

public class CComponentEMCredalBuilder extends CredalBuilder{

	//todo getters and setters
	// todo: make private
	public TIntObjectMap<BayesianFactor> empiricalFactors;

	public List<StructuralCausalModel>  selectedPoints;

	public List<StructuralCausalModel> results;

	private boolean[] mask;

	public Map<Integer, BayesianFactor> endogJointProbs;

	public Map<Set<Integer>, BayesianFactor> inputGenDist;

	public Map<Set<Integer>, BayesianFactor> targetGenDist;

	public int maxEMIter = 200;

	public int numTrajectories = 10;

	public boolean discardNonConverging = false;

	public int splits = 10;

	private TIntIntMap[] data = null;

	private boolean verbose = false;

	private boolean buildCredalModel = false;

	private boolean weightedEM = true;

	private int[] trainableVars = null;

	private double threshold = 0.0;
	private FrequentistCausalEM.StopCriteria stopCriteria = FrequentistCausalEM.StopCriteria.KL;


	public enum SelectionPolicy {
		LAST,	// Selects the last point in the trajectory.
		BISECTION_BORDER_SAME_PATH, // Bisection with the 2 points in the same path closer to the border.
		BISECTION_BORDER,	// Bisection with 2 points from any point such that these are close to the border.
		BISECTION_ALL, // Bisection with all possible pair of points.

	}

	SelectionPolicy selPolicy = SelectionPolicy.LAST;

	public CComponentEMCredalBuilder(StructuralCausalModel causalModel){
		this.causalmodel = causalModel;
		this.endogJointProbs = causalModel.endogenousBlanketProb();
		this.trainableVars = causalModel.getExogenousVars();
	}

	public static EMCredalBuilder of(StructuralCausalModel causalModel){
		return new EMCredalBuilder(causalModel);
	}


	public CComponentEMCredalBuilder(StructuralCausalModel causalModel, TIntIntMap[] data){
		this.causalmodel = causalModel;
		this.data = data;
		this.trainableVars = causalModel.getExogenousVars();
	}



	public static CComponentEMCredalBuilder of(StructuralCausalModel causalModel, TIntIntMap[] data){
		return new CComponentEMCredalBuilder(causalModel, data);
	}


	public CComponentEMCredalBuilder build(int... exoVars) throws InterruptedException {
		if(exoVars.length>0)
			throw new NotImplementedException("Not implemented EMbuilder for a subsection of exogenous variables");

		Table data = new Table(this.data);
		CComponents components = new CComponents();
		components.apply(causalmodel, data);
		
		buildTrajectories();
		mergePoints();
		return this;
	}

	public CComponentEMCredalBuilder selectAndMerge(){
		mergePoints();
		return this;
	}

	public CComponentEMCredalBuilder buildTrajectories() throws InterruptedException {
		selectedPoints = new ArrayList<>();
		
		if (inferenceVariation == 5 && this.method != null){
			this.method.initialize(causalmodel);
		}

		for(int i = 0; i < numTrajectories; i++) {
			selectedPoints.add(runEM());
		}
		return this;
	}



	private void mergePoints(){
		if(buildCredalModel) {
			BayesianNetwork[] bnets =
				selectedPoints.stream().map(s -> s.toBnet()).toArray(BayesianNetwork[]::new);
			model = VertexFactor.buildModel(true, bnets);
		}
	}

	public boolean isInside(StructuralCausalModel m){
		return ratioLk(m) >= 1.0;
	}
	public boolean isOutside(StructuralCausalModel m){
		return !isInside(m);
	}

	public double ratioLk(StructuralCausalModel m){
		HashMap<Set<Integer>, BayesianFactor> map_i = m.getEmpiricalMap(false);
		return Probability.ratioLogLikelihood(map_i, targetGenDist, 1);
	}

	public double klPQ(StructuralCausalModel m, boolean zeroSafe){
		HashMap<Set<Integer>, BayesianFactor> map_i = m.getEmpiricalMap(false);
		return Probability.KL(targetGenDist, map_i, zeroSafe);
	}
	public double klQP(StructuralCausalModel m, boolean zeroSafe){
		HashMap<Set<Integer>, BayesianFactor> map_i = m.getEmpiricalMap(false);
		return Probability.KL(map_i, targetGenDist, zeroSafe);
	}
	public double klsym(StructuralCausalModel m, boolean zeroSafe){
		HashMap<Set<Integer>, BayesianFactor> map_i = m.getEmpiricalMap(false);
		return Probability.KLsymmetrized(map_i, targetGenDist, zeroSafe);
	}

	public List<StructuralCausalModel> getSelectedPoints() {
		return selectedPoints;
	}


	public CComponentEMCredalBuilder setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public CComponentEMCredalBuilder setBuildCredalModel(boolean buildCredalModel) {
		this.buildCredalModel = buildCredalModel;
		return this;
	}

	/**
	 * The result is an inner approximation if all the precise models composing the
	 * result are inside.
	 * @return
	 */
	public boolean isInnerApproximation(){
		return selectedPoints.stream().allMatch(this::isInside);
	}


	private StructuralCausalModel randomModel(StructuralCausalModel reference) {
		StructuralCausalModel rmodel = reference.copy();
		rmodel.fillExogenousWithRandomFactors();
		return rmodel;
	}



	private StructuralCausalModel runEM() throws InterruptedException {
		StructuralCausalModel startingModel = randomModel(causalmodel);
		
		FrequentistCausalEM em = null;
		Collection stepArgs = null;

		if(this.data==null) {
			throw new IllegalArgumentException("No data provided");
			//em = new BayesianCausalEM(startingModel).setKlthreshold(threshold).setRegularization(0.0);
			//stepArgs = (Collection) endogJointProbs.values();
		} else if(weightedEM) {
			em = new WeightedCausalEM(startingModel).setRegularization(0.0)
					.setStopCriteria(stopCriteria)
					.setThreshold(threshold)
					.setInferenceVariation(inferenceVariation);

			stepArgs = (Collection) Arrays.asList(data);

		} else {
			em = new FrequentistCausalEM(startingModel)
					.setStopCriteria(stopCriteria)
					.setThreshold(threshold)
					.setRegularization(0.0)
					.setInferenceVariation(inferenceVariation);

			stepArgs = (Collection) Arrays.asList(data);
		}

		em.setVerbose(verbose)
				.setRecordIntermediate(false)
				.setTrainableVars(this.trainableVars)
				.setInferenceMethod(method);


		em.run(stepArgs, maxEMIter);

		return (StructuralCausalModel) em.getPosterior();
	}


	public CComponentEMCredalBuilder setMaxEMIter(int maxEMIter) {
		this.maxEMIter = maxEMIter;
		return this;
	}

	public CComponentEMCredalBuilder setNumTrajectories(int numTrajectories) {
		this.numTrajectories = numTrajectories;
		return this;
	}


	public CComponentEMCredalBuilder setMask(boolean[] mask) {
		this.mask = mask;
		return this;
	}


	public CComponentEMCredalBuilder setWeightedEM(boolean weightedEM) {
		this.weightedEM = weightedEM;
		return this;
	}




	public CComponentEMCredalBuilder setStopCriteria(FrequentistCausalEM.StopCriteria stopCriteria) {
		this.stopCriteria = stopCriteria;
		return this;
	}

	private int inferenceVariation= 0;
	public CComponentEMCredalBuilder setInferenceVariation(int i) {
		this.inferenceVariation = i;
		return this;
	}

	private ComponentInference method; 
	public CComponentEMCredalBuilder setInference(ComponentInference inference) {
		this.method = inference;
		return this;
	}


}
