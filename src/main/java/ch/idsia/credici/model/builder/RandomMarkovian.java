package ch.idsia.credici.model.builder;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import br.usp.poli.generator.BNGenerator;
import ch.idsia.credici.model.StructuralCausalModel;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class RandomMarkovian {

	// int totalDegree=10;
//	private int maxValues = 2; // Default is binary nodes.
	private boolean fixed_nVal = false;
	private int nIterations = 0;
	private int maxInducedWidth = -1; // this value means that there is no induced width constraint
	private int numberNodes = 4;
	private int numberMaxDegree = 5;
	private int numberMaxInDegree = 2;
	private int numberMaxOutDegree = 2;
	private int numberMaxArcs = 16;
//	private int nPoints = 3; // default number of points used to generate credal sets
	private float lowerP = 0;
	private float upperP = 1;

	private long seed = 0;
	
	public RandomMarkovian() {
		seed = System.nanoTime();
	}
	
	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public RandomMarkovian(long seed) {
		this.seed = seed;
	}
	
	public int getNumberNodes() {
		return numberNodes;
	}

	public void setNumberNodes(int numberNodes) {
		this.numberNodes = numberNodes;
	}

	public int getNumberMaxDegree() {
		return numberMaxDegree;
	}

	public void setNumberMaxDegree(int numberMaxDegree) {
		this.numberMaxDegree = numberMaxDegree;
	}

	public int getNumberMaxInDegree() {
		return numberMaxInDegree;
	}

	public void setNumberMaxInDegree(int numberMaxInDegree) {
		this.numberMaxInDegree = numberMaxInDegree;
	}

	public int getNumberMaxOutDegree() {
		return numberMaxOutDegree;
	}

	public void setNumberMaxOutDegree(int numberMaxOutDegree) {
		this.numberMaxOutDegree = numberMaxOutDegree;
	}

	public int getNumberMaxArcs() {
		return numberMaxArcs;
	}

	public void setNumberMaxArcs(int numberMaxArcs) {
		this.numberMaxArcs = numberMaxArcs;
	}


	public StructuralCausalModel generate(int endo, int exoSize, boolean monoleaf) {
		numberNodes = endo;
		numberMaxArcs = endo * 10;
		numberMaxDegree = numberNodes - 1;
		
		BNGenerator bn = new BNGenerator(numberNodes, numberMaxDegree, seed);
		
		bn.setnNodes(numberNodes); // set nNodes
		
		bn.setMaxDegree(numberMaxDegree); // set maxDegree
		bn.setMaxInDegree(numberMaxInDegree); // set maximum number of incoming arcs
		bn.setMaxOutDegree(numberMaxOutDegree); // set maximum number of outgoing arcs
		bn.setMaxArcs(numberMaxArcs); // set maxArcs(a global variable)
		bn.setFixed_nValue(fixed_nVal);
		// bn.setnPointProb(nPoints);
		bn.setLowerP(lowerP);
		bn.setUpperP(upperP);

		// Determining the number of iterations for
		// the chain to converge is a difficult task.
		// This value follows the DagAlea (see Melancon;Bousque,2000) suggestion,
		// and we verified that this number is satisfatory:
		if (nIterations == 0)
			nIterations = 6 * bn.getnNodes() * bn.getnNodes();

		bn.initializeGraph(); // Inicialize a simple ordered tree as a BN structure

		try {
			bn.generate("multi", 1, nIterations, numberNodes, "dag", "test", maxInducedWidth);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<Pair<Integer, Integer>> arcs = bn.toArcs();

		StructuralCausalModel model = new StructuralCausalModel();

		// add variables
		arcs.stream().flatMap(item -> Arrays.asList(item.getLeft(), item.getRight()).stream()).mapToInt(i -> i).sorted()
				.distinct().forEach(v -> model.addVariable(v, 2));

		// add arcs
		arcs.stream().forEach(arc -> model.addParent(arc.getRight(), arc.getLeft()));

		if (monoleaf) {
			// find all leaves
			var bounds = getEndogenousBoundaries(model);
			var leaves = bounds.getRight();

			if (leaves.length > 1) {
				int leaf = model.addVariable(2);
				model.addParents(leaf, leaves);
			}
		}

		// make markovian
		for (int variable : model.getEndogenousVars()) {
			int size = exoSize;
			int vsize = model.getSize(variable);
			
			var p = model.getFullDomain(variable).remove(variable);
			int comb = p.getCombinations();
			int cano = (int) Math.pow(vsize, comb);
			
			if (exoSize == -1) {
				var d = model.getFullDomain(variable);
				size = d.getCombinations();
			}
			if (cano < size) {
				System.out.println("limiting to " + cano);
			}
			// linit to the canonical 
			size = Math.max(size, cano);
			
			int exo = model.addVariable(size, true);
			model.addParent(variable, exo);
		}


		return model;
	}
	
	/** 
	 * Get endogenous roots and leaves of the specified network
	 * 
	 * @param model
	 * @return
	 */
	public Pair<int[], int[]> getEndogenousBoundaries(StructuralCausalModel model) {
		TIntList roots = new TIntArrayList();
		TIntList leaves = new TIntArrayList();
		for (int v : model.getEndogenousVars()) {
			if (model.getEndegenousParents(v).length == 0) {
				roots.add(v);
			}
			if (model.getEndogenousChildren(v).length == 0) {
				leaves.add(v);
			}
		}
		return Pair.of(roots.toArray(), leaves.toArray());
	}

}
