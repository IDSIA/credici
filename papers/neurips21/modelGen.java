package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.ModelGenerator;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.IntStream;

public class modelGen {

	//static String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
	static String wdir = "./";
	static String modelFolder = "papers/neurips21/models/set1b/";



	public static void main(String[] args) throws IOException, InterruptedException {

		// set1
		String[] topologies = new String[]{"chain"};
		int[] treeWidthExo = new int[]{0,1}; //
		int[] numEndogenous = new int[]{4,6};
		int[] index = IntStream.range(20,40).toArray();


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


		int i = 0;

		do {
			//RandomUtil.setRandomSeed(name.hashCode()+i);

			feasible = true;
			m = ModelGenerator.RandomChain(nEndo, twExo);

			try {
				if(twExo<2){
					//RandomUtil.setRandomSeed(0);
					TIntIntMap[] data = m.samples(1000, m.getEndogenousVars());
					HashMap empMap = DataUtil.getEmpiricalMap(m, data);
					empMap = FactorUtil.fixEmpiricalMap(empMap, 5);
					m.toVCredal(empMap.values());
				}
			} catch (Exception e) {
				feasible = false;
				System.out.print("*");
				i++;
			}
		}while(!feasible);

		System.out.println("tw="+m.getExogenousTreewidth());




		String filename = wdir+modelFolder+name+".uai";
		IO.write(m, filename);
		System.out.println(filename);
	}
}
