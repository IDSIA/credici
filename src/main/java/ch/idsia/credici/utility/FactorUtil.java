package ch.idsia.credici.utility;

import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FactorUtil {

	public static VertexFactor mergeFactors(List<BayesianFactor> factors, int leftvar, boolean applyConvexhull){
		VertexFactor vf = VertexFactor.mergeVertices(
				factors.stream().map(f -> new BayesianToVertex().apply(f, leftvar)).toArray(VertexFactor[]::new)
		);
		if(applyConvexhull)
			vf = vf.convexHull(true);

		return vf;
	}

	public static HashMap<Set<Integer>, BayesianFactor> fixEmpiricalMap(
			HashMap<Set<Integer>,
			BayesianFactor> emp, int numDecimals){

		HashMap out = new HashMap();

		for(Set<Integer> s : emp.keySet()){
			out.put(s, FactorUtil.fixPrecission(((BayesianFactor)emp.get(s)), numDecimals, false, Ints.toArray(s)));
		}
		return out;

	}

	public static int EmpiricalMapSize(HashMap<Set<Integer>, BayesianFactor> map){
		return map.values().stream().mapToInt(p -> p.getData().length).sum();
	}

	public static GenericFactor filter(GenericFactor f, int var, int state) {
		GenericFactor res = null;

		if (f instanceof VertexFactor)
			res = ((VertexFactor) f).filter(var, state);
		else if (f instanceof BayesianFactor)
			res = ((BayesianFactor) f).filter(var, state);

		else if (f instanceof IntervalFactor)
			res = ((IntervalFactor) f).filter(var, state);
		else
			throw new IllegalStateException("Cannot cast result");

		return res;
	}

	public static void print(BayesianFactor p) { print(p, new HashMap());}

	public static void print(BayesianFactor p, HashMap varNames){//, int[] vars){


		Strides dom = p.getDomain();
		int[] vars = dom.getVariables();

		//IndexIterator it = dom.getReorderedIterator(vars);

		System.out.println("f("+ Arrays.toString(vars)+")");
		System.out.println("-------------------------------");
		for (int i = 0; i < dom.getCombinations(); i++) {
			//while (it.hasNext()){
			//	int i = it.next();
			int[] states = dom.statesOf(i);
			TIntIntMap assignament = new TIntIntHashMap();

			String strAssig = "";
			for (int j = vars.length-1; j >= 0; j--) {
				assignament.put(vars[j], states[j]);

				strAssig +=varNames.getOrDefault(vars[j], vars[j])+"="+states[j];
				if(j>0)
					strAssig+=", ";
			}

			System.out.println("f("+strAssig+") = " + +p.getValue(states));
		}
		System.out.println("-------------------------------");



	}
/*

	public static void print(BayesianFactor p){
		print(p, p.getDomain().getVariables());
	}
*/

	public static BayesianFactor  fixPrecission(BayesianFactor f, int num_decimals, boolean newZeros, int... left_vars){

		Strides left = f.getDomain().intersection(left_vars);
		Strides right = f.getDomain().remove(left);
		BayesianFactor newFactor = f.reorderDomain(left.concat(right));
		double[][] newData = new double[right.getCombinations()][left.getCombinations()];
		double[][] oldData = ArraysUtil.reshape2d(newFactor.getData(), right.getCombinations());

		for(int i=0; i<right.getCombinations(); i++){
			newData[i] = CollectionTools.roundNonZerosToTarget(oldData[i], 1.0, newZeros, num_decimals);
		}

		newFactor.setData(Doubles.concat(newData));
		return newFactor.reorderDomain(f.getDomain());

	}


	public static int getOffset(BayesianFactor f, TIntIntMap conf){
		if(!ArraysUtil.equals(f.getDomain().getVariables(), conf.keys(), true, true))
			throw new IllegalArgumentException("Incompatible domains");

		int[] states = IntStream.of(f.getDomain().getVariables()).map(x -> conf.get(x)).toArray();
		return f.getDomain().getOffset(states);
	}


}
