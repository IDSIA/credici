package dev;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BiasSeparated1Var {
	public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException {

		int dataSize = 1000;
		int maxiter = 250;
		int executions = 50;


		///// Model
		StructuralCausalModel model = new StructuralCausalModel();
		int N = 1;

		for(int i=0; i<N; i++) {
			model.addVariable(8, false);
			if(i>0)
				model.addParents(i, i-1);
		}

		int[] X = model.getEndogenousVars();
		int[] U = new int[1];

		U[0] = model.addVariable(8, true);
		//U[1] = model.addVariable(8, true);

		model.addParents(X[0], U[0]);


		RandomUtil.setRandomSeed(1);
		model.fillWithRandomFactors(4);


		/////// Sample data over the Xs
		TIntIntMap[] data = model.samples(dataSize, model.getEndogenousVars());

		int[] assignments = {1,1,1,1, 1,0,1,0};





		StructuralCausalModel modelBiased = SelectionBias.addSelectorFullyConnected(model, assignments);

		int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];
		TIntIntMap[]  dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);

		TIntIntMap[]  dataBiased0 = Arrays.stream(dataBiased).filter(d -> d.get(selectVar)==0).toArray(TIntIntMap[]::new);
		TIntIntMap[]  dataBiased1 = Arrays.stream(dataBiased).filter(d -> d.get(selectVar)==1).toArray(TIntIntMap[]::new);





		// Exact results (without selector and all the data)

		EMCredalBuilder builder = EMCredalBuilder.of(model, data)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.build();


		VertexFactor p = null;//(VertexFactor) new CausalMultiVE(builder.getSelectedPoints()).probNecessityAndSufficiency(cause, effect);
		System.out.println(p);

		//runApprox

		int endoComb = model.getDomain(model.getEndogenousVars()).getCombinations();

		Watch.start();



		builder = EMCredalBuilder.of(modelBiased, dataBiased)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.build();

		//p = (VertexFactor) new CausalMultiVE(builder.getSelectedPoints()).probNecessityAndSufficiency(cause, effect);
		System.out.println(p);

		// Run EM separatelly

		EMCredalBuilder builder0 = EMCredalBuilder.of(modelBiased, dataBiased0)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.build();



		EMCredalBuilder builder1 = EMCredalBuilder.of(modelBiased, dataBiased1)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setVerbose(true)
				.setWeightedEM(true)
				.build();






		double p0 = ((double)dataBiased0.length) / data.length;


		List mergedModels = new ArrayList();


		for(StructuralCausalModel m0 : builder0.getSelectedPoints()){
			for(StructuralCausalModel m1 : builder1.getSelectedPoints()) {

				StructuralCausalModel m0_ = m0.copy();
				StructuralCausalModel m1_ = m1.copy();

				m0_.removeVariable(selectVar);
				m1_.removeVariable(selectVar);

				m0_.removeVariable(m0.getExogenousParents(selectVar)[0]);
				m1_.removeVariable(m1.getExogenousParents(selectVar)[0]);


				for(int u: model.getExogenousVars()){
					BayesianFactor fu = m0.conditionalProb(u, selectVar).filter(selectVar,0);
					m0_.setFactor(u, fu);
				}
				for(int u: model.getExogenousVars()) {
					BayesianFactor fu = m1.conditionalProb(u, selectVar).filter(selectVar, 1);
					m1_.setFactor(u, fu);
				}


				mergedModels.add(m0_.average(m1_, p0, model.getExogenousVars()));
				System.out.print(".");
			}
			System.out.println("");
		}


		//new CausalMultiVE(mergedModels).probNecessityAndSufficiency(cause, effect);


	}
}
