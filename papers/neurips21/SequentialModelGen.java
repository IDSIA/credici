package neurips21;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.ChainGenerator;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import ch.idsia.credici.IO;


import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

public class SequentialModelGen {


	static String modelFolder = "./papers/neurips21/models/tmp2/";
	static String top = "chain";

	public static void main(String[] args) throws IOException {


		int[] treeWidthExo = new int[]{1}; //
		int[] index = IntStream.range(0,1).toArray();

		if(args.length>0){
			System.out.println("args: "+Arrays.toString(args));
			treeWidthExo = new int[]{Integer.valueOf(args[0])};
			index = IntStream.range(Integer.valueOf(args[1]),Integer.valueOf(args[2])).toArray();
		}


			for(int twExo : treeWidthExo){
					for(int idx: index){
						buildModel(1000, twExo, 5,15, false, idx);
					}
				}
	}

	private static void save(StructuralCausalModel m,   TIntIntMap[] data, int twExo, int nEndo, int idx) throws IOException {

		String filename = modelFolder+getName(twExo, nEndo, idx);
		System.out.println(filename);

		IO.write(m, filename+".uai");
		if(data!=null)
			DataUtil.toCSV(filename+".csv", data);
	}
	private static String getName(int twExo, int nEndo, int idx){
		return "chain_twExo"+twExo+"_nEndo"+nEndo+"_"+idx;
	}


	private static void buildModel(int dataSize, int twExo, int m, int nEndo, boolean doubleCard, int idx) throws IOException {

		String argStr = dataSize+"_"+twExo+"_"+m+"_"+doubleCard+"_"+idx;
		System.out.println(argStr);
		RandomUtil.setRandomSeed(argStr.hashCode());

		///
		StructuralCausalModel currentM = null;
		TIntIntMap[] currentD = null;
		do{
			currentM = new ChainGenerator(m, twExo).setDoubleCard(doubleCard).build();
			currentD = currentM.samples(dataSize, currentM.getEndogenousVars());
			System.out.print("0");
		}while(twExo < 2 && !currentM.isCompatible(currentD,5));

		save(currentM, currentD, twExo, m, idx);


		StructuralCausalModel candidateM = null;
		TIntIntMap[] candidateD = null;
		boolean error = false;

		for(int it = 1; it<nEndo/m; it++) {

			int j = 0;

			// Generate candidate
			do {

				j++;
				// Maximum limit of model generations
				if(j>5000) return;

				System.out.print(it);
				if (currentM == null) {
					candidateM = new ChainGenerator(m, twExo).setDoubleCard(doubleCard).build();
				} else {

					try {
						StructuralCausalModel newSeq = null;
						error = false;
						//  do {
						newSeq = new ChainGenerator(m, twExo).setDoubleCard(doubleCard).build();
						// }while(newSeq.getExogenousParents(0).length>1);

						newSeq = newSeq.incrementVarIDs(currentM.getVariables().length);

						StructuralCausalModel finalCurrentM = currentM;
						int xi = IntStream.of(currentM.getEndogenousVars()).filter(x -> finalCurrentM.getChildren(x).length == 0).toArray()[0];
						StructuralCausalModel finalNewSeq = newSeq;
						int xj = IntStream.of(newSeq.getEndogenousVars()).filter(x -> finalNewSeq.getEndegenousParents(x).length == 0).toArray()[0];

						candidateM = currentM.copy();

						/// Change u parents of xj

						int u1 = newSeq.getExogenousParents(xj)[0];
						int chU1[] = newSeq.getChildren(u1);

						if (chU1.length > 1) {
							int u1_ = newSeq.addVariable(newSeq.getSize(u1), true);
							for (int x : chU1) {
								if (x != xj) {
									newSeq.removeParent(x, u1);
									newSeq.addParent(x, u1_);
								}
							}
							newSeq.fillWithRandomFactors(5);
						}


						for (int x : newSeq.getEndogenousVars()) {
							candidateM.addVariable(x, newSeq.getSize(x), false);
						}
						for (int u : newSeq.getExogenousVars()) {

							int X[] = newSeq.getEndogenousChildren(u);
							int card = newSeq.getDomain(X).getCombinations();

							if (Arrays.asList(X).contains(xj))
								card = card * currentM.getDomain(xi).getCombinations();

							candidateM.addVariable(u, card * 2, true);
						}

						for (int v : newSeq.getVariables()) {
							candidateM.addParents(v, newSeq.getParents(v));
							//candidateM.setFactor(v, newSeq.getFactor(v));
						}

						// Add the link
						candidateM.addParents(xj, xi);
						for (int x : newSeq.getEndogenousVars())
							candidateM.randomizeEndoFactor(x);
						for (int u : newSeq.getExogenousVars())
							candidateM.randomizeExoFactor(u, 5);



						/// getDataCandidate
						candidateD = candidateM.samples(currentD, candidateM.getEndogenousVars());
					} catch(Exception e){
						System.out.print("#");
						error = true;
					}
				}
			} while (error || (twExo < 2 && !candidateM.isCompatible(candidateD, 5)));

			currentD = candidateD;
			currentM = candidateM;
			System.out.println(candidateM.getExogenousDAG());

			save(currentM, currentD, twExo, (it+1)*m , idx);

		}
	}
}
