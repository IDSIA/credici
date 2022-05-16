package dev;

import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ch.idsia.credici.utility.EncodingUtil.getRandomSeqIntMask;

public class SelectBias {


	static int dataSize = 1000;
	static int maxiter = 500;
	static int executions = 50;

	static int numSelectorParents = 3;

	static int cause, effect;
	static TIntIntMap[] data;
	static StructuralCausalModel model;


	public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException {

		model = getModel();
		cause = model.getEndogenousVars()[0];
		effect = model.getEndogenousVars()[model.getEndogenousVars().length-1];
		data = model.samples(dataSize, model.getEndogenousVars());

		//runExact();
		runApprox();


	}

	private static void runApprox() throws InterruptedException, ExecutionControl.NotImplementedException {


		// Fix the parents of S
		int[] endoVars = model.getEndogenousVars();
		int[] parents = new int[numSelectorParents];
		parents[0] = endoVars[0];
		parents[numSelectorParents-1] = endoVars[endoVars.length-1];
		int idx[] = CollectionTools.shuffle(IntStream.range(1, endoVars.length-1).toArray());
		for(int i=0; i<numSelectorParents-2; i++)
			parents[i+1] = endoVars[idx[i]];



		int parentComb = model.getDomain(parents).getCombinations();

		List<int[]> assigList = getRandomSeqIntMask(parentComb, true);


		System.out.println(assigList.size()+" different selection tables");
		for(int[] assignments : assigList){
			System.out.println(Arrays.toString(assignments));
		}

		for(int[] assignments : assigList) {


			StructuralCausalModel modelBiased = SelectionBias.addSelector(model, parents, assignments);

			int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];
			TIntIntMap[] dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);

			int n1 = (int) Stream.of(dataBiased).filter(d->d.get(selectVar)==1).count();
			double pS1 = (1.0 * n1)/dataBiased.length;

			System.out.println("p(S=1):"+pS1);

			Watch.start();

			EMCredalBuilder builder = EMCredalBuilder.of(modelBiased, dataBiased)
					.setMaxEMIter(maxiter)
					.setNumTrajectories(executions)
					.setWeightedEM(true)
					.build();


			CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
			double[] res = runQueries(inf);
			Watch.stopAndPrint();

			int npoints = inf.pointsForConvergingPNS(0.90, cause,effect);
			System.out.println("Convergence with "+npoints+" runs (>90%)");
			System.out.println(Arrays.toString(res));
		}

	}

	private static void runExact() throws InterruptedException, ExecutionControl.NotImplementedException {
		Watch.start();
		EMCredalBuilder builder = EMCredalBuilder.of(model, data)
				.setMaxEMIter(maxiter)
				.setNumTrajectories(executions)
				.setWeightedEM(true)
				.build();


		CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
		double[] res = runQueries(inf);
		Watch.stopAndPrint();
		System.out.println(Arrays.toString(res));
	}

	private static double[] runQueries(CausalInference inf) throws ExecutionControl.NotImplementedException, InterruptedException {

		// runQueries(CausalInference inf)
		List resList = new ArrayList();
		VertexFactor p = null;
		double[] v = null;

		p = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);
		v =  Doubles.concat(p.getData()[0]);
		Arrays.sort(v);
		for(double val : v) resList.add(val);

		p = ((VertexFactor) inf.causalQuery().setIntervention(cause, 0).setTarget(effect).run()).filter(effect,0);
		v =  Doubles.concat(p.getData()[0]);
		Arrays.sort(v);
		for(double val : v) resList.add(val);

		IntervalFactor ace = (IntervalFactor) inf.averageCausalEffects(cause, effect, 1, 1, 0);
		resList.add(ace.getDataLower()[0][0]);
		resList.add(ace.getDataUpper()[0][0]);


		// return
		return resList.stream().mapToDouble(d -> (double)d).toArray();
	}

	private static StructuralCausalModel getModel() {
		///// Model
		StructuralCausalModel model = new StructuralCausalModel();
		int N = 5; //3

		for(int i=0; i<N; i++) {
			model.addVariable(2, false);
			if(i>0)
				model.addParents(i, i-1);
		}

		int[] X = model.getEndogenousVars();
		//int[] U = new int[2];
		int[] U = new int[4];

		U[0] = model.addVariable(2, true);
		U[1] = model.addVariable(8, true);
		U[2] = model.addVariable(8, true);


		model.addParents(X[0], U[0]);
		model.addParents(X[1], U[1]);
		model.addParents(X[2], U[1]);
		model.addParents(X[3], U[2]); //
		model.addParents(X[4], U[2]); //


		RandomUtil.setRandomSeed(1);
		model.fillWithRandomFactors(4);
		return model;
	}
}
