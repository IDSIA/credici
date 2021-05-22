package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.ChainGenerator;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.data.WriterCSV;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.IntStream;

public class modelGen {

	//static String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
	static String wdir = "./";
	static String modelFolder = "papers/neurips21/models/s2_2/";

/*
TODO: experiments python code: more points and more time
 */

	public static void main(String[] args) throws IOException, InterruptedException {

		// set1
		String[] topologies = new String[]{"chain"};
		int[] treeWidthExo = new int[]{2}; //
		int[] numEndogenous = new int[]{5, 15};
		int[] index = IntStream.range(21,40).toArray();



		for(String top : topologies){
			for(int twExo : treeWidthExo){
				for(int nEndo: numEndogenous){
					for(int idx: index){

						boolean dataCheck = false;
						boolean empCheck = false;

						//if(twExo==0 || (twExo==1 && nEndo<8)) dataCheck = true;
						//if(twExo<2) empCheck = false;


						buildModel(top, twExo, nEndo, idx, false);
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

	private static void buildModel(String top, int twExo, int nEndo, int idx, boolean dataCheck) throws IOException {
		String name = top+"_twExo"+twExo+"_nEndo"+nEndo+"_"+idx;
		boolean feasible = true;

		StructuralCausalModel m = null;
		RandomUtil.setRandomSeed(name.hashCode());
		TIntIntMap[] data = null;



		int i = 0;

		do {
			feasible = true;

//			if(i%2==0)
				m = ChainGenerator.build(nEndo, twExo);
//			else
//				m.fillWithRandomFactors(3);


			try {
				if(dataCheck){

/*

					data = m.samples(1000, m.getEndogenousVars());
					HashMap empMap = DataUtil.getEmpiricalMap(m, data);
					empMap = FactorUtil.fixEmpiricalMap(empMap, 5);

					// If the data is not compatible, try with the exact empiricals
					if(!m.isCompatible(data, 5)) {
						data = null;
						m.toVCredal(FactorUtil.fixEmpiricalMap(m.getEmpiricalMap(), 5).values());
					}

 */
					m.toVCredal(FactorUtil.fixEmpiricalMap(m.getEmpiricalMap(), 5).values());

				}

				int t = m.getExogenousTreewidth();
				//System.out.println(t);
				if(t!=twExo)
					throw new IllegalArgumentException("");

			} catch (Exception e) {
				feasible = false;
				System.out.print("*");
				i++;
			}
			 catch (Error e) {
				feasible = false;
				System.out.print("#");
			i++;
		}
		}while(!feasible);

		System.out.println("tw="+m.getExogenousTreewidth());
		System.out.println(m.getExogenousDAG());




		String filename = wdir+modelFolder+name;
		System.out.println(filename+".uai");
		IO.write(m, filename+".uai");


		if(data != null) {
			System.out.println(filename + ".csv");
			DataUtil.toCSV(filename+".csv", data);

		}


/*		IO.write(m, filename+".uai");

		if(dataCheck)
			DataUtil.toCSV(filename+".csv", data);

*/
	}
}
