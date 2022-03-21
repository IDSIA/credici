package dev;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.CredalVariableElimination;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.util.stream.Stream;

public class EMwithSampling {
	public static void main(String[] args) throws InterruptedException {
		// Case (a)
		BayesianNetwork bnet = new BayesianNetwork();

		int x = bnet.addVariable(2); // no degree - with degree
		int y = bnet.addVariable(2); // low income - high income
		int s = bnet.addVariable(2);	// not selected - selected

	/*
			Fig. 1a

	 		0 is X with states no degree (0), with degree (1)
	 		1 is Y with states low income (0), high income (1)
	 		2 is S with states not selected (0), selected (1)

f([0])
-------------------------------
f({0=0}) = 0.4
f({0=1}) = 0.6
-------------------------------
f([1, 0])
-------------------------------
f({1=0, 0=0}) = 0.8
f({1=1, 0=0}) = 0.2
f({1=0, 0=1}) = 0.3
f({1=1, 0=1}) = 0.7
-------------------------------
f([2, 0, 1]) ------ XNOR
-------------------------------
f({2=0, 1=0, 0=0}) = 0.0
f({2=1, 1=0, 0=0}) = 1.0
f({2=0, 1=0, 0=1}) = 1.0
f({2=1, 1=0, 0=1}) = 0.0
f({2=0, 1=1, 0=0}) = 1.0
f({2=1, 1=1, 0=0}) = 0.0
f({2=0, 1=1, 0=1}) = 0.0
f({2=1, 1=1, 0=1}) = 1.0
-------------------------------




	 */

		bnet.addParents(s,x);
		bnet.addParents(s,x, y);
		bnet.addParents(y,x);

		BayesianFactor px = new BayesianFactor(bnet.getDomain(x), new double[]{0.4, 0.6});
		BayesianFactor py_x = new BayesianFactor(bnet.getDomain(y, x), new double[]{0.8, 0.2, 0.3, 0.7});
		BayesianFactor ps_xy = 	BayesianFactor.deterministic(bnet.getDomain(s), bnet.getDomain(x,y), 1,0,0,1);



		bnet.setFactor(x, px);
		bnet.setFactor(y, py_x);
		bnet.setFactor(s, ps_xy);

		for(int v: bnet.getVariables())
			FactorUtil.print(bnet.getFactor(v));


		// sample and drop S=0
		RandomUtil.setRandomSeed(1);
		TIntIntMap[] data = bnet.samples(500, bnet.getVariables());
		data = Stream.of(data).filter(d -> d.get(s)==1 ).toArray(TIntIntMap[]::new);



		// causal model


		StructuralCausalModel prior = new StructuralCausalModel();
		for(int v: bnet.getVariables())
			prior.addVariable(v, bnet.getSize(v), false);

		for(int v: bnet.getVariables())
			prior.addParents(v, bnet.getParents(v));


		prior = CausalBuilder.of(bnet).setFillRandomExogenousFactors(2).build();

		int us = prior.getExogenousParents(s)[0];
		prior.removeVariable(us);
		prior.addVariable(us,1,true);
		prior.setFactor(s, ps_xy);




		//// Inference variables
		EMCredalBuilder builder = null;
		SparseModel vmodel = null;
		VariableElimination ve = null;
		CredalVariableElimination cve = null;
		CausalMultiVE mve = null;


		int m = data.length;
		int n = 500 - m;

		for(int k = 0; k<30; k++) {
			System.out.println("iteration" + k);


			// Sampling X,Y | S = 0

			ve = new FactorVariableElimination(prior.getVariables());
			ve.setFactors(prior.getFactors());
			ve.setEvidence(ObservationBuilder.observe(s, 0));
			BayesianFactor psampling = (BayesianFactor) ve.run(x, y);

			System.out.println(psampling);


			TIntIntMap[] dataExt = new TIntIntMap[m + n];
			for (int i = 0; i < m; i++)
				dataExt[i] = data[i];

			//TIntIntMap sample = FactorUtil.fixPrecission(psampling, 5, false, x, y).sample();

			for (int i = 0; i < n; i++) {
				TIntIntMap sample = FactorUtil.fixPrecission(psampling, 5, false, x, y).sample();
				sample.put(s, 0);
				dataExt[i + m] = sample;
			}


			builder = EMCredalBuilder.of(prior, dataExt)
					.setNumTrajectories(10)
					.setBuildCredalModel(true)
					.build();

			vmodel = builder.getModel();


			System.out.println("Results");

			System.out.println("p(x|y=0)");

			// queries
			//p(x|y)
			ve = new FactorVariableElimination(bnet.getVariables());
			ve.setFactors(bnet.getFactors());
			ve.setEvidence(ObservationBuilder.observe(y, 0));
			System.out.println(ve.run(x));    // [0.6400000000000001, 0.36]

			cve = new CredalVariableElimination(vmodel);
			System.out.println(cve.query(x, ObservationBuilder.observe(y, 0)));

			//mve = new CausalMultiVE(builder.getSelectedPoints());
			//System.out.println(mve.query(x, ObservationBuilder.observe(y, 0)));


			System.out.println("\np(y|x=0)");
			// p(y|x)
			ve = new FactorVariableElimination(bnet.getVariables());
			ve.setFactors(bnet.getFactors());
			ve.setEvidence(ObservationBuilder.observe(x, 0));
			System.out.println(ve.run(y));    // [0.8, 0.2]

			cve = new CredalVariableElimination(vmodel);
			System.out.println(cve.query(y, ObservationBuilder.observe(x, 0)));

			//mve = new CausalMultiVE(builder.getSelectedPoints());
			//System.out.println(mve.query(y, ObservationBuilder.observe(x, 0)));


			// Update Us



			StructuralCausalModel posterior = builder.getSelectedPoints().get(0);

			for (int v : prior.getExogenousParents(x,y)) {
				prior.setFactor(v, posterior.getFactor(v));
				//System.out.println(v);
				//System.out.println(posterior.getFactor(v));
			}


		}


	}
}
