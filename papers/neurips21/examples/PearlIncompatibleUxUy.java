package neurips21.examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.util.HashMap;

public class PearlIncompatibleUxUy {
	public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {

		String prj_folder = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";


		String folder = prj_folder+"papers/neurips21/examples/";

		TIntIntMap[] data = (TIntIntMap[])DataUtil.fromCSV(folder+"dataPearl.csv");
		StructuralCausalModel mcons = (StructuralCausalModel)IO.readUAI(folder+"consPearl.uai");


		// states x, y, z = True = 0
		int x=0, y=0, z=0;

		// states x', y', z' = False = 1
		int x_=1, y_=1, z_=1;

		int X=0, Y=1, Z=2;
		int Ux = mcons.getExogenousParents(X)[0]; //3
		int Uy = mcons.getExogenousParents(Y)[0]; //4
		int Uz = mcons.getExogenousParents(Z)[0]; //5



		HashMap empData = DataUtil.getEmpiricalMap(mcons, data);
		empData = FactorUtil.fixEmpiricalMap(empData,6);
		System.out.println(empData);


		StructuralCausalModel mxy = mcons.copy();


		// Alter the Ux
		mxy.removeVariable(Ux);
		mxy.addVariable(Ux, 3, true);


		BayesianFactor f = new BayesianFactor(mxy.getDomain(Ux,X,Z));

		f = f.reorderDomain(Ux,X,Z);
		f.setData(new double[]{0,0,1, 1,1,0,  1,0,0, 0,1,1});

		f.filter(Z,0).filter(X,0);
		f.filter(Z,0).filter(X,1);
		f.filter(Z,1).filter(X,0);
		f.filter(Z,1).filter(X,1);

		f = f.reorderDomain(mcons.getFactor(X).getDomain().getVariables());

		mxy.setFactor(X, f);

		////// Alter fy


		f = mxy.getFactor(Y);

		f = f.reorderDomain(Uy,Y,X,Z);

		f.getData();

		f.setData(new double[]{
				1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
				1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, //
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, //
				0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, //
				1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, //
				1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0,
				0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0
		});

		f.filter(X,0).filter(Z,1).filter(Y,0);
		f.filter(X,0).filter(Z,1).filter(Y,1);
		f.filter(X,1).filter(Z,0).filter(Y,0);
		f.filter(X,1).filter(Z,0).filter(Y,1);

		f = f.reorderDomain(mcons.getFactor(Y).getDomain().getVariables());

		mxy.setFactor(Y, f);



		//f.reorderDomain(mcons.getFactor(X).getDomain().getVariables())


		SparseModel vmcons =  mcons.toVCredal(empData.values());

		// not feasible as expected
		//mxy.toVCredal(empData.values());



		EMCredalBuilder builder = new EMCredalBuilder(mxy, data, empData)
				.setMaxEMIter(200)
				.setNumTrajectories(20)
				//.setNumDecimalsRound(-1)
				.build();


		CausalMultiVE inf2 = new CausalMultiVE(builder.getSelectedPoints());

		VertexFactor pn2 = (VertexFactor) inf2.probNecessity(X,Y);
		VertexFactor ps2 = (VertexFactor) inf2.probSufficiency(X,Y);
		VertexFactor pns2 = (VertexFactor) inf2.probNecessityAndSufficiency(X,Y,0,1);


		System.out.println("EM method: Ux ternary incompatible and Uy 16 states incompatible too\n");
		System.out.println("====================");


		System.out.println("pn="+pn2);
		System.out.println("ps="+ps2);
		System.out.println("pns="+pns2);

		for(StructuralCausalModel m : builder.getSelectedPoints())
			System.out.println(m.getEmpiricalMap());


		for(StructuralCausalModel m : builder.getSelectedPoints())
			System.out.println(m.getEmpiricalMap());



		for(CausalInference i : inf2.getInferenceList()){
			BayesianFactor pns = (BayesianFactor) i.probNecessityAndSufficiency(X,Y,0,1);
			System.out.println(pns);
		}


		StructuralCausalModel m = builder.getSelectedPoints().get(0);


		System.out.println("P(U)");
		for(int u : m.getExogenousVars())
			FactorUtil.print(m.getFactor(u));

		HashMap empModel = m.getEmpiricalMap();

		System.out.println("Empiricals: ");
		for(BayesianFactor p : m.getEmpiricalMap().values())
			FactorUtil.print(p);


		BayesianFactor pns = (BayesianFactor) inf2.getInferenceList().get(0).probNecessityAndSufficiency(X,Y,0,1);

		double maxll = Probability.logLikelihood(empData, empData, 1);
		double mll = Probability.logLikelihood(empModel, empData, 1);
		double ratio = maxll / mll;

		System.out.println("PNS by this model = "+pns);
		System.out.println("Max LL by this data = "+maxll);
		System.out.println("LL by this model and data = "+mll);
		System.out.println("Ratio =  "+ratio);






	}
}
