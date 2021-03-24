package ch.idsia.credici.model.builder;

import ch.idsia.credici.learning.BayesianCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntObjectMap;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EMCredalBuilder extends CredalBuilder{

	//todo getters and setters

	protected TIntObjectMap<BayesianFactor> empiricalFactors;

	private List<StructuralCausalModel>  selectedPoints;

	private List<List<StructuralCausalModel>> trayectories;

	private HashMap<Integer, BayesianFactor> endogJointProbs;

	private HashMap<Set<Integer>, BayesianFactor> targetGenDist;

	private int maxEMIter = 200;

	private int numTrayectories = 10;

	private boolean discardNonConverging = false;

	public enum SelectionPolicy {
		LAST,	// Selects the last point in the trajectory.
		BISECTION_BORDER_SAME_PATH, // Bisection with the 2 points in the same path closer to the border.
		BISECTION_BORDER,	// Bisection with 2 points from any point such that these are close to the border.
		BISECTION_ALL, // Bisection with all posible pair of points.

	}

	SelectionPolicy selPolicy = SelectionPolicy.LAST;



	public EMCredalBuilder(StructuralCausalModel causalModel){
		this.causalmodel = causalModel;
		this.endogJointProbs = causalModel.endogenousBlanketProb();
		this.targetGenDist = causalModel.getEmpiricalMap();
	}

	public static EMCredalBuilder of(StructuralCausalModel causalModel){
		return new EMCredalBuilder(causalModel);
	}


	@Override
	public EMCredalBuilder build() throws InterruptedException {
		buildTrayectories();
		selectPoints();
		mergePoints();
		return this;
	}


	private void buildTrayectories() throws InterruptedException {
		trayectories = new ArrayList<>();
		for(int i=0; i<numTrayectories; i++)
			trayectories.add(runEM());

	}

	private void selectPoints(){
		// If there is not any inner point, apply LAST,
		// which can always be applyed but the inner approximation is not guaranteed.

		if(selPolicy == SelectionPolicy.LAST){
			selectedPoints = trayectories.stream().map(t -> t.get(t.size()-1)).collect(Collectors.toList());
		}else{
			throw new IllegalArgumentException("Wrong selection policy");
		}




	}
	private void mergePoints(){

		BayesianNetwork[] bnets =
				selectedPoints.stream().map(s -> s.toBnet()).toArray(BayesianNetwork[]::new);

		model = VertexFactor.buildModel(true, bnets);

	}

	private StructuralCausalModel bisection(StructuralCausalModel out, StructuralCausalModel in, int splits){
		StructuralCausalModel p1 = out;
		StructuralCausalModel p2 = in;

		for(int i=0; i<splits; i++){
			StructuralCausalModel mid = p1.average(p2, p1.getExogenousVars());
			double ratio = Probability.ratioLogLikelihood(mid.getEmpiricalMap(), in.getEmpiricalMap(), 1);
			if(ratio<1.0)
				p1 = mid;
			else
				p2 = mid;
		}
		return p2;
	}

	private boolean isInside(StructuralCausalModel m){
		return Probability.ratioLogLikelihood(m.getEmpiricalMap(), targetGenDist, 1) == 1.0;
	}

	public List<StructuralCausalModel> getSelectedPoints() {
		return selectedPoints;
	}

	/**
	 * The result is an inner approximation if all the precise models composing the
	 * result are inside.
	 * @return
	 */
	public boolean isInnerApproximation(){
		return selectedPoints.stream().allMatch(this::isInside);
	}

	private List<StructuralCausalModel> runEM() throws InterruptedException {

		StructuralCausalModel startingModel =
				(StructuralCausalModel) BayesianFactor.randomModel(
						causalmodel, 5, false
						,causalmodel.getExogenousVars()
		);


		// Run EM in the causal model
		ExpectationMaximization em =
				new BayesianCausalEM(startingModel)
						.setVerbose(false)
						.setRecordIntermediate(true)
						.setRegularization(0.0)
						.setTrainableVars(causalmodel.getExogenousVars());

		// run the method
		em.run(endogJointProbs.values(), maxEMIter);

		// Return the trayectories
		return em.getIntermediateModels();

	}


	public static void main(String[] args) throws InterruptedException {



		RandomUtil.setRandomSeed(1);

		StructuralCausalModel m = new StructuralCausalModel();

		// Xs
		m.addVariable(2, false);
		//m.addVariable(3, false);
		m.addVariable(3, false);
		//   m.addVariable(3, false);

		// Us
		m.addVariable(2, true);
		m.addVariable(5, true);
		//m.addVariable(6, true);

		int[] X = m.getEndogenousVars();
		int[] U = m.getExogenousVars();

		for(int i=1; i<X.length; i++)
			m.addParents(X[i], X[i-1]);

		m.addParents(X[0], U[0]);
		m.addParents(X[1], U[1]);

		// m.addParents(X[2], U[0]);
		// m.addParents(X[3], U[1]);

		m.fillWithRandomFactors(3);

		EMCredalBuilder builder = EMCredalBuilder.of(m).build();

		builder.getSelectedPoints().forEach(System.out::println);

		System.out.println(builder.isInnerApproximation());

		System.out.println(builder.getModel());

	}






}
