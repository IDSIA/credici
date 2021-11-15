package dev;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BiasSeparated {
	public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException {

		int dataSize = 1000;
		int maxiter = 2000;
		int executions = 40;


		///// Model
		StructuralCausalModel model = new StructuralCausalModel();
		int N = 3;

		for(int i=0; i<N; i++) {
			model.addVariable(2, false);
			if(i>0)
				model.addParents(i, i-1);
		}

		int[] X = model.getEndogenousVars();
		int[] U = new int[2];

		U[0] = model.addVariable(2, true);
		U[1] = model.addVariable(8, true);

		model.addParents(X[0], U[0]);
		model.addParents(X[1], U[1]);
		model.addParents(X[2], U[1]);


		RandomUtil.setRandomSeed(1);
		model.fillWithRandomFactors(4);

		///  init

		int cause = model.getEndogenousVars()[0];
		int effect = model.getEndogenousVars()[model.getEndogenousVars().length-1];

		/////// Sample data over the Xs
		TIntIntMap[] data = model.samples(dataSize, model.getEndogenousVars());



		int[] assignments = {1,1,1,1, 1,0,1,0};






		StructuralCausalModel modelBiased = SelectionBias.addSelectorFullyConnected(model, assignments);

		int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];
		TIntIntMap[]  dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);

		TIntIntMap[]  dataBiased0 = Arrays.stream(dataBiased).filter(d -> d.get(selectVar)==0).toArray(TIntIntMap[]::new);
		TIntIntMap[]  dataBiased1 = Arrays.stream(dataBiased).filter(d -> d.get(selectVar)==1).toArray(TIntIntMap[]::new);


		double ps0 = ((double)dataBiased0.length) / data.length;
		System.out.println("P(S=1)="+(1-ps0));



		// Exact results (without selector and all the data)

		EMCredalBuilder builder = EMCredalBuilder.of(model, data)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.build();


		VertexFactor p = (VertexFactor) new CausalMultiVE(builder.getSelectedPoints()).probNecessityAndSufficiency(cause, effect);



		//runApprox

		int endoComb = model.getDomain(model.getEndogenousVars()).getCombinations();

		Watch.start();



		EMCredalBuilder builder01 = EMCredalBuilder.of(modelBiased, dataBiased)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.setVerbose(true)
				.build();

		VertexFactor p01 = (VertexFactor) new CausalMultiVE(builder01.getSelectedPoints()).probNecessityAndSufficiency(cause, effect);


		// Run EM separatelly
		System.out.println("running for D0");
		EMCredalBuilder builder0 = EMCredalBuilder.of(modelBiased, dataBiased0)
				.setMaxEMIter(maxiter)
				.setTrainableVars(U)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.setVerbose(true)
				.build();


		System.out.println("running for D1");

		EMCredalBuilder builder1 = EMCredalBuilder.of(modelBiased, dataBiased1)
				.setMaxEMIter(maxiter)
				.setTrainableVars(U)
				.setNumTrajectories(executions)
				.setVerbose(true)
				.setWeightedEM(true)
				.build();








		List mergedModelsCond = new ArrayList();
		List mergedModelsMarg = new ArrayList();



		for(StructuralCausalModel m0 : builder0.getSelectedPoints()){
			for(StructuralCausalModel m1 : builder1.getSelectedPoints()) {

				StructuralCausalModel m0_ = m0.copy();
				StructuralCausalModel m1_ = m1.copy();

				m0_.removeVariable(selectVar);
				m1_.removeVariable(selectVar);

				m0_.removeVariable(m0.getExogenousParents(selectVar)[0]);
				m1_.removeVariable(m1.getExogenousParents(selectVar)[0]);


				VariableElimination inf = null;
				for(int u: model.getExogenousVars()){
					//BayesianFactor fu = m0.conditionalProb(u, selectVar).filter(selectVar,0);

					inf = new FactorVariableElimination(m0.getVariables());
					inf.setFactors(m0.getFactors());
					inf.setEvidence(ObservationBuilder.observe(selectVar, 0));
					BayesianFactor fu = (BayesianFactor) inf.run(u);

					m0_.setFactor(u, fu);
				}
				for(int u: model.getExogenousVars()) {
					//BayesianFactor fu = m1.conditionalProb(u, selectVar).filter(selectVar, 1);
					inf = new FactorVariableElimination(m1.getVariables());
					inf.setFactors(m1.getFactors());
					inf.setEvidence(ObservationBuilder.observe(selectVar, 1));
					BayesianFactor fu = (BayesianFactor) inf.run(u);

					m1_.setFactor(u, fu);
				}


				mergedModelsCond.add(m0_.average(m1_, ps0, model.getExogenousVars()));
				mergedModelsMarg.add(m0.average(m1, ps0, model.getExogenousVars()));

				System.out.print(".");
			}
			System.out.println("");
		}


		System.out.println("P(S=1)="+(1-ps0));

		System.out.println("full EM without bias");
		System.out.println(p);

		System.out.println("full EM with bias");
		System.out.println(p01);

		System.out.println("separated EM with bias (conditioning)");
		VertexFactor p01cond = (VertexFactor) new CausalMultiVE(mergedModelsCond).probNecessityAndSufficiency(cause, effect);
		System.out.println(p01cond);

		System.out.println("separated EM with bias (marginals)");
		VertexFactor p01marg = (VertexFactor) new CausalMultiVE(mergedModelsMarg).probNecessityAndSufficiency(cause, effect);
		System.out.println(p01marg);

	}
}
