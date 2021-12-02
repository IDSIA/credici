package dev;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BiasSeparated2Us2Xs {
	public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException {

		int dataSize = 1000;
		int maxiter = 100;
		int executions = 20;


		///// Model
		StructuralCausalModel model = new StructuralCausalModel();
		int X = model.addVariable(2, false);
		int Y = model.addVariable(2, false);
		int U = model.addVariable(2, true);
		int V = model.addVariable(4, true);

		model.addParents(X, U);
		model.addParents(Y,X,V);

		RandomUtil.setRandomSeed(1);
		model.fillWithRandomFactors(4);

		for(int x : model.getEndogenousVars())
			model.setFactor(x, EquationBuilder.of(model).withAllAssignments(x));

		/////// Sample data over the Xs
		TIntIntMap[] data = model.samples(dataSize, model.getEndogenousVars());

		int[] assignments = {1,0,0,1};



		StructuralCausalModel modelBiased = SelectionBias.addSelectorFullyConnected(model, assignments);



		int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];
		int Us = modelBiased.getExogenousParents(selectVar)[0];
		//Print selector function
		FactorUtil.print(modelBiased.getFactor(selectVar).filter(Us, 0).filter(selectVar,1));



		TIntIntMap[]  dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);

		TIntIntMap[]  dataBiased0 = Arrays.stream(dataBiased).filter(d -> d.get(selectVar)==0).toArray(TIntIntMap[]::new);
		TIntIntMap[]  dataBiased1 = Arrays.stream(dataBiased).filter(d -> d.get(selectVar)==1).toArray(TIntIntMap[]::new);

		int cause = X;
		int effect = Y;




		// Exact results (without selector and all the data)

		EMCredalBuilder builder = EMCredalBuilder.of(model, data)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.setVerbose(true)
				.build();


		VertexFactor p  = (VertexFactor) new CausalMultiVE(builder.getSelectedPoints()).probNecessityAndSufficiency(cause, effect);
		System.out.println(p);



		EMCredalBuilder builder01 = EMCredalBuilder.of(modelBiased, dataBiased)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.setVerbose(true)
				.setBuildCredalModel(true)
				.build();

		VertexFactor p01 = (VertexFactor) new CausalMultiVE(builder01.getSelectedPoints()).probNecessityAndSufficiency(cause, effect);
		System.out.println(p01);



		// Run EM separatelly

		EMCredalBuilder builder0 = EMCredalBuilder.of(modelBiased, dataBiased0)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setVerbose(true)
				.setWeightedEM(true)
				.build();



		EMCredalBuilder builder1 = EMCredalBuilder.of(modelBiased, dataBiased1)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setVerbose(true)
				.setWeightedEM(true)
				.build();


		double ps0 = ((double)dataBiased0.length) / data.length;
		double ps1 = ((double)dataBiased1.length) / data.length;



		for(int j= 0; j<5; j++){
			StructuralCausalModel m0 = builder0.getSelectedPoints().get(j);
			//System.out.println(BayesianFactor.combineAll(m0.getFactors(U,V)));
			System.out.println(m0.getFactor(V));
		}


		for(int j= 0; j<5; j++){
			StructuralCausalModel m1 = builder1.getSelectedPoints().get(j);
			//System.out.println(BayesianFactor.combineAll(m1.getFactors(U,V)));
			System.out.println(m1.getFactor(V));

		}

		// PNS separated
		VertexFactor p0 = (VertexFactor) new CausalMultiVE(builder0.getSelectedPoints()).probNecessityAndSufficiency(cause, effect);
		VertexFactor p1 = (VertexFactor) new CausalMultiVE(builder1.getSelectedPoints()).probNecessityAndSufficiency(cause, effect);





		// Compute

		List mergedModels = new ArrayList();

		for(StructuralCausalModel m0 : builder0.getSelectedPoints()){
			for(StructuralCausalModel m1 : builder1.getSelectedPoints()) {
/*
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
*/

				//		mergedModels.add(m0_.average(m1_, ps0, model.getExogenousVars()));
				mergedModels.add(m0.average(m1, ps0, model.getExogenousVars()));

				System.out.print(".");
			}
			System.out.println("");
		}




		RandomUtil.setRandomSeed(1);
		Random r = RandomUtil.getRandom();

		for(int j =0; j<10; j++){
			System.out.println(
					new CausalVE((StructuralCausalModel) mergedModels.get(r.nextInt(mergedModels.size()))).probNecessityAndSufficiency(cause, effect)
			);
		}

		VertexFactor p01sep = (VertexFactor) new CausalMultiVE(mergedModels).probNecessityAndSufficiency(cause, effect);
		System.out.print(p01sep);






		///////// p(U,V|S) calculus


		StructuralCausalModel rmodel = modelBiased.copy();
		rmodel.fillExogenousWithRandomFactors(4);


		// P(U,S=0)
		BayesianFactor pus0 = BayesianFactor.combineAll(rmodel.getFactors(X,Y,selectVar, U,V))
				.filter(Us,0)
				.marginalize(X,Y,V)
				.filter(selectVar,0);


		// P(V,S=0)
		BayesianFactor pvs0 = BayesianFactor.combineAll(rmodel.getFactors(X,Y,selectVar, U,V))
				.filter(Us,0)
				.marginalize(X,Y,U)
				.filter(selectVar,0);

		// P(U,V,S=0)
		BayesianFactor puvs0 = BayesianFactor.combineAll(rmodel.getFactors(X,Y,selectVar, U,V))
				.filter(Us,0)
				.marginalize(X,Y)
				.filter(selectVar,0);

		FactorUtil.print(pvs0);


		BayesianFactor puv = BayesianFactor.combineAll(rmodel.getFactors(U,V));
		BayesianFactor pu = rmodel.getFactor(U);
		BayesianFactor pv = rmodel.getFactor(V);




		// P(U|S=0)
		System.out.println(pus0.divide(pus0.marginalize(U)));

		// P(U|S=1)
		System.out.println(pvs0.divide(pvs0.marginalize(V)));

	}
}
