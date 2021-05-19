package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.ChainGenerator;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

public class modelGencopy {

	//static String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
	static String wdir = "./";
	static String modelFolder = "papers/neurips21/models/set5/";
	static double maxDataTries = 1;


	public static void main(String[] args) throws IOException, InterruptedException {


		// set1
		String[] topologies = new String[]{"chain"};
		int[] treeWidthExo = new int[]{1}; //
		int[] numEndogenous = new int[]{5};
		int[] index = IntStream.range(0,10).toArray();



		for(String top : topologies){
			for(int twExo : treeWidthExo){
				for(int nEndo: numEndogenous){
					for(int idx: index){
						buildModel(top, twExo, nEndo, idx);
					}
				}
			}
		}


		/*

		   U1        U2
		 /   \  /   /   \
		X1   X2   X3   X4

		c-component = {U1, U2}  tw > 1

		 */


	}

	private static void buildModel(String top, int twExo, int nEndo, int idx) throws IOException {
		String name = top+"_twExo"+twExo+"_nEndo"+nEndo+"_"+idx;
		boolean feasible = true;



		StructuralCausalModel m = null;
		RandomUtil.setRandomSeed(name.hashCode());
		TIntIntMap[] data = null;
		ExactCredalBuilder builder = null;
		List<Integer> unfeasibleNodes = null;

		int i = 0;

		do{

			if (unfeasibleNodes == null)
				m = ChainGenerator.build(nEndo, twExo);
			else{
				for(int u : unfeasibleNodes){
					m.randomizeExoFactor(u, 2);
					//m.randomizeEndoChildren(u);
				}
			}



			//m = ModelGenerator.RandomChain(nEndo, twExo);


			System.out.print("*");
			i = 0;

			do {
				//RandomUtil.setRandomSeed(name.hashCode()+i);
				feasible = true;
				if (twExo < 2) {
					//RandomUtil.setRandomSeed(0);
					data = m.samples(1000, m.getEndogenousVars());

					try {
						HashMap empMap = DataUtil.getEmpiricalMap(m, data);
						empMap = FactorUtil.fixEmpiricalMap(empMap, 5);
						builder = ExactCredalBuilder.of(m)
								.setEmpirical(empMap.values())
								.setToVertex()
								.setRaiseNoFeasible(false)
								.build();

						unfeasibleNodes = builder.getUnfeasibleNodes();
						if (unfeasibleNodes.size() > 0) {
							//System.out.println(unfeasibleNodes);
							feasible = false;
							i++;
							System.out.print(".");
							System.out.print(unfeasibleNodes.size());
							if (i >= maxDataTries)
								break;
						}
					}catch (Exception e){
						feasible = false;
					}

				}


			}while(!feasible);
		}while(!feasible);

		System.out.println("tw="+m.getExogenousTreewidth());




		String filename = wdir+modelFolder+name+".uai";
		IO.write(m, filename);
		System.out.println(filename);
	}

}

