package ch.idsia.credici.model.eqmc;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.util.MathArrays;

import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.learning.eqem.EQEMLearner;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.transform.EmpiricalNetwork;
import ch.idsia.credici.utility.Randomizer;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class TestEQMCLearner {
//	v1=bn.add(gum.LabelizedVariable('v1','v1',2))
//			v2=bn.add(gum.LabelizedVariable('v2','v2',2))
//			# Exogenous variables
//			u1=bn.add(gum.LabelizedVariable('u1','u1',2))
//			u2=bn.add(gum.LabelizedVariable('u2','u2',4))
//			# Dummy children for joint queries
//			a=bn.add(gum.LabelizedVariable('a','a',4))
//			# SCM Arcs
//			bn.addArc(v1,v2)
//			bn.addArc(u1,v1)
//			bn.addArc(u2,v2)
//			# Dummy children arcs
//			bn.addArc(v1,a)
//			bn.addArc(v2,a)
	
	
	
//			bn.cpt("a")[{'v1': 0, 'v2': 0}] = [1, 0, 0, 0]
//			bn.cpt("a")[{'v1': 0, 'v2': 1}] = [0, 1, 0, 0]
//			bn.cpt("a")[{'v1': 1, 'v2': 0}] = [0, 0, 1, 0]
//			bn.cpt("a")[{'v1': 1, 'v2': 1}] = [0, 0, 0, 1]
	public static void main1(String[] args) throws InterruptedException {
		StructuralCausalModel model = new StructuralCausalModel("Anto");	
		int v1 = model.addVariable(0, 2);
		int v2 = model.addVariable(1, 2);
		int u1 = model.addVariable(2, 2, true);
		int u2 = model.addVariable(3, 4, true);
//		int a = model.addVariable(5,4);
		
		model.addParent(v2, v1);
		model.addParent(v1, u1);
		model.addParent(v2, u2);
		
//		model.addParent(a, v1);
//		model.addParent(a, v2);
		
//		BayesianFactor a_factor = new BayesianFactor(model.getDomain(v1, v2, a));
//		a_factor.setData(new int[] { a, v2, v1 }, new double[] {
//			1, 0, 0, 0, // v1 = 0, v2 = 0 
//			0, 1, 0, 0, // v1 = 0, v2 = 1
//			0, 0, 1, 0, // v1 = 1, v2 = 0
//			0, 0, 0, 1  // v1 = 1, v2 = 1
//		});
//		model.setFactor(a, a_factor);
		
		
		Randomizer r = new Randomizer();
//		r.randomizeInplace(a_factor, a);
		
		var v1_factor = new BayesianFactor(model.getDomain(v1,u1));
		v1_factor.setData(new double[] {1,0, 0,1 });
		model.setFactor(v1, v1_factor);
		
		var v2_factor = new BayesianFactor(model.getDomain(v1, v2, u2));
		v2_factor.setData(new double[] { 1, 1, 0, 0, // u=0
										 1, 0, 0, 1, // u=1
										 0, 1, 1, 0, // u=2
										 0, 0, 1, 1  // u=3
		});
		model.setFactor(v2, v2_factor);
		
		var u1_factor = new BayesianFactor(model.getDomain(u1));
		//r.randomFactor(model.getDomain(u1), u1, true);
		u1_factor.setData(new double[] {0.5, 0.5});
		model.setFactor(u1, u1_factor);
		
		var u2_factor = new BayesianFactor(model.getDomain(u2));//r.randomFactor(model.getDomain(u2), u2, true);
		u2_factor.setData(new double[] {0.5, 0.5});
		model.setFactor(u2, u2_factor);
		
		DoubleTable datatable = new DoubleTable(new int[] { v1, v2 });
		datatable.add(new int[] { 0, 0 }, 100.0);
		datatable.add(new int[] { 0, 1 }, 20.0);
		datatable.add(new int[] { 1, 0 }, 30.0);
		datatable.add(new int[] { 1, 1 }, 400.0);

		CredalCausalVE ccve = new CredalCausalVE(model, datatable.toMap(false), u1, u2);
		System.out.print("CCVE: ");
		var pns = ccve.probNecessityAndSufficiency(v1, v2);
		for (double[] v : pns.getData()[0]) {
			System.out.print(v[0] + ",");
		}
		
		System.out.println("");

		long time = System.nanoTime();
//		TIntIntMap[] data = model.samples(5000, v1,v2);
//		DoubleTable datatable2 = new DoubleTable(data);
//		time = System.nanoTime() - time;
//		System.out.println("D1:" + time);
		
		
//		time = System.nanoTime();
//		Sampler s = new Sampler();
//		DoubleTable datatable = s.sample(model, 5000, v1, v2);//new DoubleTable(data);
//		time = System.nanoTime() - time;
//		System.out.println("D2:"+time);
//		
//
		EmpiricalNetwork en = new EmpiricalNetwork();
		BayesianNetwork network = en.apply(model, datatable);
		
		DetailedDotSerializer.saveModel("./run/emp.png", new Info().model(network).data(datatable).title("empirical Model"));		
		double ll = en.loglikelihood(network, datatable);
		System.out.println("LL: "+ll);
		
		
		TIntIntMap sizes = new TIntIntHashMap();
		sizes.put(u1, 2);
		sizes.put(u2, 4);

		Config settings = new Config(10000, 1000, 0, 1000, false);
		double[] free = pns(model, settings, v1, v2, datatable, sizes);
		 settings = new Config(100, 1000, 50, 1000, true);
		double[] determinisitc = pns(model, settings, v1, v2, datatable, sizes);
//		
		
		System.out.println("PNS free: " + Arrays.toString(minmax(free)));
		System.out.println("PNS dete: " + Arrays.toString(minmax(determinisitc)));
	}
	
	static double[] minmax(double[] data) {
		double min = data[0];
		double max = data[0];
		
		for (int v = 1; v < data.length; ++v) {
			double val = data[v];
			if (val < min) min = val;
			else if (val > max) max = val;
		}
		return new double[] { min, max};
	}
	
	private static double[] pns(StructuralCausalModel model, Config c, int v1, int v2, DoubleTable datatable, TIntIntMap sizes) throws InterruptedException {
		EQEMLearner learner = new EQEMLearner(model, datatable, sizes, false);
		
		learner.setConfig(c);
		
		//learner.setDebugLoggerGenerator(new PDFLoggerGenerator("./run"));
		var cc = learner.run();
		long before = cc.combinations();
		cc.simplify();
		long after = cc.combinations();
		System.out.println(before + " -> " + after);

		var solutions = (after < 40000) ? cc.exaustiveIterator() :  cc.sobolIterator();
		
		TDoubleList pnss = new TDoubleArrayList(100);
		StructuralCausalModel solution = null; 
		for (int i = 0; i < 75000 && solutions.hasNext(); ++i) {
			solution = solutions.next();
			CausalVE cve = new CausalVE(solution);
			var pns = cve.probNecessityAndSufficiency(v1, v2);
			double pnsval = pns.getData()[0];
			pnss.add(pnsval);	
		}
		DetailedDotSerializer.saveModel("./run/solution.png", new Info().model(solution).data(datatable));
		return pnss.toArray();
	}

	public static void main(String[] args) throws InterruptedException {
		StructuralCausalModel model = new StructuralCausalModel("Anto");	
		int v1 = model.addVariable(0, 2);
		int v2 = model.addVariable(1, 2);
		int v3 = model.addVariable(2, 2);
		int u1 = model.addVariable(3, 4, true);
		int u2 = model.addVariable(4, 4, true);

		model.addParent(v3, v2);
		model.addParent(v2, v1);
		model.addParent(v1, u1);
		model.addParent(v2, u2);
		model.addParent(v2, u2);
		model.addParent(v3, u2);

		
//		model.addParent(a, v1);
//		model.addParent(a, v2);
		
//		BayesianFactor a_factor = new BayesianFactor(model.getDomain(v1, v2, a));
//		a_factor.setData(new int[] { a, v2, v1 }, new double[] {
//			1, 0, 0, 0, // v1 = 0, v2 = 0 
//			0, 1, 0, 0, // v1 = 0, v2 = 1
//			0, 0, 1, 0, // v1 = 1, v2 = 0
//			0, 0, 0, 1  // v1 = 1, v2 = 1
//		});
//		model.setFactor(a, a_factor);
		
		
		Randomizer r = new Randomizer();
//		r.randomizeInplace(a_factor, a);
		
		var v1_factor = new BayesianFactor(model.getDomain(v1,u1));
		v1_factor.setData(new double[] {1,0, // u = 0
										0,1, // u = 1
										1,0, // u = 2
										0,1  // u = 3
										});
		model.setFactor(v1, v1_factor);
		
		var v2_factor = new BayesianFactor(model.getDomain(v1, v2, u1, u2));
		
		// v2 | v1, u1, u2
		// m(v1->v2) | u1, u2
		// m(v1_u1 -> v2) | u1, u2
		
		
		v2_factor.setData(new double[] { 
				// v2|v1 seen as v1,v2 i.e. v1=0v2=0, v1=1v2=0, v1=0v2=1, v1=1v2=1
			//  v2=0, v2=1
				1, 1, 0, 0, // u1=0
				1, 1, 0, 0, // u1=1
				1, 0, 0, 1, // u1=2
				1, 0, 0, 1, // u1=3
				
				1, 0, 0, 1, // u1=0
				1, 0, 0, 1, // u1=1
				1, 1, 0, 0, // u1=2
				1, 1, 0, 0, // u1=3
				
				0, 1, 1, 0, // u1=0
				0, 1, 1, 0, // u1=1
				0, 0, 1, 1, // u1=2
				0, 0, 1, 1,  // u1=3
				
				0, 0, 1, 1, // u1=0
				0, 0, 1, 1, // u1=1
				0, 1, 1, 0, // u1=2
				0, 1, 1, 0, // u1=3
		});
		
		model.setFactor(v2, v2_factor);
		
		var v3_factor = new BayesianFactor(model.getDomain(v2, v3, u2));
		v3_factor.setData(new double[] { 
			//	v3=0, v3=1
				1, 1, 0, 0, // u2=0
				1, 0, 0, 1, // u2=1
				0, 1, 1, 0, // u2=2
				0, 0, 1, 1  // u2=3
		});
		
		var u1_factor = new BayesianFactor(model.getDomain(u1));
		//r.randomFactor(model.getDomain(u1), u1, true);
		u1_factor.setData(new double[] {0.25,0.25,0.25,0.25});
		model.setFactor(u1, u1_factor);
		
		var u2_factor = new BayesianFactor(model.getDomain(u2));//r.randomFactor(model.getDomain(u2), u2, true);
		u2_factor.setData(new double[] {0.25,0.25,0.25,0.25});
		model.setFactor(u2, u2_factor);
		
		DoubleTable datatable = new DoubleTable(new int[] { v1, v2, v3 });
		datatable.add(new int[] { 0, 0, 0 }, 90.0);
		datatable.add(new int[] { 0, 1, 0 }, 90.0);
		datatable.add(new int[] { 1, 0, 0 }, 90.0);
		datatable.add(new int[] { 1, 1, 0 }, 90.0);
		datatable.add(new int[] { 0, 0, 1 }, 90.0);
		datatable.add(new int[] { 0, 1, 1 }, 90.0);
		datatable.add(new int[] { 1, 0, 1 }, 90.0);
		datatable.add(new int[] { 1, 1, 1 }, 80.0);
		
		System.out.println("");

		long time = System.nanoTime();


//		BayesianNetwork en2 = model.getEmpiricalNet(datatable.toMap(false));
//		double[] xx = en2.logProb(datatable.toMap(false));
//		double ll = Arrays.stream(xx).sum();
		
		EmpiricalNetwork en = new EmpiricalNetwork();
		BayesianNetwork network = en.apply(model, datatable);
		
		DetailedDotSerializer.saveModel("./run/emp.png", new Info().model(network).data(datatable).title("empirical Model"));		
		double ll = en.loglikelihood(network, datatable);
		System.out.println("LL: " + ll);
		
		
		TIntIntMap sizes = new TIntIntHashMap();
		sizes.put(u1, 2);
		sizes.put(u2, 4);

		Config settings = new Config(100, 500, 0, 1000, false);
		double[] free = pns(model, settings, v1, v3, datatable, sizes);
		
//		Config settings = new Config(20, 1000, 0, 1000, true);
		double[] determinisitc = pns(model, settings, v1, v3, datatable, sizes);
//		
		
//		System.out.println("PNS free: " + Arrays.toString(minmax(free)));
		System.out.println("PNS dete: " + Arrays.toString(minmax(determinisitc)));
	}
}