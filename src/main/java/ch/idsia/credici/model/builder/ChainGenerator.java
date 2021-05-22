package ch.idsia.credici.model.builder;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.Combinatorial;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChainGenerator {

	int n;
	int treeWidth;
	StructuralCausalModel m = null;
	boolean doubleCard = false;

	int maxDist = 2;

	public ChainGenerator(int n, int treeWidth){
		this.n = n;
		this.treeWidth = treeWidth;

	}

	public StructuralCausalModel build(){

		m = new StructuralCausalModel();
		for(int i=0;i<n; i++)
			m.addVariable(2,false);

		int X[] = m.getEndogenousVars();
		for(int i=1;i<n; i++)
			m.addParents(X[i], X[i-1]);


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
		return new ChainGenerator(n, treeWidth).build();
	}

	public ChainGenerator setDoubleCard(boolean doubleCard) {
		this.doubleCard = doubleCard;
		return this;
	}

	private void addQuasiMarkCoFounders(){
		int[][] pairs = Combinatorial.randomPairs(m.getEndogenousVars(), maxDist);
		//int [][] pairs = IntStream.range(0, m.getEndogenousVars().length/2).mapToObj(i -> new int[]{i*2,i*2+1}).toArray(int[][]::new);

		for(int[] p : pairs) {
			// Cardinality of the new U variable
			int card = getExoCard(m, p);
			int u_new = m.addVariable(card, true);
			m.addParent(p[0], u_new);
			m.addParent(p[1], u_new);
		}
	}

	private int getExoCard(StructuralCausalModel model, int... X){
		int card = model.getDomain(Ints.concat(X, model.getEndegenousParents(X))).getCombinations();
		if(this.doubleCard)
			return card*2;
		return card+1;

	}

	private void addRandomCoFounders() {

		// Generate all the possible pairs and shuffle them
		List<int[]> pairs = Arrays.asList(Combinatorial.getCombinations(2, m.getEndogenousVars()))
				.stream()
				.filter(p -> p[0] < p[1])
				.filter(p -> p[1] - p[0] <= maxDist)
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
				int tw2 = m2.getExogenousTreewidth();

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


	public static void main(String[] args) {
		StructuralCausalModel m = ChainGenerator.build(5,1);
		System.out.println(m);
		System.out.println(m.getExogenousDAG());
		System.out.println(m.getExogenousTreewidth());
	}
}
