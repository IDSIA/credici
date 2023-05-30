package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.Combinatorial;
import ch.idsia.credici.utility.DAGUtil;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReverseHMM {

	int numEndog;
	int treeWidth;
	StructuralCausalModel m = null;
	boolean doubleCard = false;

	int maxDist = 2;

	public ReverseHMM(int numEndog, int treeWidth){
		this.numEndog = numEndog;
		this.treeWidth = treeWidth;

	}

	public StructuralCausalModel build(){

		int lenX = (int) Math.round(this.numEndog/2.0);
		int lenY = (int) Math.floor(this.numEndog/2.0);

		int X[] = new int[lenX];
		int Y[] = new int[lenY];


		m = new StructuralCausalModel();
		for(int i = 0; i< lenX; i++) {
			X[i] = m.addVariable(2, false);
			if (i < lenY)
				Y[i] = m.addVariable(2, false);
		}


		for(int i = 1; i<lenX; i++)
			m.addParents(X[i], X[i-1]);

		for(int i = 0; i< lenY; i++)
			m.addParents(X[i], Y[i]);



		if(treeWidth==1)
			addQuasiMarkCoFounders();
		else if(treeWidth>1)
			addRandomCoFounders();

		addMissingExogenous();
		// fill the factors
		m.fillWithRandomFactors(3);
		return m;
	}




	public static StructuralCausalModel build(int n, int treeWidth){
		return new ReverseHMM(n, treeWidth).build();
	}
	public static StructuralCausalModel build(int n, int treeWidth, int maxDist){
		return new ReverseHMM(n, treeWidth).setMaxDist(maxDist).build();
	}

	public ReverseHMM setDoubleCard(boolean doubleCard) {
		this.doubleCard = doubleCard;
		return this;
	}

	private void addQuasiMarkCoFounders(){
		int[][] pairs = Combinatorial.randomPairs(m.getEndogenousVars(), maxDist, DAGUtil.matrixDistances(m.getNetwork()));
		for(int[] p : pairs) {
			// Cardinality of the new U variable
			int card = getExoCard(m, p);
			int u_new = m.addVariable(card, true);
			m.addParent(p[0], u_new);
			m.addParent(p[1], u_new);
		}
	}

	private int getExoCard(StructuralCausalModel model, int... X){
		int card = model.getDomain(Ints.concat(X, model.getEndogenousParents(X))).getCombinations();
		if(this.doubleCard)
			return card*2;
		return card+1;

	}

	private void addRandomCoFounders() {

		int dist[][] = DAGUtil.matrixDistances(m.getNetwork());

		// Generate all the possible pairs and shuffle them
		List<int[]> pairs = Arrays.asList(Combinatorial.getCombinations(2, m.getEndogenousVars()))
				.stream()
				.filter(p -> p[0] < p[1])
				.filter(p -> dist[p[1]][p[0]] <= maxDist)
				.collect(Collectors.toList());

		Collections.shuffle(pairs);

		// get the treewidth of the current model
		int tw = m.getExogenousTreewidth();
		for(int[] p : pairs){
			// Build a new candidate model
			StructuralCausalModel m2 = m.copy();

			// Cardinality of the new U variable
			int card = getExoCard(m2, p);
			int u_new = m2.addVariable(card, true);
			m2.addParent(p[0], u_new);
			m2.addParent(p[1], u_new);

			// Treewidth of the new model
			try {
				//int tw2 = m2.getExogenousTreewidth();
				int tw2 = m2.maxExoCC();

				if (tw2 <= treeWidth) {
					m = m2;
					tw = tw2;
				} else if (tw == treeWidth) {
					break;
				}
			}catch (Exception e){}
		}
	}

	private void addMissingExogenous(){
		// Add U nodes to those X without a U parent
		for(int x: m.getEndogenousVars()){
			if(m.getExogenousParents(x).length==0){
				int card = getExoCard(m, x);
				int u_new = m.addVariable(card, true);
				m.addParent(x, u_new);
			}
		}
	}

	public ReverseHMM setMaxDist(int maxDist) {
		this.maxDist = maxDist;
		return this;
	}

	public static void main(String[] args) {
		StructuralCausalModel m = ReverseHMM.build(5,0);
		System.out.println(m);
		System.out.println(m.getNetwork());
		System.out.println(m.getExogenousDAG());
		System.out.println(m.getExogenousTreewidth());
	}
}
