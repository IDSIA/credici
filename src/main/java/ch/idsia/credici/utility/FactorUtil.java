package ch.idsia.credici.utility;

import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Strides;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
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
			out.put(s, ((BayesianFactor)emp.get(s)).fixPrecission(numDecimals, Ints.toArray(s)));
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

	public static void print(BayesianFactor p){

		Strides dom = p.getDomain();
		int[] vars = dom.getVariables();

		System.out.println("f("+ Arrays.toString(vars)+")");
		System.out.println("-------------------------------");
		for (int i = 0; i < dom.getCombinations(); i++) {
			int[] states = dom.statesOf(i);
			TIntIntMap assignament = new TIntIntHashMap();
			for (int j = 0; j < vars.length; j++)
				assignament.put(vars[j], states[j]);
			System.out.println("f("+assignament+") = " + +p.getValue(states));
		}
		System.out.println("-------------------------------");


	}



}
