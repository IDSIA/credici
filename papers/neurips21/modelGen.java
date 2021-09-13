package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.ChainGenerator;
import ch.idsia.credici.model.builder.ReverseHMM;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class modelGen {

	//static String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
	static String wdir = "./";
	static String modelFolder = "papers/neurips21/models/";
	static String set = "sr";
	static String fcount = "_2";



	public static void main(String[] args) throws IOException, InterruptedException {

		// set1
		String[] topologies = new String[]{"rhmm"};
		int[] treeWidthExo = new int[]{0,1,2}; //
		int[] numEndogenous = new int[]{5,7,10};
		int[] index = IntStream.range(20,40).toArray();



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
		int maxDist = 2;


		int i = 0;

		do {
			feasible = true;

			if(top == "rhmm")
				m = ReverseHMM.build(nEndo, twExo, maxDist);
			else // chain
				m = ChainGenerator.build(nEndo, twExo, maxDist);


			try {
				if(dataCheck){
					m.toVCredal(FactorUtil.fixEmpiricalMap(m.getEmpiricalMap(), 5).values());
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

		String filename = wdir+modelFolder+set+twExo+fcount+"/"+name;
		System.out.println(filename+".uai");
		IO.write(m, filename+".uai");

		if(data != null) {
			System.out.println(filename + ".csv");
			DataUtil.toCSV(filename+".csv", data);
		}
	}
}
