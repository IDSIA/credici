package ch.idsia.credici.factor;

import ch.idsia.credici.model.CausalOps;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.hash.TIntIntHashMap;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.ArrayList;
import java.util.List;

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
