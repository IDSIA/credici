package neurips21.examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.util.HashMap;

public class PearlIncompatibleUx {
	public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {

		String prj_folder = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";


		String folder = prj_folder+"papers/neurips21/examples/";

		TIntIntMap[] data = (TIntIntMap[]) DataUtil.fromCSV(folder+"dataPearl.csv");
		StructuralCausalModel mcons = (StructuralCausalModel) IO.readUAI(folder+"consPearl.uai");


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


		StructuralCausalModel mx = mcons.copy();


		// Alter the Ux
		mx.removeVariable(Ux);
		mx.addVariable(Ux, 3, true);


		BayesianFactor f = new BayesianFactor(mx.getDomain(Ux,X,Z));

		f = f.reorderDomain(Ux,X,Z);
		f.setData(new double[]{0,0,1, 1,1,0,  1,0,0, 0,1,1});

		f.filter(Z,0).filter(X,0);
		f.filter(Z,0).filter(X,1);
		f.filter(Z,1).filter(X,0);
		f.filter(Z,1).filter(X,1);

		f = f.reorderDomain(mcons.getFactor(X).getDomain().getVariables());

		mx.setFactor(X, f);

		//f.reorderDomain(mcons.getFactor(X).getDomain().getVariables())


		SparseModel vmcons =  mcons.toVCredal(empData.values());

		// not feasible as expected
		//mx.toVCredal(empData.values());

		vmcons.getFactor(Ux);


		EMCredalBuilder builder = new EMCredalBuilder(mx, data, empData)
				.setMaxEMIter(200)
				.setNumTrajectories(20)
				//.setNumDecimalsRound(-1)
				.build();


		CausalMultiVE inf2 = new CausalMultiVE(builder.getSelectedPoints());

		VertexFactor pn2 = (VertexFactor) inf2.probNecessity(X,Y);
		VertexFactor ps2 = (VertexFactor) inf2.probSufficiency(X,Y);
		VertexFactor pns2 = (VertexFactor) inf2.probNecessityAndSufficiency(X,Y,0,1);


		System.out.println("EM method: Ux ternary incompatible as in Ex2\n");
		System.out.println("====================");


		System.out.println("pn="+pn2);
		System.out.println("ps="+ps2);
		System.out.println("pns="+pns2);

		for(StructuralCausalModel m : builder.getSelectedPoints())
			System.out.println(m.getEmpiricalMap());


	}
}
