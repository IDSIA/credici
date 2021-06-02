package edu.neurips.causalem.utility;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Strides;
import com.google.common.primitives.Doubles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

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


	public static double likelihood(HashMap<Set<Integer>, BayesianFactor> prob,
										 HashMap<Set<Integer>, BayesianFactor> emp, int counts) {
		double L = 1.0;
		for(Set<Integer> k : emp.keySet())
			L *= Probability.likelihood((BayesianFactor) prob.get(k), (BayesianFactor)emp.get(k), counts);

		return L;
	}


	public static double logLikelihood(HashMap<Set<Integer>, BayesianFactor> prob,
										 HashMap<Set<Integer>, BayesianFactor> emp, int counts) {
		double l = 0.0;
		for(Set<Integer> k : emp.keySet())
			l += Probability.logLikelihood((BayesianFactor) prob.get(k), (BayesianFactor)emp.get(k), counts);

		return l;
	}

	public static double ratioLikelihood(BayesianFactor prob, BayesianFactor emp, int counts) {
		return logLikelihood(prob, emp, counts)/logLikelihood(emp, emp, counts);
	}

	public static double ratioLikelihood(HashMap<Set<Integer>, BayesianFactor> prob,
									HashMap<Set<Integer>, BayesianFactor> emp, int counts) {
		return likelihood(prob, emp, counts)/likelihood(emp, emp, counts);
	}

	public static double ratioLogLikelihood(BayesianFactor prob, BayesianFactor emp, int counts) {
		return logLikelihood(emp, emp, counts)/logLikelihood(prob, emp, counts);
	}

	public static double ratioLogLikelihood(HashMap<Set<Integer>, BayesianFactor> prob,
										 HashMap<Set<Integer>, BayesianFactor> emp, int counts) {
		return logLikelihood(emp, emp, counts)/logLikelihood(prob, emp, counts);
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


	public static double KLsymmetrized(BayesianFactor p, BayesianFactor q, boolean... zeroSafe){
		return KLsymmetrized(p.getData(), q.getData(), zeroSafe);
	}

	public static double KL(BayesianFactor p, BayesianFactor q, boolean... zeroSafe){
		return KL(p.getData(), q.getData(), zeroSafe);
	}


	public static double KLsymmetrized(HashMap<Set<Integer>, BayesianFactor> p,
									   HashMap<Set<Integer>, BayesianFactor> q, boolean... zeroSafe) {
		double l = 0.0;
		for(Set<Integer> k : q.keySet())
			l += Probability.KLsymmetrized((BayesianFactor) p.get(k), (BayesianFactor)q.get(k), zeroSafe);

		return l;
	}

	public static double KL(HashMap<Set<Integer>, BayesianFactor> p,
							HashMap<Set<Integer>, BayesianFactor> q, boolean... zeroSafe) {
		double l = 0.0;
		for(Set<Integer> k : q.keySet())
			l += Probability.KL((BayesianFactor) p.get(k), (BayesianFactor)q.get(k), zeroSafe);

		return l;
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




}
