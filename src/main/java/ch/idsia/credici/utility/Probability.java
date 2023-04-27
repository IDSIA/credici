package ch.idsia.credici.utility;

import ch.idsia.credici.IO;
import ch.idsia.credici.learning.ve.VE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.Factor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.user.core.Variable;

import com.google.common.primitives.Doubles;
import com.opencsv.exceptions.CsvException;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class Probability {


	public static double likelihood(BayesianFactor prob, BayesianFactor emp, int counts) {
		if(!compareDomains(prob.getDomain(), emp.getDomain()))
			throw new IllegalArgumentException("Wrong domains");
		double L = 1.0;
		for (int i = 0; i < prob.getDomain().getCombinations(); i++)
			L *= Math.pow(prob.getValueAt(i), counts * emp.getValueAt(i));
		return L;
	}

	public static double logLikelihood(BayesianFactor prob, BayesianFactor emp, int counts) {
		if(!compareDomains(prob.getDomain(), emp.getDomain()))
			throw new IllegalArgumentException("Wrong domains");
		double l = 0.0;
		for(int i=0; i<prob.getDomain().getCombinations(); i++) {
			if(prob.getValueAt(i) == 0){
				if(emp.getValueAt(i) != 0)
					l += Double.NEGATIVE_INFINITY;
			}else {
				l += counts * emp.getValueAt(i) * Math.log(prob.getValueAt(i));
			}
		}
		return l;
	}

	public static double logLikelihood(BayesianFactor prob, TIntIntMap[] data) {
		Strides dom = prob.getDomain();
		TIntIntMap[] D =  DataUtil.selectColumns(data, dom.getVariables());
		BayesianFactor counts = DataUtil.getCounts(D, dom);

		double llk = 0;

		for(int i=0; i<dom.getCombinations(); i++) {
			int s[] = dom.statesOf(i);
			double c = counts.getValue(s);
			if(c>0)
				llk += c * Math.log(prob.getValue(s));
		}
		return llk;
	}


	public static double likelihood(Map<Set<Integer>, BayesianFactor> prob,
									Map<Set<Integer>, BayesianFactor> emp, int counts) {
		double L = 1.0;
		for(Set<Integer> k : emp.keySet())
			L *= Probability.likelihood((BayesianFactor) prob.get(k), (BayesianFactor)emp.get(k), counts);

		return L;
	}


	public static double likelihood(TIntObjectMap<BayesianFactor> prob,
									TIntObjectMap<BayesianFactor> emp, int counts) {
		return likelihood(FactorUtil.intMapToHashMap(prob), FactorUtil.intMapToHashMap(emp), counts);

	}





	public static double logLikelihood(Map<Set<Integer>, BayesianFactor> prob,
									  	Map<Set<Integer>, BayesianFactor> emp, int counts) {
		double l = 0.0;
		for(Set<Integer> k : emp.keySet())
			l += Probability.logLikelihood((BayesianFactor) prob.get(k), (BayesianFactor)emp.get(k), counts);

		return l;
	}

	public static double logLikelihood(TIntObjectMap<BayesianFactor> prob,
									TIntObjectMap<BayesianFactor> emp, int counts) {
		return logLikelihood(FactorUtil.intMapToHashMap(prob), FactorUtil.intMapToHashMap(emp), counts);

	}



	public static double logLikelihood(Map<Set<Integer>, BayesianFactor> prob,
									   TIntIntMap[] data) {
		double l = 0.0;
		for(Set<Integer> k : prob.keySet())
			l += Probability.logLikelihood((BayesianFactor) prob.get(k), data);

		return l;
	}

	public static double logLikelihood(TIntObjectMap<BayesianFactor> prob,
									   TIntIntMap[] data) {
		return logLikelihood(FactorUtil.intMapToHashMap(prob), data);

	}

	public static double ratioLikelihood(BayesianFactor prob, BayesianFactor emp, int counts) {
		return logLikelihood(prob, emp, counts)/logLikelihood(emp, emp, counts);
	}

	public static double ratioLikelihood(Map<Set<Integer>, BayesianFactor> prob,
									Map<Set<Integer>, BayesianFactor> emp, int counts) {
		return likelihood(prob, emp, counts)/likelihood(emp, emp, counts);
	}

	public static double ratioLikelihood(TIntObjectMap<BayesianFactor> prob,
									   TIntObjectMap<BayesianFactor> emp, int counts) {
		return ratioLikelihood(FactorUtil.intMapToHashMap(prob), FactorUtil.intMapToHashMap(emp), counts);

	}


	public static double ratioLogLikelihood(BayesianFactor prob, BayesianFactor emp, int counts) {
		return logLikelihood(emp, emp, counts)/logLikelihood(prob, emp, counts);
	}

	public static double ratioLogLikelihood(Map<Set<Integer>, BayesianFactor> prob,
										 	Map<Set<Integer>, BayesianFactor> emp, int counts) {
		return logLikelihood(emp, emp, counts)/logLikelihood(prob, emp, counts);
	}

	public static double ratioLogLikelihood(TIntObjectMap<BayesianFactor> prob,
									   TIntObjectMap<BayesianFactor> emp, int counts) {
		return ratioLogLikelihood(FactorUtil.intMapToHashMap(prob), FactorUtil.intMapToHashMap(emp), counts);

	}

	public static double maxLogLikelihood(Map<Set<Integer>, BayesianFactor> emp, int counts) {
		return logLikelihood(emp, emp, counts);
	}

	public static double maxLogLikelihood(TIntObjectMap<BayesianFactor> emp, int counts) {
		return maxLogLikelihood(FactorUtil.intMapToHashMap(emp), counts);
	}
	public static double maxLogLikelihood(StructuralCausalModel model, TIntIntMap[] data){
		TIntObjectMap<BayesianFactor> emp = DataUtil.getCFactorsSplittedMap(model, data);
		return Probability.maxLogLikelihood(emp, data.length);
	}



	private static boolean compareDomains(Strides dom1, Strides dom2){
		return Arrays.equals(dom1.getVariables(), dom2.getVariables()) &&
					Arrays.equals(dom1.getSizes(), dom2.getSizes());
	}



	// Compute the symmetrized KL distance between v1 and v2
	public static double KLsymmetrized(double[] p, double[] q, boolean... zeroSafe){
		return KL(p,q,zeroSafe) + KL(q,p,zeroSafe);
	}

	public static double KL(double[] p, double[] q, boolean... zeroSafe){

		if(zeroSafe.length>1) throw new IllegalArgumentException("Wrong number of arguments,");
		if(zeroSafe.length==0) zeroSafe = new boolean[]{false};
		if(p.length != q.length) throw new IllegalArgumentException("Arrays of different sizes.");

		double distance = 0;
		int n = p.length;
		for(int i=0; i<n; i++){

			// p and q is 0, distance is o
			if(p[i]!=0 || q[i]!=0) {
				if(p[i]==0 && q[i]>0) {
					if(!zeroSafe[0])
						distance += Double.POSITIVE_INFINITY;
				}
				else if(!(p[i]>0 && q[i]==0)) {	// otherwise is not defined
					distance += p[i] * (Math.log(p[i]) - Math.log(q[i]));
				}

			}


			if(!zeroSafe[0] || (p[i]!=0 && q[i]!=0)) {
			}
		}
		return distance;
	}

	public static double manhattanDist(double[] p, double[] q) {
		if(p.length != q.length) throw new IllegalArgumentException("Arrays of different sizes.");
		return IntStream.range(0, p.length).mapToDouble(i -> Math.abs(p[i] - q[i])).sum();
	}


		public static double KLsymmetrized(BayesianFactor p, BayesianFactor q, boolean... zeroSafe){
		return KLsymmetrized(p.getData(), q.getData(), zeroSafe);
	}

	public static double KL(BayesianFactor p, BayesianFactor q, boolean... zeroSafe){
		return KL(p.getData(), q.getData(), zeroSafe);
	}


	public static double KLsymmetrized(Map<Set<Integer>, BayesianFactor> p,
									   Map<Set<Integer>, BayesianFactor> q, boolean... zeroSafe) {
		double l = 0.0;
		for(Set<Integer> k : q.keySet())
			l += Probability.KLsymmetrized((BayesianFactor) p.get(k), (BayesianFactor)q.get(k), zeroSafe);

		return l;
	}

	public static double KL(Map<Set<Integer>, BayesianFactor> p,
							Map<Set<Integer>, BayesianFactor> q, boolean... zeroSafe) {
		double l = 0.0;
		for(Set<Integer> k : q.keySet())
			l += Probability.KL((BayesianFactor) p.get(k), (BayesianFactor)q.get(k), zeroSafe);

		return l;
	}

	public static double manhattanDist(BayesianFactor p, BayesianFactor q){
		return manhattanDist(p.getData(), q.getData());
	}


	public static boolean vertexInside(BayesianFactor f, VertexFactor vf){

		int[] leftVars = vf.getDataDomain().getVariables();
		if(leftVars.length>1)
			throw new IllegalArgumentException("No more than 1 variables in the data domain.");

		VertexFactor merged = vf.merge(new BayesianToVertex().apply(f, leftVars[0]));

		for(int i=0; i<vf.getSeparatingDomain().getCombinations(); i++){

			System.out.println(Doubles.concat(merged.getData()[i]));
			System.out.println(Doubles.concat(vf.getData()[i]));
			if(!Arrays.equals(
					Doubles.concat(merged.getData()[i]),
					Doubles.concat(vf.getData()[i])))
				return false;
		}
		return true;

	}

	/*

	List<StructuralCausalModel> t = em.getIntermediateModels();

Probability.vertexInside(t.get(31).getFactor(4), (VertexFactor) this.trueCredalModel.getFactor(4));
	 */


	public static <F extends GraphicalModel> double LL(F model, TIntIntMap[] data) throws InterruptedException {
		
		MinFillOrdering mfo = new MinFillOrdering();
		int[] order = mfo.apply(model);
		double ll = 0;
		for (TIntIntMap obs : data) {
			CutObserved co = new CutObserved();
			F net = co.execute(model, obs);
			
			RemoveBarren rm = new RemoveBarren();
			net = rm.execute(net, obs.keys(), obs);

			VE<BayesianFactor> ve = new VE<>(order);
			ve.setEvidence(obs);
			ve.setFactors(net.getFactors());
			ve.setNormalize(false); // we are interested in P(e)
			
			BayesianFactor bf = ve.apply(net, obs.keys(), obs);
			ll += Math.log(bf.getData()[0]);
		}
		return ll;
	}
}
