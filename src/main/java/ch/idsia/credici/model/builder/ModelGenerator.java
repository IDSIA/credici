package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.Combinatorial;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ModelGenerator {
	public static StructuralCausalModel RandomChain(int n, int treeWidth){

		StructuralCausalModel m = new StructuralCausalModel();
		for(int i=0;i<n; i++)
			m.addVariable(2,false);

		int X[] = m.getEndogenousVars();
		for(int i=1;i<n; i++)
			m.addParents(X[i], X[i-1]);


		if(treeWidth==1)
			m = addQuasiMarkCoFounders(m);
		else if(treeWidth>1)
			m =  addRandomCoFounders(m, treeWidth);

		m = addMissingExogenous(m);
		return m;
	}

	private static StructuralCausalModel addQuasiMarkCoFounders(StructuralCausalModel m ){
		int[][] pairs = Combinatorial.randomPairs(m.getEndogenousVars());

		for(int[] p : pairs) {
			// Cardinality of the new U variable
			int card = m.getDomain(Ints.concat(p, m.getEndegenousParents(p))).getCombinations();
			int u_new = m.addVariable(card, true);
			m.addParent(p[0], u_new);
			m.addParent(p[1], u_new);
		}

		return m;
	}

	private static StructuralCausalModel addRandomCoFounders(StructuralCausalModel endogenousModel, int treeWidth) {

		// Generate all the possible pairs and shuffle them
		List<int[]> pairs = Arrays.asList(Combinatorial.getCombinations(2, endogenousModel.getEndogenousVars()))
				.stream().filter(p -> p[0] < p[1]).collect(Collectors.toList());

		Collections.shuffle(pairs);

		// get the treewidth of the current model
		int tw = endogenousModel.getExogenousTreewidth();
		for(int[] p : pairs){
			// Build a new candidate model
			StructuralCausalModel m2 = endogenousModel.copy();

			// Cardinality of the new U variable
			int card = m2.getDomain(Ints.concat(p, m2.getEndegenousParents(p))).getCombinations();
			int u_new = m2.addVariable(card, true);
			m2.addParent(p[0], u_new);
			m2.addParent(p[1], u_new);

			// Treewidth of the new model
			try {
				int tw2 = m2.getExogenousTreewidth();

				if (tw2 <= treeWidth) {
					endogenousModel = m2;
					tw = tw2;
				} else if (tw == treeWidth) {
					break;
				}
			}catch (Exception e){}
		}

		return endogenousModel;
	}

	private static StructuralCausalModel addMissingExogenous(StructuralCausalModel endogenousModel){
		// Add U nodes to those X without a U parent
		for(int x: endogenousModel.getEndogenousVars()){
			if(endogenousModel.getExogenousParents(x).length==0){
				int card = endogenousModel.getDomain(Ints.concat(new int[]{x}, endogenousModel.getEndegenousParents(x))).getCombinations();
				int u_new = endogenousModel.addVariable(card, true);
				endogenousModel.addParent(x, u_new);
			}
		}

		// fill the factors
		endogenousModel.fillWithRandomFactors(3);
		return endogenousModel;
	}


	public static void main(String[] args) {
		StructuralCausalModel m = ModelGenerator.RandomChain(5,3);
		System.out.println(m);
		System.out.println(m.getExogenousDAG());
		System.out.println(m.getExogenousTreewidth());
	}
}
