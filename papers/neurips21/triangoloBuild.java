package neurips21;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

public class triangoloBuild {

	static String prj_folder = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
	static String bnetFile = prj_folder+"models/empirical_triangolo.uai";
	static String scmFile = prj_folder+"models/triangolo_causal.uai";
	static String hFile = prj_folder+"models/triangolo_hcredal.uai";
	static String vFile = prj_folder+"models/triangolo_vcredal.uai";

	static BayesianNetwork bnet = null;

	public static void main(String[] args) throws IOException {

		bnet = (BayesianNetwork) IO.read(bnetFile);

		//buildSCM();
		//buildHmodel();;
		buildVmodel();



		//model.toVCredal(model.getEmpiricalMap().values())

	}

	private static void buildSCM() throws IOException {

		StructuralCausalModel m = CausalBuilder.of(bnet).build();
		System.out.println(m);
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
		Collection factors = bnet.getFactors();

		System.out.println("Building V-model");
		SparseModel hmodel = model.toVCredal(factors);

		System.out.println("Saving file");
		IO.writeUAI(hmodel, vFile);

	}
}
