package ch.idsia.credici.utility;

import ch.idsia.crema.core.Strides;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.linear.interval.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.factor.credal.vertex.separate.VertexFactorUtilities;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.hull.ConvexHull;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.*;
import java.util.stream.DoubleStream;

public class FactorUtil {

	public static VertexFactor mergeFactors(List<BayesianFactor> factors, int leftvar, boolean applyConvexhull){
		VertexFactor vf = VertexFactorUtilities.mergeVertices(
				factors.stream().map(f -> new BayesianToVertex().apply(f, leftvar)).toArray(VertexFactor[]::new)
		);
		if(applyConvexhull)
			vf = vf.convexHull(ConvexHull.DEFAULT);

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

	public static void print(BayesianFactor p){//, int[] vars){

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
			for (int j = 0; j < vars.length; j++)
				assignament.put(vars[j], states[j]);
			System.out.println("f("+assignament+") = " + +p.getValue(states));
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

		BayesianFactorFactory ff = BayesianFactorFactory.factory().domain(left.concat(right));

		double[][] newData = new double[right.getCombinations()][left.getCombinations()];
		double[][] oldData = ArraysUtil.reshape2d(f.reorderDomain(left.concat(right)).getData(), right.getCombinations());

		for(int i=0; i<right.getCombinations(); i++){
			newData[i] = CollectionTools.roundNonZerosToTarget(oldData[i], 1.0, newZeros, num_decimals);
		}

		ff = ff.data(Doubles.concat(newData));
		return ff.get().reorderDomain(f.getDomain());

	}


	public static boolean isDeterministic(BayesianFactor f, int... given){

		if(!DoubleStream.of(f.getData()).allMatch(x -> x==0.0 || x==1.0))
			return false;

		int[] left = ArraysUtil.difference(f.getDomain().getVariables(), given);

		for(int v: left){
			f = f.marginalize(v);
		}

		if(!DoubleStream.of(f.getData()).allMatch(x->x==1.0))
			return false;
		return true;

	}


}
