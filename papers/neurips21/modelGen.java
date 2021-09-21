package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.ChainGenerator;
import ch.idsia.credici.model.builder.ReverseHMM;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class modelGen {

	//static String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
	static String wdir = "./";
	static String modelFolder = "papers/neurips21/models/";
	static String set = "src";
	static String fcount = "";
	static Boolean overwrite = true;
	static int datasize = 1000;


	public static void main(String[] args) throws IOException, InterruptedException {

		// set1
		String[] topologies = new String[]{"rhmm"};
		int[] treeWidthExo = new int[]{0,1}; //
		int[] numEndogenous = new int[]{5,7,10};
		int[] index = IntStream.range(0,20).toArray();



		for(String top : topologies){
					for(int idx: index){
						for(int nEndo: numEndogenous){
							for(int twExo : treeWidthExo){


							boolean dataCheck = false;
						boolean empCheck = false;
						if(top == "rhmm")
							dataCheck = true;

						//if(twExo==0 || (twExo==1 && nEndo<8)) dataCheck = true;
						//if(twExo<2) empCheck = false;

						buildModel(top, twExo, nEndo, idx, dataCheck);
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
		int maxDist = 2;

		boolean doubleCard = false;
		if(twExo==1)
			doubleCard = false;


		String filename = wdir+modelFolder+set+twExo+fcount+"/"+name;
		System.out.println("\nGenerating:");
		System.out.println(filename);

		if(!overwrite && new File(filename+".uai").exists())
			return;

		int i = 0;

		do {
			feasible = true;

			if(top == "rhmm") {
				m = new ReverseHMM(nEndo, twExo).setMaxDist(maxDist).setDoubleCard(doubleCard).build();
				//m = ReverseHMM.build(nEndo, twExo, maxDist);
			}
			else // chain
				m = ChainGenerator.build(nEndo, twExo, maxDist);


			try {
				if(dataCheck){

					int j = 0;
					do {
						if(j>10)
							throw new IllegalStateException("Compatible data not found");
						data = m.samples(datasize, m.getEndogenousVars());
						System.out.print(".");
						j++;
					}while(!m.isCompatible(data, 5));
					//m.toVCredal(FactorUtil.fixEmpiricalMap(m.getEmpiricalMap(), 5).values());
				}

				int t = m.maxExoCC();
				System.out.println(m.exoConnectComponents().stream().map(c -> Arrays.toString(c)).collect(Collectors.joining()));
				System.out.println(m.endoConnectComponents().stream().map(c -> Arrays.toString(c)).collect(Collectors.joining()));

				if(twExo>0 && t!=twExo)
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

		System.out.println(filename+".uai");
		IO.write(m, filename+".uai");

		if(data != null) {
			System.out.println(filename + ".csv");
			DataUtil.toCSV(filename+".csv", data);
		}

	}
}
