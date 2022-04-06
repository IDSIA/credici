package ch.idsia.credici.factor;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DomainUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.hash.TIntIntHashMap;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EquationOps {
	public static void setValue(BayesianFactor f, TIntIntHashMap exoPaValues, TIntIntHashMap endoPaValues, int var, int value){
		//System.out.println("exoPaValues: "+exoPaValues);
		//System.out.println("endoPaValues: "+endoPaValues);

		TIntIntHashMap conf = new TIntIntHashMap();

		for(int y : endoPaValues.keys())
			conf.put(y, endoPaValues.get(y));

		for(int u : exoPaValues.keys())
			conf.put(u, exoPaValues.get(u));

		for(int v=0; v<f.getDomain().getCardinality(var); v++){
			conf.put(var,v);
			int offset = FactorUtil.getOffset(f, conf);
			if(v!=value) {
				f.setValueAt(0, offset);
				//System.out.println("0 -> "+offset);
			}
			else {
				f.setValueAt(1, offset);
				//System.out.println("1 -> "+offset);

			}
		}

	}




	public static void setValue(BayesianFactor f, TIntIntHashMap exoPaValues, TIntIntHashMap endoPaValues, int[] vars, int[] value){

		TIntIntHashMap conf = new TIntIntHashMap();

		for(int y : endoPaValues.keys())
			conf.put(y, endoPaValues.get(y));

		for(int u : exoPaValues.keys())
			conf.put(u, exoPaValues.get(u));

		for(int[] v : DomainUtil.getEventSpace(DomainUtil.subDomain(f.getDomain(), vars))) {
			for(int i=0; i<vars.length; i++)
				conf.put(vars[i], v[i]);
			int offset = FactorUtil.getOffset(f, conf);

			if(!Arrays.equals(v, value)){
				f.setValueAt(0, offset);
				//System.out.println("0 -> "+offset);
			}
			else {
				f.setValueAt(1, offset);
				//System.out.println("1 -> "+offset);
			}
		}
	}

	public static int[] getValue(BayesianFactor f, TIntIntHashMap exoPaValues, TIntIntHashMap endoPaValues, int[] vars) {

		TIntIntHashMap conf = new TIntIntHashMap();
		for (int y : endoPaValues.keys())
			conf.put(y, endoPaValues.get(y));
		for (int u : exoPaValues.keys())
			conf.put(u, exoPaValues.get(u));
		for (int[] v : DomainUtil.getEventSpace(DomainUtil.subDomain(f.getDomain(), vars))) {
			for (int i = 0; i < vars.length; i++)
				conf.put(vars[i], v[i]);
			if (f.filter(conf).getData()[0] == 1)
				return v;
		}
		return null;
	}

	// getValue: BayesianFactor f, TIntIntHashMap exoPaValues, int[] vars

	public static int[] getValue(BayesianFactor f, TIntIntHashMap exoPaValues, int[] vars) {

		if(exoPaValues.size()!=1) throw new IllegalArgumentException("Wrong number of exogenous variables");
		int Uvar = exoPaValues.keys()[0];

		int[] endoPa = Arrays.stream(f.getDomain().getVariables()).filter(v -> v != Uvar && !ArraysUtil.contains(v, vars)).toArray();
		int[] xvalues = new int[0];
		for (int[] v : DomainUtil.getEventSpace(DomainUtil.subDomain(f.getDomain(), endoPa))) {
			int[] xval = EquationOps.getValue(f, exoPaValues, ObservationBuilder.observe(endoPa, v), vars);
			xvalues = Ints.concat(xvalues, xval);
		}
		return xvalues;
	}


	public static boolean isConservative(BayesianFactor f, int exoVar, int[] leftVars){

		int[] endoPa =Arrays.stream(f.getDomain().getVariables()).filter(v -> v != exoVar && !ArraysUtil.contains(v, leftVars)).toArray();
		Strides domX = DomainUtil.subDomain(f.getDomain(), leftVars);
		Strides domY = DomainUtil.subDomain(f.getDomain(), endoPa);
		int m = domY.getCombinations();
		int Usize = f.getDomain().getCardinality(exoVar);

		List Xvalues =
				IntStream.range(0,Usize).mapToObj(u ->
						EquationOps.getValue(f, ObservationBuilder.observe(exoVar,  u), leftVars)
				).collect(Collectors.toList());

		for(int[] confX : DomainUtil.getEventSpace(IntStream.range(0,m).mapToObj(k->domX).toArray(Strides[]::new))){
			if(!Xvalues.stream().anyMatch(xi -> Arrays.equals((int[]) xi, confX)))
				return false;
		}
		return true;
	}
	public static List<int[]> getRedundancies(BayesianFactor f, int exoVar, int[] leftVars){

		int[] endoPa =Arrays.stream(f.getDomain().getVariables()).filter(v -> v != exoVar && !ArraysUtil.contains(v, leftVars)).toArray();
		Strides domX = DomainUtil.subDomain(f.getDomain(), leftVars);
		Strides domY = DomainUtil.subDomain(f.getDomain(), endoPa);
		int m = domY.getCombinations();
		int Usize = f.getDomain().getCardinality(exoVar);

		int[][] Xvalues =
				IntStream.range(0,Usize).mapToObj(u ->
						EquationOps.getValue(f, ObservationBuilder.observe(exoVar,  u), leftVars)
				).toArray(int[][]::new);

		List redundant = new ArrayList();

		for(int[] confX : DomainUtil.getEventSpace(IntStream.range(0,m).mapToObj(k->domX).toArray(Strides[]::new))) {
			List idx = new ArrayList();
			for (int i = 0; i < Xvalues.length; i++) {
				if (Arrays.equals(Xvalues[i], confX))
					idx.add(i);
			}
			if(idx.size()>1)
				redundant.add(CollectionTools.toIntArray(idx));
		}
		return redundant;
	}

	public static int maxExoCardinality(int exoVar, StructuralCausalModel model) {
		if(!model.isExogenous(exoVar))
			throw new IllegalArgumentException("Variable "+exoVar+" is not exogenous");
		return maxExoCardinality(exoVar, model.getNetwork(), model.getDomain(model.getEndogenousVars()));

	}

	public static int maxExoCardinality(int exoVar, DirectedAcyclicGraph causalDAG, Strides domains) {


		DirectedAcyclicGraph endoDAG = DAGUtil.getSubDAG((SparseDirectedAcyclicGraph) causalDAG, DAGUtil.getEndogenous((SparseDirectedAcyclicGraph) causalDAG));

		int[] endoCh = DAGUtil.getTopologicalOrder(endoDAG, ((SparseDirectedAcyclicGraph) causalDAG).getChildren(exoVar));
		List previousVars = new ArrayList();
		int size = 1;

		for(int i = 0; i<endoCh.length; i++) {
			int[] endoPa = ArraysUtil.difference(
					((SparseDirectedAcyclicGraph) endoDAG).getParents(endoCh[i]),
					CollectionTools.toIntArray(previousVars));

			int cardX = domains.intersection(endoCh[i]).getCombinations();
			int cardY = domains.intersection(endoPa).getCombinations();
			size *= (int)Math.pow(cardX, cardY);

			previousVars.add(endoCh[i]);
			for(int p: endoPa)
				previousVars.add(p);

		}

		//if (exoVar==3) return 32;
		return size;
	}

}
