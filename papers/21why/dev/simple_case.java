package dev;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.CredalVariableElimination;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.util.stream.Stream;

public class simple_case {

	public static void main(String[] args) throws InterruptedException {

		// Case (c)
		BayesianNetwork bnet = new BayesianNetwork();

		int x = bnet.addVariable(2); // no degree - with degree
		int y = bnet.addVariable(2); // low income - high income
		int s = bnet.addVariable(2);	// not selected - selected

	/*
			Fig. 1c

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
			f([2, 0])
			-------------------------------
			f({2=0, 0=0}) = 0.6
			f({2=1, 0=0}) = 0.4
			f({2=0, 0=1}) = 0.2
			f({2=1, 0=1}) = 0.8
			-------------------------------





	 */

		bnet.addParents(s,x);
		bnet.addParents(y,x);

		BayesianFactor px = new BayesianFactor(bnet.getDomain(x), new double[]{0.4, 0.6});
		BayesianFactor py_x = new BayesianFactor(bnet.getDomain(y, x), new double[]{0.8, 0.2, 0.3, 0.7});
		BayesianFactor ps_x = new BayesianFactor(bnet.getDomain(s, x), new double[]{0.6, 0.4, 0.2, 0.8});


		bnet.setFactor(x, px);
		bnet.setFactor(y, py_x);
		bnet.setFactor(s, ps_x);

		for(int v: bnet.getVariables())
			FactorUtil.print(bnet.getFactor(v));


		// sample and drop S=0
		RandomUtil.setRandomSeed(1);
		TIntIntMap[] data = bnet.samples(500, bnet.getVariables());
		int finalS = s;
		data = Stream.of(data).filter(d -> d.get(finalS)==1 ).toArray(TIntIntMap[]::new);



		// causal model

		StructuralCausalModel prior = CausalBuilder.of(bnet).setFillRandomExogenousFactors(2).build();

		EMCredalBuilder builder = EMCredalBuilder.of(prior, data)
				.setNumTrajectories(10)
				.setBuildCredalModel(true)
				.build();

		SparseModel vmodel = builder.getModel();

		int us = vmodel.getParents(s)[1];
		VertexFactor vfs = ((VertexFactor)vmodel.getFactor(us)).combine((VertexFactor) vmodel.getFactor(s)).marginalize(us);

		FactorUtil.print(ps_x);
		FactorUtil.print(vfs.sampleVertex());

		System.out.println("Results:");


		// queries
		// p(x|y)
		VariableElimination ve = new FactorVariableElimination(bnet.getVariables());
		ve.setFactors(bnet.getFactors());
		ve.setEvidence(ObservationBuilder.observe(y, 0));
		System.out.println(ve.run(x));	// [0.6400000000000001, 0.36]


		CredalVariableElimination cve = new CredalVariableElimination(vmodel);
		System.out.println(cve.query(x, ObservationBuilder.observe(y, 0)));	//  [0.4864864766190158, 0.5135135233809842]... nothing to do

		// p(y|x)
		ve = new FactorVariableElimination(bnet.getVariables());
		ve.setFactors(bnet.getFactors());
		ve.setEvidence(ObservationBuilder.observe(x, 0));
		System.out.println(ve.run(y));	// [0.8, 0.2]


		cve = new CredalVariableElimination(vmodel);
		System.out.println(cve.query(y, ObservationBuilder.observe(x, 0)));	//  [0.8674698555490101, 0.13253014445098982]

/////

		System.out.println("----");

		// Case (d)
		bnet = new BayesianNetwork();

		x = bnet.addVariable(2); // no degree - with degree
		y = bnet.addVariable(2); // low income - high income
		s = bnet.addVariable(2);	// not selected - selected

		bnet.addParents(s,y);
		bnet.addParents(y,x);

		px = new BayesianFactor(bnet.getDomain(x), new double[]{0.4, 0.6});
		py_x = new BayesianFactor(bnet.getDomain(y, x), new double[]{0.8, 0.2, 0.3, 0.7});
		BayesianFactor ps_y = new BayesianFactor(bnet.getDomain(s, y), new double[]{0.6, 0.4, 0.2, 0.8});


		bnet.setFactor(x, px);
		bnet.setFactor(y, py_x);
		bnet.setFactor(s, ps_y);



		for(int v: bnet.getVariables())
			FactorUtil.print(bnet.getFactor(v));

	/*
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
	f([2, 1])
	-------------------------------
	f({2=0, 1=0}) = 0.6
	f({2=1, 1=0}) = 0.4
	f({2=0, 1=1}) = 0.2
	f({2=1, 1=1}) = 0.8
	-------------------------------


	 */



		// sample and drop S=0
		RandomUtil.setRandomSeed(1);
		data = bnet.samples(500, bnet.getVariables());
		int finalS1 = s;
		data = Stream.of(data).filter(d -> d.get(finalS1)==1 ).toArray(TIntIntMap[]::new);


		// causal model

		prior = CausalBuilder.of(bnet).setFillRandomExogenousFactors(2).build();

		builder = EMCredalBuilder.of(prior, data)
				.setNumTrajectories(10)
				.setBuildCredalModel(true)
				.build();

		vmodel = builder.getModel();


		System.out.println("Results");

		// queries
		//p(x|y)
		ve = new FactorVariableElimination(bnet.getVariables());
		ve.setFactors(bnet.getFactors());
		ve.setEvidence(ObservationBuilder.observe(y, 0));
		System.out.println(ve.run(x));	// [0.6400000000000001, 0.36]


		cve = new CredalVariableElimination(vmodel);
		System.out.println(cve.query(x, ObservationBuilder.observe(y, 0)));	//  [0.666666660457089, 0.33333333954291106]... similar



		// p(y|x)
		ve = new FactorVariableElimination(bnet.getVariables());
		ve.setFactors(bnet.getFactors());
		ve.setEvidence(ObservationBuilder.observe(x, 0));
		System.out.println(ve.run(y));	// [0.8, 0.2]


		cve = new CredalVariableElimination(vmodel);
		System.out.println(cve.query(y, ObservationBuilder.observe(x, 0)));	//  [0.7272727442382917, 0.2727272557617083]... similar

		System.out.println("----");

	/*

	 Structure Fig 1.c

	 - Queries in original sampling BN:
	 		P(X | Y=0) = [0.6400000000000001, 0.36]
	 		P(Y | X=0) =  [0.8, 0.2]

	- Queries using EM:
			P(X | Y=0) =  [0.4864864766190158, 0.5135135233809842]... nothing to do
	 		P(Y | X=0) =  [0.8674698555490101, 0.13253014445098982] ... similar


	 Structure Fig 1.d


	 - Queries in original sampling BN:
	 		P(X | Y=0) = [0.6400000000000001, 0.36]
	 		P(Y | X=0) =  [0.8, 0.2]

	- Queries using EM:
			P(X | Y=0) =  [0.666666660457089, 0.33333333954291106]... similar
	 		P(Y | X=0) = //  [0.7272727442382917, 0.2727272557617083]... similar


	Note: with the EM I always get a tiny interval, though here I give you just one of the extreme points.

	 */

	}

}
