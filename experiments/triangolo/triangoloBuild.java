package neurips21.triangolo;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.ExactCredalBuilder;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class triangoloBuild {

	//static String prj_folder = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
	static String prj_folder = "./";

	static String bnetFile = prj_folder+"models/empirical_triangolo.uai";
	static String scmFile = prj_folder+"models/triangolo_causal.uai";
	static String hFile = prj_folder+"models/triangolo_hcredal.uai";
	static String vFile = prj_folder+"models/triangolo_vcredal.uai";

	static BayesianNetwork bnet = null;

	public static void main(String[] args) throws IOException {

		RandomUtil.setRandomSeed(0);

		bnet = (BayesianNetwork) IO.read(bnetFile);
		System.out.println(bnet.getNetwork()); // (8,0), (7,0), (2,0), (11,0), (9,2), (1,3), (8,5), (3,6), (10,6), (1,6), (5,7), (1,7), (10,8), (1,8), (1,9), (4,10), (1,10), (6,11)


		//buildSCM();
		//buildVmodel();


		int s = 0;
		boolean feasible = false;
		/*do {
			try {
				feasible = true;
				System.out.println(s);
				RandomUtil.setRandomSeed(s);
				buildSCM();
				//buildHmodel();;
				buildVmodel();
			} catch (Exception e) {
				s++;
				feasible = false;
			}
		}while(!feasible);*/

		//model.toVCredal(model.getEmpiricalMap().values())

	}

	private static void buildSCM() throws IOException {

		int[] exoSizes = bnet.getFactors().stream().mapToInt(f -> f.getData().length + 1).toArray();

		StructuralCausalModel m =
				CausalBuilder.of(bnet)
						//.setExoVarSizes(exoSizes)
						//.setFillRandomEquations(true)
						.build();

		System.out.println(m);

		for(int u: m.getExogenousVars())
			System.out.println("u\t"+m.getFactor(u));

		IO.writeUAI(m, scmFile);
	}

	private static void buildHmodel() throws IOException {
		System.out.println("Reading model");
		StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(scmFile);

		System.out.println("Preparing empirical");
		Collection factors = bnet.getFactors();

		System.out.println("Building H-model");
		SparseModel hmodel = model.toHCredal(factors);

		System.out.println("Saving file");
		IO.writeUAI(hmodel, hFile);

	}


	private static void buildVmodel() throws IOException {
		System.out.println("Reading model");
		StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(scmFile);

		System.out.println("Preparing empirical");
		//Collection factors = bnet.getFactors();

	//int num_decimals, boolean newZeros, int... left_vars
		Collection factors = IntStream.of(bnet.getVariables())
				.mapToObj(x -> FactorUtil.fixPrecission(bnet.getFactor(x), 3, true, x ))
				.collect(Collectors.toList());

		System.out.println("Building V-model");
		//SparseModel vmodel = model.toVCredal(factors);
		SparseModel vmodel = null;

		ExactCredalBuilder builder = ExactCredalBuilder.of(model)
				.setEmpirical(factors)
				.setToVertex()
				.setRaiseNoFeasible(false)
				.build();

		List<Integer> unfeasibleNodes = builder.getUnfeasibleNodes();
		if(unfeasibleNodes.size()>0){
			System.out.println(unfeasibleNodes);
		}else {
			vmodel = builder.getModel();
			System.out.println("Saving file");
			IO.writeUAI(vmodel, vFile);
		}

	}
}
