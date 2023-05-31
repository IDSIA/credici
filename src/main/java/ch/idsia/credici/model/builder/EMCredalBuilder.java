package ch.idsia.credici.model.builder;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.BayesianCausalEM;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.learning.WeightedCausalEM;
import ch.idsia.credici.learning.inference.ComponentInference;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
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

public class EMCredalBuilder extends CredalBuilder{

	//todo getters and setters
	// todo: make private
	public TIntObjectMap<BayesianFactor> empiricalFactors;

	public List<StructuralCausalModel>  selectedPoints;

	public List<List<StructuralCausalModel>> trajectories;

	private boolean[] mask;

	public HashMap<Integer, BayesianFactor> endogJointProbs;

	public HashMap<Set<Integer>, BayesianFactor> inputGenDist;

	public HashMap<Set<Integer>, BayesianFactor> targetGenDist;

	public int maxEMIter = 200;

	public int numTrajectories = 10;

	public boolean discardNonConverging = false;

	public int splits = 10;

	private int numDecimalsRound = 5;

	private SparseModel trueCredalModel;

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

	public EMCredalBuilder(StructuralCausalModel causalModel){
		this.causalmodel = causalModel;
		this.endogJointProbs = causalModel.endogenousBlanketProb();
		this.trainableVars = causalModel.getExogenousVars();
	}

	public static EMCredalBuilder of(StructuralCausalModel causalModel){
		return new EMCredalBuilder(causalModel);
	}


	public EMCredalBuilder(StructuralCausalModel causalModel, TIntIntMap[] data){
		this.causalmodel = causalModel;
		this.data = data;
		this.trainableVars = causalModel.getExogenousVars();
	}



	public EMCredalBuilder(StructuralCausalModel causalModel, TIntIntMap[] data, HashMap genDist){
		this.causalmodel = causalModel;
		this.endogJointProbs = causalModel.endogenousBlanketProb();
		this.data = data;
		this.trainableVars = causalModel.getExogenousVars();
	}

	public static EMCredalBuilder of(StructuralCausalModel causalModel, TIntIntMap[] data){
		return new EMCredalBuilder(causalModel, data);
	}


	@Override
	public EMCredalBuilder build(int... exoVars) throws InterruptedException {
		if(exoVars.length>0)
			throw new NotImplementedException("Not implemented EMbuilder for a subsection of exogenous variables");
		buildTrajectories();
		selectPoints();
		mergePoints();
		return this;
	}

	public EMCredalBuilder selectAndMerge(){
		selectPoints();
		mergePoints();
		return this;
	}

	public EMCredalBuilder buildTrajectories() throws InterruptedException {
		trajectories = new ArrayList<>();
	
		if (inferenceVariation == 5 && this.method != null){
			this.method.initialize(causalmodel);
		}

		for(int i = 0; i < numTrajectories; i++) {
			trajectories.add(runEM());
		}
		return this;
	}


	private void selectPoints() {
		// If there is not any inner point, apply LAST,
		// which can always be applyed but the inner approximation is not guaranteed.
		
		if(selPolicy == SelectionPolicy.LAST || !hasInnerPoint()) {
			selectedPoints = getTrajectories().stream().map(t -> t.get(t.size() - 1)).collect(Collectors.toList());
		}else {
			if (selPolicy == SelectionPolicy.BISECTION_ALL) {

				List<StructuralCausalModel> in = getTrajectories().stream()
						.flatMap(t -> t.stream().filter(this::isInside))
						.collect(Collectors.toList());

				List<StructuralCausalModel> out = getTrajectories().stream()
						.flatMap(t -> t.stream().filter(this::isOutside))
						.collect(Collectors.toList());

				selectedPoints = bisection(out, in);
			} else if (selPolicy == SelectionPolicy.BISECTION_BORDER) {

				List<StructuralCausalModel> in = getTrajectories().stream()
						.map(t -> getFirstInside(t))
						.filter(p -> p != null)
						.collect(Collectors.toList());

				List<StructuralCausalModel> out = getTrajectories().stream()
						.map(t -> getLastOutside(t))
						.filter(p -> p != null)
						.collect(Collectors.toList());

				selectedPoints = bisection(out, in);

			} else if (selPolicy == SelectionPolicy.BISECTION_BORDER_SAME_PATH) {

				selectedPoints = new ArrayList<>();

				for (List<StructuralCausalModel> t : getConvergingTrajectories()) {
					StructuralCausalModel out = getLastOutside(t);
					StructuralCausalModel in = getFirstInside(t);
					if (out != null && in != null)
						selectedPoints.add(bisection(out, in));
				}
			} else {
				throw new IllegalArgumentException("Wrong selection policy");
			}
		}

	}

	public EMCredalBuilder setThreshold(double threshold) {
		this.threshold = threshold;
		return this;
	}

	private StructuralCausalModel getFirstInside(List<StructuralCausalModel> t){
		return t.stream().filter(this::isInside).reduce((f, s) -> f).orElse(null);
	}

	private StructuralCausalModel getLastOutside(List<StructuralCausalModel> t){
		return t.stream().filter(this::isOutside).reduce((f, s) -> s).orElse(null);
	}

	public List<List<StructuralCausalModel>> getTrajectories(){

		List<List<StructuralCausalModel>> out;
		if(mask == null)
			out = this.trajectories;
		else{
			out = new ArrayList<>();
			for(int i=0; i<mask.length; i++){
				if(mask[i])
					out.add(trajectories.get(i));
			}
		}

		return out;

	}

	public List<List<StructuralCausalModel>> getConvergingTrajectories(){
		return getTrajectories().stream()
				.filter(t -> isInside(t.get(t.size()-1)))
				.collect(Collectors.toList());
	}

	private boolean hasInnerPoint(){
		for(List<StructuralCausalModel> t : trajectories){
			if(isInside(t.get(t.size()-1))) {
				return true;
			}
		}
		return false;
	}


	private void mergePoints(){

		BayesianNetwork[] bnets =
				selectedPoints.stream().map(s -> s.toBnet()).toArray(BayesianNetwork[]::new);

		if(buildCredalModel)
			model = VertexFactor.buildModel(true, bnets);

	}

	private List<StructuralCausalModel> bisection(List<StructuralCausalModel> out, List<StructuralCausalModel> in){

		System.out.println("\tNumber of bisections = "+out.size()+"*"+in.size()+" = "+(out.size()*in.size()));
		List<StructuralCausalModel> midPoints  = new ArrayList<>();
		for(StructuralCausalModel pOut : out){
			for(StructuralCausalModel pIn : in){
				midPoints.add(bisection(pOut, pIn));
			}
		}
		return midPoints;
	}

	private StructuralCausalModel bisection(StructuralCausalModel out, StructuralCausalModel in){

		if(isOutside(in) || isInside(out))
			throw new IllegalArgumentException("Bad points for bisection");

		StructuralCausalModel p1 = out;
		StructuralCausalModel p2 = in;

		double ratio =  0;

		for(int i=0; i<this.splits; i++){
			StructuralCausalModel mid = p1.average(p2, p1.getExogenousVars());
			if(isOutside(mid))
				p1 = mid;
			else
				p2 = mid;

		}

		if(isOutside(p2)) {
			System.out.println("bad bisection");
			System.out.println(ratio);

		}
		return p2;
	}

	public boolean isInside(StructuralCausalModel m){
		/*f(trueCredalModel != null) {
			for (int u : m.getExogenousVars())
				if (!Probability.vertexInside(m.getFactor(u), (VertexFactor) trueCredalModel.getFactor(u)))
					return false;
			return true;
		}*/
		return ratioLk(m) >= 1.0;
	}
	public boolean isOutside(StructuralCausalModel m){
		return !isInside(m);
	}
	public double ratioLk(StructuralCausalModel m){
		HashMap<Set<Integer>, BayesianFactor> map_i = m.getEmpiricalMap(false);
		if(numDecimalsRound>0)
			map_i = FactorUtil.fixEmpiricalMap(map_i, numDecimalsRound);

		return Probability.ratioLogLikelihood(map_i, targetGenDist, 1);
	}

	public double klPQ(StructuralCausalModel m, boolean zeroSafe){
		HashMap<Set<Integer>, BayesianFactor> map_i = m.getEmpiricalMap(false);
		if(numDecimalsRound>0)
			map_i = FactorUtil.fixEmpiricalMap(map_i, numDecimalsRound);
		return Probability.KL(targetGenDist, map_i, zeroSafe);
	}
	public double klQP(StructuralCausalModel m, boolean zeroSafe){
		HashMap<Set<Integer>, BayesianFactor> map_i = m.getEmpiricalMap(false);
		if(numDecimalsRound>0)
			map_i = FactorUtil.fixEmpiricalMap(map_i, numDecimalsRound);
		return Probability.KL(map_i, targetGenDist, zeroSafe);
	}
	public double klsym(StructuralCausalModel m, boolean zeroSafe){
		HashMap<Set<Integer>, BayesianFactor> map_i = m.getEmpiricalMap(false);
		if(numDecimalsRound>0)
			map_i = FactorUtil.fixEmpiricalMap(map_i, numDecimalsRound);
		return Probability.KLsymmetrized(map_i, targetGenDist, zeroSafe);
	}

	public List<StructuralCausalModel> getSelectedPoints() {
		return selectedPoints;
	}

	public EMCredalBuilder setTrueCredalModel(SparseModel trueCredalModel) {
		this.trueCredalModel = trueCredalModel;
		return this;
	}

	public EMCredalBuilder setVerbose(boolean verbose) {
		this.verbose = verbose;
		return this;
	}

	public EMCredalBuilder setBuildCredalModel(boolean buildCredalModel) {
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

	private double aceQueryTime;
	private double aceSetupTime;
	public double getAceQueryTime() {
		return aceQueryTime;
	}
	public double getAceSetupTime() {
		return aceSetupTime;
	}


	private LinkedList<StructuralCausalModel> randomModels;

	/**
	 * Sequence of Structural Causal Models to be used as reference.
	 * @param models
	 */
	public EMCredalBuilder setRandomModels(StructuralCausalModel reference, StructuralCausalModel[] models) {
		this.randomModels = new LinkedList<>(Arrays.stream(models).map(m->{
			StructuralCausalModel model =  reference.copy();
			for (int exo : model.getExogenousVars()) {
				model.setFactor(exo, m.getFactor(exo).copy());
			}
			return model;
		}).collect(Collectors.toCollection(LinkedList<StructuralCausalModel>::new)));
		return this;
	}

	private StructuralCausalModel randomModel(StructuralCausalModel reference) {
		if (randomModels != null && !randomModels.isEmpty()) {
			return randomModels.removeFirst();
		} else {
			StructuralCausalModel rmodel = reference.copy();
			rmodel.fillExogenousWithRandomFactors();
			return rmodel;
		}
	}

	private List<StructuralCausalModel> runEM() throws InterruptedException {
		// if (inferenceVariation == 5 && this.method != null){
		// 	this.method.initialize(causalmodel);
		// 	System.out.println("compiled" + causalmodel);
		// }
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
				.setRecordIntermediate(true)
				.setTrainableVars(this.trainableVars)
				.setInferenceMethod(method);

		em.run(stepArgs, maxEMIter);

		List<StructuralCausalModel> t = em.getIntermediateModels().stream().map(n->(StructuralCausalModel)n).collect(Collectors.toList());

		if(verbose)
			System.out.println(" calculated EM trajectory of "+(t.size()-1));


		// Return the trajectories
		return t;

	}

	public EMCredalBuilder setSelPolicy(SelectionPolicy selPolicy) {
		this.selPolicy = selPolicy;
		return this;
	}

	public EMCredalBuilder setMaxEMIter(int maxEMIter) {
		this.maxEMIter = maxEMIter;
		return this;
	}

	public EMCredalBuilder setNumTrajectories(int numTrajectories) {
		this.numTrajectories = numTrajectories;
		return this;
	}


	public EMCredalBuilder setMask(boolean[] mask) {
		this.mask = mask;
		return this;
	}

	public EMCredalBuilder setNumDecimalsRound(int numDecimalsRound) {
		this.numDecimalsRound = numDecimalsRound;
		setTargetGenDist();
		return this;
	}

	public EMCredalBuilder setWeightedEM(boolean weightedEM) {
		this.weightedEM = weightedEM;
		return this;
	}

	private void setTargetGenDist(){
		this.targetGenDist = this.inputGenDist;
	}

	public  EMCredalBuilder setTrainableVars(int[] trainableVars) {
		this.trainableVars = trainableVars;
		return this;
	}


	public EMCredalBuilder setStopCriteria(FrequentistCausalEM.StopCriteria stopCriteria) {
		this.stopCriteria = stopCriteria;
		return this;
	}

	private int inferenceVariation= 0;
	public EMCredalBuilder setInferenceVariation(int i) {
		this.inferenceVariation = i;
		return this;
	}

	private ComponentInference method; 
	public EMCredalBuilder setInference(ComponentInference inference) {
		this.method = inference;
		return this;
	}


}
