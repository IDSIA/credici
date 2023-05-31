package ch.idsia.credici.utility.apps;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import ch.idsia.credici.collections.FIntIntHashMap;
import org.eclipse.persistence.internal.libraries.asm.tree.TypeInsnNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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


	public static StructuralCausalModel addSelector(StructuralCausalModel m, int[] parents, int[]... hidden_conf){
		return addSelector(m, parents, SelectionBias.getAssignmentWithHidden(m, parents, hidden_conf));
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
		selectModel.setFactor(us, new BayesianFactor(selectModel.getDomain(us), new double[]{1.0}));


		BayesianFactor fs = EquationBuilder.fromVector(
				selectModel.getDomain(s),
				selectModel.getDomain(Ints.concat(parents, new int[]{us})),
				assignments
		);
		selectModel.setFactor(s, fs);

		return selectModel;
	}




	public static TIntIntMap[] applySelector(TIntIntMap[] dataX, StructuralCausalModel model, int selectorVar){

		int X[] = IntStream.of(model.getEndogenousVars()).filter(x -> x!=selectorVar).toArray();
		int Xselecting[] = model.getEndogenousParents(selectorVar);
		// Add the value of S
		TIntIntMap[] data =  Stream.of(dataX).map(d -> {
			FIntIntHashMap dnew = new FIntIntHashMap(d);
			int valS = (int) model.getFactor(selectorVar).filter((FIntIntHashMap) DataUtil.select(d, Xselecting)).getData()[1];
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

	public static int[] getAssignmentWithHidden(StructuralCausalModel model, int[] Sparents, int[]... hiddenConf){
		Strides dom = model.getDomain(Sparents);
		int assignments[] = IntStream.range(0, dom.getCombinations()).map(i->1).toArray();
		for(int[] c : hiddenConf)
			assignments[dom.getOffset(c)] = 0;
		return assignments;
	}

	public static int[] getAssignmentWithVisible(StructuralCausalModel model, int[] Sparents, int[]... visibleConf){
		Strides dom = model.getDomain(Sparents);
		int assignments[] = IntStream.range(0, dom.getCombinations()).map(i->0).toArray();
		for(int[] c : visibleConf)
			assignments[dom.getOffset(c)] = 1;
		return assignments;
	}

	public static List<StructuralCausalModel> runEM(StructuralCausalModel modelBiased, int selectVar, TIntIntMap[] dataBiased, int maxIter, int executions) throws InterruptedException {
		int[] trainable = Arrays.stream(modelBiased.getExogenousVars())
				.filter(v -> !ArraysUtil.contains(selectVar, modelBiased.getChildren(v)))
				.toArray();

		EMCredalBuilder builder = EMCredalBuilder.of(modelBiased, dataBiased)
				.setMaxEMIter(maxIter)
				.setNumTrajectories(executions)
				.setTrainableVars(trainable)
				.setWeightedEM(true)
				.build();

		List endingPoints = builder.getSelectedPoints().stream().map(m -> {
			m = m.copy();
			m.removeVariable(m.getExogenousParents(selectVar)[0]);
			m.removeVariable(selectVar);
			return m;
		}).collect(Collectors.toList());
		return endingPoints;
	}

	public static int findSelector(StructuralCausalModel modelBiased){

		int[] exoPa = Arrays.stream(modelBiased.getExogenousVars())
				.filter(u-> modelBiased.getDomain(u).getCombinations()== 1).toArray();


		if (exoPa.length != 1 || modelBiased.getChildren(exoPa[0]).length !=1)
			throw new IllegalArgumentException("Model should have only one vacuous exogenous with one child.");

		int S = modelBiased.getEndogenousChildren(exoPa[0])[0];

		if(modelBiased.getChildren(S).length>0)
			throw new IllegalArgumentException("Selector cannot have children.");

		return S;

	}

}
