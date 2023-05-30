package ch.idsia.credici.utility;

import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToVertex;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FactorUtil {


	public static int DEFAULT_DECIMALS = 5;

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

		else if (f instanceof IntervalFactor) {
			res = filterInterval((IntervalFactor) f, var, state);
		}
		else
			throw new IllegalStateException("Cannot cast result");

		return res;
	}

	public static IntervalFactor filterInterval(IntervalFactor f, int var, int state){
		int[] dom = f.getDomain().getVariables();
		if(dom.length!=1 || dom[0]!=var)
		    return f.filter(var,state);
		IntervalFactor res = new IntervalFactor(Strides.empty(), Strides.empty());
		res.setLower(new double[]{f.getDataLower()[0][state]});
		res.setUpper(new double[]{f.getDataUpper()[0][state]});
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

		Strides left = f.getDomain().sort().intersection(left_vars);
		Strides right = f.getDomain().sort().remove(left);
		BayesianFactor newFactor = f.reorderDomain(left.concat(right));
		double[][] newData = new double[right.getCombinations()][left.getCombinations()];
		double[][] oldData = ArraysUtil.reshape2d(newFactor.getData(), right.getCombinations());

		for(int i=0; i<right.getCombinations(); i++){
			newData[i] = CollectionTools.roundNonZerosToTarget(oldData[i], 1.0, newZeros, num_decimals);
		}

		newFactor.setData(Doubles.concat(newData));
		return newFactor.reorderDomain(f.getDomain());

	}


	public static void printTab(BayesianFactor f, Strides iterDom, String sep){
		// Print equation

		IndexIterator it = iterDom.getIterator();
		int[] iterVars = iterDom.getVariables();

		int[] nonIterVars = ArraysUtil.difference(f.getDomain().getVariables(), iterVars);

		System.out.println(ArraysTools.toString(iterVars, sep)+sep+"\\"+sep+ArraysTools.toString(nonIterVars, sep));
		while(it.hasNext()){
			int idx = it.next();
			ObservationBuilder iterValues = ObservationBuilder.observe(iterVars, iterDom.statesOf(idx));

			System.out.print(ArraysTools.toString(iterValues.values(),sep));
			System.out.print(sep+":"+sep);
			System.out.println(ArraysTools.toString(f.filter(iterValues).getData(),sep));

		}

	}


	public static int getOffset(BayesianFactor f, TIntIntMap conf){
		if(!ArraysUtil.equals(f.getDomain().getVariables(), conf.keys(), true, true))
			throw new IllegalArgumentException("Incompatible domains");

		int[] states = IntStream.of(f.getDomain().getVariables()).map(x -> conf.get(x)).toArray();
		return f.getDomain().getOffset(states);
	}

	public static VertexFactor inverseFilter(VertexFactor f, int var, int state) {
		if(f.getDataDomain().contains(var))
			return inverseLeftFilter(f,var,state);
		if(f.getSeparatingDomain().contains(var))
			return inverseRightFilter(f,var,state);
		throw new IllegalArgumentException("Variable not present");

	}

	private static VertexFactor inverseLeftFilter(VertexFactor f, int var, int state){

		//A single left Variable
		if(f.getDataDomain().getVariables().length>1)
			throw new IllegalArgumentException("Factor cannot have more than 1 variable on the left");

		if(!f.getDataDomain().contains(var))
			throw new IllegalArgumentException("Target variable is not on the left");

		double[][][] newData =  new double[f.getSeparatingDomain().getCombinations()][][];
		// pa, vert, dim
		double data[][][] = f.getData();
		for(int i = 0; i<data.length; i++){
			newData[i] = new double[data[i].length][];
			for(int j = 0; j<data[i].length; j++){
				newData[i][j] = new double[data[i][j].length-1];

				for(int k = 0; k< data[i][j].length; k++){
					if(k<state)
						newData[i][j][k] = data[i][j][k];
					else if(k>state)
						newData[i][j][k-1] = data[i][j][k];
				}
			}
		}

		Strides newLeftDom = Strides.as(var, f.getDataDomain().getCardinality(var)-1);
		return new VertexFactor(newLeftDom, f.getSeparatingDomain(), newData);

	}


	private static VertexFactor inverseRightFilter(VertexFactor f, int var, int state){

		if(!f.getSeparatingDomain().contains(var))
			throw new IllegalArgumentException("Target variable is not on the right");

		int[] idxRemove = f.getSeparatingDomain().getCompatibleIndexes(ObservationBuilder.observe(var, state));
		double[][][] newData =  new double[f.getSeparatingDomain().getCombinations()- idxRemove.length][][];
		// pa, vert, dim
		double data[][][] = f.getData();
		int i_ = 0;
		for(int i = 0; i<data.length; i++) {
			if (!ArraysUtil.contains(i, idxRemove)) {

				newData[i_] = new double[data[i].length][];
				for (int j = 0; j < data[i].length; j++) {
					newData[i_][j] = new double[data[i][j].length];
					for (int k = 0; k < data[i][j].length; k++) {
						newData[i_][j][k] = data[i][j][k];
					}
				}
				i_++;
			}
		}

		Strides rightDom = f.getSeparatingDomain();

		int[] newRightCard =
				Arrays.stream(rightDom.getVariables())
						.map(v -> {
							if(v==var)
								return rightDom.getCardinality(v) - 1;
							return rightDom.getCardinality(v);}
						).toArray();

		Strides newRightDom = new Strides(rightDom.getVariables(), newRightCard);
		return new VertexFactor(f.getDataDomain(), newRightDom, newData);

	}

	public static double getValue(BayesianFactor factor, TIntIntHashMap obs){

		int[] states =  new int[factor.getDomain().getSize()];
		for(int i=0; i< states.length; i++) {
			int v = factor.getDomain().getVariables()[i];
			if(!obs.containsKey(v))
				throw new IllegalArgumentException("Missing value for variable "+v);
			states[i] = obs.get(v);
		}
		return factor.getValue(states);
	}

	public static void setValue(BayesianFactor factor, TIntIntHashMap obs, double value){

		int[] states =  new int[factor.getDomain().getSize()];
		for(int i=0; i< states.length; i++) {
			int v = factor.getDomain().getVariables()[i];
			if(!obs.containsKey(v))
				throw new IllegalArgumentException("Missing value for variable "+v);
			states[i] = obs.get(v);
		}
		factor.setValue(value, states);
	}

	public static BayesianFactor dropState(BayesianFactor factor, int var, int state){

		Strides oldDomain = factor.getDomain();
		Strides newDomain =
				Strides.as(
						Arrays.stream(oldDomain.getVariables())
								.mapToObj(v-> {
									if(v==var) return new int[] {v, oldDomain.getCardinality(v)-1};
									return new int[] {v, oldDomain.getCardinality(v)};
								}).flatMapToInt(Arrays::stream)
								.toArray()
				);

		BayesianFactor newFactor = new BayesianFactor(newDomain);
		int vpos = ArraysUtil.indexOf(var, oldDomain.getVariables());

		for(int[] Sold : DomainUtil.getEventSpace(oldDomain)){
			if (Sold[vpos] != state) {
				int[] Snew;
				double val = factor.getValue(Sold);
				if (Sold[vpos] < state) {
					Snew = Sold;
				} else {
					Snew = Sold;
					Snew[vpos] = Snew[vpos] - 1;
				}
				newFactor.setValue(val, Snew);
			}
		}
		return newFactor;
	}

	public static HashMap<Set<Integer>, BayesianFactor> intMapToHashMap(TIntObjectMap map){
		HashMap out = new HashMap();
		for(int v: map.keys()){
			Set<Integer> s = new HashSet<>();
			s.add(Integer.valueOf(v));
			out.put(s, map.get(v));
		}
		return out;
	}


	public static VertexFactor scalarMultiply(VertexFactor f, double scalar){
		double[][][] values = ArraysUtil.deepClone(f.getData());
		for(int i=0; i<values.length; i++) {
			for (int j = 0; j < values[i].length; j++) {
				for (int k = 0; k < values[i][j].length; k++) {
					values[i][j][k]=values[i][j][k]*scalar;
				}
			}
		}
		return new VertexFactor(f.getDataDomain(), f.getSeparatingDomain(), values);
	}



	public static IntervalFactor scalarMultiply(IntervalFactor f, double scalar){


		double[][] valuesUp = ArraysUtil.deepClone(f.getDataUpper());
		double[][] valuesLow = ArraysUtil.deepClone(f.getDataLower());

		for(int i=0; i<valuesUp.length; i++) {
			for (int j = 0; j < valuesUp[i].length; j++) {
				valuesUp[i][j]=valuesUp[i][j]*scalar;
			}
		}


		for(int i=0; i<valuesLow.length; i++) {
			for (int j = 0; j < valuesLow[i].length; j++) {
				valuesLow[i][j]=valuesLow[i][j]*scalar;
			}
		}

		return new IntervalFactor(f.getDataDomain(), f.getSeparatingDomain(), valuesLow, valuesUp);
	}
}
