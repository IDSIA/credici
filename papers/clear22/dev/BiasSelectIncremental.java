package dev;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import ch.idsia.credici.collections.FIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BiasSelectIncremental {

	static int EMruns = 100;
	static int dataSize = 5000;


	public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException {

		StructuralCausalModel m = new StructuralCausalModel();
		int N = 3;


		for(int i=0; i<N; i++) {
			m.addVariable(2, false);
			if(i>0)
				m.addParents(i, i-1);
		}

		int[] X = m.getEndogenousVars();
		int[] U = new int[2];

		U[0] = m.addVariable(2, true);
		U[1] = m.addVariable(8, true);

		m.addParents(X[0], U[0]);
		m.addParents(X[1], U[1]);
		m.addParents(X[2], U[1]);


		RandomUtil.setRandomSeed(1);
		m.fillWithRandomFactors(4);

		CredalCausalVE inf = new CredalCausalVE(m);

		VertexFactor res = (VertexFactor) inf.probNecessityAndSufficiency(X[0], X[2]);
		System.out.println(res);    // [0.185, 0.212]


		// Sample data over the Xs
		RandomUtil.setRandomSeed(1);
		TIntIntMap[] dataX = m.samples(dataSize, m.getEndogenousVars());

		System.out.println("Starting...");

		int[] assignments = new int[]{0,0,0,0, 0,0,0,0};
		getIntervalFactor(m, dataX, assignments);

		RandomUtil.setRandomSeed(1);

		int[] idx = CollectionTools.shuffle(IntStream.range(0, assignments.length).toArray());
		for(int i : idx){
			assignments[i] = 1;
			getIntervalFactor(m, dataX, assignments);
		}

	}

	private static VertexFactor getIntervalFactor(StructuralCausalModel m, TIntIntMap[] dataX, int[] assignments) throws InterruptedException, ExecutionControl.NotImplementedException {

		int[] X= m.getEndogenousVars();

		// Add the selector variable
		StructuralCausalModel prior = m.copy();
		int s = prior.addVariable(2, false);
		int us = prior.addVariable(1, true);

		prior.addParent(s,us);
		prior.addParents(s, X);
		prior.setFactor(us, new BayesianFactor(prior.getDomain(us), new double[]{1.0}));

		//prior.randomizeEndoFactor(s);    // Change this node to change the proportion


		BayesianFactor fs = EquationBuilder.fromVector(
				prior.getDomain(s),
				prior.getDomain(prior.getParents(s)),
				assignments
		);
		prior.setFactor(s, fs);


		// Add the value of S
		TIntIntMap[] data =  Stream.of(dataX).map(d -> {
			FIntIntHashMap dnew = new FIntIntHashMap(d);
			int valS = (int) prior.getFactor(s).filter((FIntIntHashMap) d).getData()[1];
			dnew.put(s, valS);
			return dnew;
		}).toArray(TIntIntMap[]::new);


		// Remove X values when  S=0
		data =
				Stream.of(data).map(d -> {
					if(d.get(s)==0){
						for(int x : X)
							d.remove(x);
					}
					return d;
				}).toArray(TIntIntMap[]::new);



		int n0 = (int) Stream.of(data).filter(d->d.get(s)==0).count();
		int n1 = data.length - n0;




		//// Inference variables
		EMCredalBuilder builder = null;
		CausalMultiVE mve = null;





		RandomUtil.setRandomSeed(1);
		prior.fillExogenousWithRandomFactors(4);
		//Watch.start();
		builder = EMCredalBuilder.of(prior, data)
				.setNumTrajectories(EMruns)
				.setBuildCredalModel(false)
				.setWeightedEM(true)
				.setVerbose(false)
				//.setSelPolicy(EMCredalBuilder.SelectionPolicy.BISECTION_BORDER_SAME_PATH)
				.build();

		//Watch.stopAndPrint();
		//System.out.println("finished learning");

		//System.out.println("\n\nEMCC results)");

		mve = new CausalMultiVE(builder.getSelectedPoints());//.setToInterval(true);

		VertexFactor pns = (VertexFactor) mve.probNecessityAndSufficiency(X[0], X[2]);
		double pnsu = Math.max(pns.getData()[0][0][0], pns.getData()[0][1][0]);
		double pnsl = Math.min(pns.getData()[0][0][0], pns.getData()[0][1][0]);

		System.out.println(((double)n1/(n1+n0))+"\t"+pnsl+"\t"+pnsu);

		return pns;

	}
}
