package ch.idsia.credici.utility.apps;

import ch.idsia.credici.factor.BayesianFactorBuilder;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.eclipse.persistence.internal.libraries.asm.tree.TypeInsnNode;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SelectionBias {
	public static StructuralCausalModel addSelectorFullyConnected(StructuralCausalModel m, int... assignments){
		return addSelector(m, m.getEndogenousVars(), assignments);
	}

	public static StructuralCausalModel addSelectorLimited(int numParents, StructuralCausalModel m, int... assignments){
		if(m.getExogenousVars().length<numParents || numParents<2)
			new IllegalArgumentException("Wrong number of parents");

		int[] endoVars = m.getEndogenousVars();
		int[] parents = new int[numParents];

		// The first and the last are always in
		parents[0] = endoVars[0];
		parents[numParents-1] = endoVars[endoVars.length-1];

		int idx[] = CollectionTools.shuffle(IntStream.range(1, endoVars.length-1).toArray());
		for(int i=0; i<numParents-2; i++)
			parents[i+1] = endoVars[idx[i]];

		return addSelector(m, parents, assignments);
	}


	public static StructuralCausalModel addSelector(StructuralCausalModel m, int[] parents, int... assignments) {
		if(assignments.length != m.getDomain(parents).getCombinations())
			new IllegalArgumentException("Wrong number of assignments: it should be equal to the size of the joint endogenous domain.");


		// Add the selector variable
		StructuralCausalModel selectModel = m.copy();
		int s = selectModel.addVariable(2, false);
		int us = selectModel.addVariable(1, true);

		selectModel.addParent(s,us);
		selectModel.addParents(s, parents);
		selectModel.setFactor(us, BayesianFactorBuilder.as(selectModel.getDomain(us), new double[]{1.0}));


		BayesianFactor fs = EquationBuilder.fromVector(
				selectModel.getDomain(s),
				selectModel.getDomain(selectModel.getParents(s)),
				assignments
		);
		selectModel.setFactor(s, fs);

		return selectModel;
	}




	public static TIntIntMap[] applySelector(TIntIntMap[] dataX, StructuralCausalModel model, int selectorVar){

		int X[] = IntStream.of(model.getEndogenousVars()).filter(x -> x!=selectorVar).toArray();
		int Xselecting[] = model.getEndegenousParents(selectorVar);
		// Add the value of S
		TIntIntMap[] data =  Stream.of(dataX).map(d -> {
			TIntIntHashMap dnew = new TIntIntHashMap(d);
			int valS = (int) model.getFactor(selectorVar).filter((TIntIntHashMap) DataUtil.select(d, Xselecting)).getData()[1];
			dnew.put(selectorVar, valS);
			return dnew;
		}).toArray(TIntIntMap[]::new);


		// Remove X values when  S=0
		data =
				Stream.of(data).map(d -> {
					if(d.get(selectorVar)==0){
						for(int x : X)
							d.remove(x);
					}
					return d;
				}).toArray(TIntIntMap[]::new);
		return data;

	}
}
