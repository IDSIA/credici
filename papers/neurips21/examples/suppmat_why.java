package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.VertexToInterval;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class suppmat_why {

	// Variables of the model
	static int X=0, Y=1, Z=2;
	static int U=3, V=4, W=5;



	public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException, IOException, CsvException {

		String prj_folder = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
		String folder = prj_folder + "papers/neurips21/examples/";

		// Read the conservative model
		StructuralCausalModel conservativeModel = (StructuralCausalModel) IO.readUAI(folder + "consPearl.uai");

		// Read the data
		TIntIntMap[] data = (TIntIntMap[]) DataUtil.fromCSV(folder + "dataPearl.csv");
		HashMap empData = DataUtil.getEmpiricalMap(conservativeModel, data);
		empData = FactorUtil.fixEmpiricalMap(empData, 6);

		System.out.print("Empirical from data:");
		for (Object p : empData.values())
			FactorUtil.print((BayesianFactor) p);

		// Run the exact inference
		SparseModel consVModel = conservativeModel.toVCredal(empData.values());
		CredalCausalVE infExact = new CredalCausalVE(consVModel);


		// Variables of the model
		int X = 0, Y = 1, Z = 2;
		int U = 3, V = 4, W = 5;

		// Upper an lower bounds
		new VertexToInterval().apply((VertexFactor) consVModel.getFactor(V), V);

		new VertexToInterval().apply((VertexFactor) consVModel.getFactor(U), U);

		new VertexToInterval().apply((VertexFactor) consVModel.getFactor(W), W);


		// Vertices for K(U)
		consVModel.getFactor(U);


		// Plot fx
		BayesianFactor fx = conservativeModel.getFactor(X);

		FactorUtil.print(fx);

		// Build incompatible non-conservative

		StructuralCausalModel mi = conservativeModel.copy();
		mi.removeVariable(U);
		mi.addVariable(U, 3, true);
		mi.addParents(X, U);

		BayesianFactor fx_ = EquationBuilder.of(mi).fromVector(X, 0, 1, 1, 0, 1, 1);//0,1,1, 1,0,1);
		fx_.filter(X, 0).filter(Z, 1);
		mi.setFactor(X, fx_);
		mi.fillExogenousWithRandomFactors(3);
		mi.toVCredal(empData.values());
	}
}
