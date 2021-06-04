import edu.neurips.causalem.IO;
import edu.neurips.causalem.inference.CausalInference;
import edu.neurips.causalem.inference.CausalMultiVE;
import edu.neurips.causalem.inference.CredalCausalVE;
import edu.neurips.causalem.model.StructuralCausalModel;
import edu.neurips.causalem.model.builder.EMCredalBuilder;
import edu.neurips.causalem.utility.DataUtil;
import edu.neurips.causalem.utility.FactorUtil;
import edu.neurips.causalem.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class suppmat {

	// Variables of the model
	static int X=0, Y=1, Z=2;
	static int U=3, V=4, W=5;



	public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException, IOException, CsvException {

		String folder = "./example/";

		// Read the conservative model
		StructuralCausalModel conservativeModel = (StructuralCausalModel) IO.readUAI(folder+"consPearl.uai");

		// Read the data
		TIntIntMap[] data = (TIntIntMap[]) DataUtil.fromCSV(folder+"dataPearl.csv");
		HashMap<Set<Integer>, BayesianFactor> empData = DataUtil.getEmpiricalMap(conservativeModel, data);
		empData = FactorUtil.fixEmpiricalMap(empData,6);

		System.out.print("Empirical from data:");
		for(BayesianFactor p : empData.values())
			FactorUtil.print(p);

		// Run the exact inference
		SparseModel consVModel =  conservativeModel.toVCredal(empData.values());
		CredalCausalVE infExact = new CredalCausalVE(consVModel);

		VertexFactor pn = (VertexFactor) infExact.probNecessity(X,Y);
		VertexFactor ps = (VertexFactor) infExact.probSufficiency(X,Y);
		VertexFactor pns = (VertexFactor) infExact.probNecessityAndSufficiency(X,Y,0,1);

		System.out.println("Exact method with the conservative model");
		System.out.println("=========================================");
		System.out.println("pn="+pn);
		System.out.println("ps="+ps);
		System.out.println("pns="+pns);


		// Modify the model to make it incompatible
		StructuralCausalModel incompatibleModel = getStructuralCausalModel(conservativeModel);

		// Run the CEM for building a new set of model
		EMCredalBuilder builder = new EMCredalBuilder(incompatibleModel, data, empData)
				.setMaxEMIter(200)
				.setNumTrajectories(20)
				.build();

		// Build an inference engine with all the SCM from the CEM
		CausalMultiVE infEM = new CausalMultiVE(builder.getSelectedPoints());

		// Run the queries
		VertexFactor pn2 = (VertexFactor) infEM.probNecessity(X,Y);
		VertexFactor ps2 = (VertexFactor) infEM.probSufficiency(X,Y);
		VertexFactor pns2 = (VertexFactor) infEM.probNecessityAndSufficiency(X,Y,0,1);


		System.out.println("Inference based on CEM\n");
		System.out.println("==========================");
		System.out.println("pn="+pn2);
		System.out.println("ps="+ps2);
		System.out.println("pns="+pns2);


		// Extract one of the precise models
		StructuralCausalModel m = builder.getSelectedPoints().get(0);

		System.out.println("P(U)");
		for(int u : m.getExogenousVars())
			FactorUtil.print(m.getFactor(u));

		HashMap empModel = m.getEmpiricalMap();

		System.out.println("Empirical from one of the learnt models: ");
		for(BayesianFactor p : m.getEmpiricalMap().values())
			FactorUtil.print(p);

		// Compute the PNS for the precise model
		BayesianFactor pns_i = (BayesianFactor) infEM.getInferenceList().get(0).probNecessityAndSufficiency(X,Y,0,1);

		double maxll = Probability.logLikelihood(empData, empData, 1);
		double mll = Probability.logLikelihood(empModel, empData, 1);
		double ratio = maxll / mll;

		System.out.println("PNS by the precise model = "+pns_i);
		System.out.println("Max LL the data = "+maxll);
		System.out.println("LL by this model and data = "+mll);
		System.out.println("Ratio =  "+ratio);



	}

	private static StructuralCausalModel getStructuralCausalModel(StructuralCausalModel conservativeModel) {
		StructuralCausalModel incompatibleModel = conservativeModel.copy();
		// Modify U
		incompatibleModel.removeVariable(U);
		incompatibleModel.addVariable(U, 3, true);
		BayesianFactor f = new BayesianFactor(incompatibleModel.getDomain(U,X,Z));
		f = f.reorderDomain(U,X,Z);
		f.setData(new double[]{0,0,1, 1,1,0,  1,0,0, 0,1,1});
		f = f.reorderDomain(conservativeModel.getFactor(X).getDomain().getVariables());
		incompatibleModel.setFactor(X, f);

		// Modify V
		incompatibleModel.removeVariable(V);
		incompatibleModel.addVariable(V, 6, true);
		f = incompatibleModel.getFactor(Y);
		f = f.reorderDomain(V,Y,X,Z);

		f = new BayesianFactor(incompatibleModel.getDomain(V,Y,X,Z));
		f.setData(new double[]{
				1,	1,	1,	0,	0,	0,
				0,	0,	0,	1,	1,	1,
				1,	1,	1,	1,	1,	0,
				0,	0,	0,	0,	0,	1,
				0,	1,	1,	1,	1,	1,
				1,	0,	0,	0,	0,	0,
				1,	0,	1,	1,	0,	0,
				0,	1,	0,	0,	1,	1
		});


		f = f.reorderDomain(Y);

		incompatibleModel.setFactor(Y, f);
		return incompatibleModel;
	}
}
