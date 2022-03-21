package dev;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.experiments.Watch;
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

public class EMwithMissingXY {
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
f({2=0, 1=0, 0=0}) = 0.0     X = Y = 0  => S = 1
f({2=1, 1=0, 0=0}) = 1.0

f({2=0, 1=0, 0=1}) = 1.0	X != Y 	    => S = 0
f({2=1, 1=0, 0=1}) = 0.0

f({2=0, 1=1, 0=0}) = 1.0	X != Y      => S = 0
f({2=1, 1=1, 0=0}) = 0.0

f({2=0, 1=1, 0=1}) = 0.0	X = Y = 1  => S = 1
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

		data =
				Stream.of(data).map(d -> {
					if(d.get(s)==0){
						d.remove(x);
						d.remove(y);
					}
					return d;
				}).toArray(TIntIntMap[]::new);

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



		RandomUtil.setRandomSeed(1);
		Watch.start();
		builder = EMCredalBuilder.of(prior, data)
				.setNumTrajectories(100)
				.setBuildCredalModel(false)
				.setWeightedEM(true)
				//.setSelPolicy(EMCredalBuilder.SelectionPolicy.BISECTION_BORDER_SAME_PATH)
				.build();

		Watch.stopAndPrint();
		System.out.println("finished learning");




		///// Exact interval calculus
		BayesianFactor joint = BayesianFactor.combineAll(bnet.getFactors(x,y,s));
		BayesianFactor ps = joint.marginalize(x,y);
		BayesianFactor pxy_s = joint.divide(ps);
		BayesianFactor px_s = joint.marginalize(y).divide(ps);

		//BayesianFactor py_x = joint.marginalize(s).divide(joint.marginalize(s,y));
		System.out.println("Results");

		System.out.println("\nQuery in complete BN:");
		System.out.println("p(y=0|x=0): "+py_x.filter(x,0).filter(y,0));
		System.out.println("p(y=0|x=1): "+py_x.filter(x,1).filter(y,0));

		System.out.println("\n\nexact interval computation:");




		double a,b,c,d;
		double l, u;
		int valx, valy;

		valx = 0;
		valy= 0;
		a = pxy_s.filter(x,valx).filter(y,valy).filter(s,1).getValueAt(0);
		b = ps.filter(s,1).getValueAt(0);
		c = px_s.filter(x,valx).filter(s,1).getValueAt(0);
		d = ps.filter(s,0).getValueAt(0);
		l = (a * b) / (c*b + d);
		u = (a * b + d) / (c*b + d);
		System.out.println("p(y="+valy+"|x="+valx+") in ["+l+","+u+"]");

		valx = 1;
		valy= 0;
		a = pxy_s.filter(x,valx).filter(y,valy).filter(s,1).getValueAt(0);
		b = ps.filter(s,1).getValueAt(0);
		c = px_s.filter(x,valx).filter(s,1).getValueAt(0);
		d = ps.filter(s,0).getValueAt(0);
		l = (a * b) / (c*b + d);
		u = (a * b + d) / (c*b + d);
		System.out.println("p(y="+valy+"|x="+valx+") in ["+l+","+u+"]");



		/////




		System.out.println("\n\nEMCC results)");

		mve = new CausalMultiVE(builder.getSelectedPoints());
		valy = 0;
		valx = 0;
		System.out.println("p(y="+valy+"|x="+valx+")");
		System.out.println(
				((VertexFactor)mve.query(y, ObservationBuilder.observe(x, valx))).filter(y,valy)
		);

		valy = 0;
		valx = 1;
		System.out.println("p(y="+valy+"|x="+valx+")");
		System.out.println(
				((VertexFactor)mve.query(y, ObservationBuilder.observe(x, valx))).filter(y,valy)
		);


	}
}
