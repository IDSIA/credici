package neurips21.examples;

import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.stream.IntStream;

public class PearlExampleRestrictedU_3 {
	public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException {



		BayesianNetwork bnet = new BayesianNetwork();

		int X = bnet.addVariable(2);
		int Y = bnet.addVariable(2);
		int Z = bnet.addVariable(2);

		bnet.addParents(X, Z);
		bnet.addParents(Y, X, Z);


		// states x, y, z = True = 0
		int x=0, y=0, z=0;

		// states x', y', z' = False = 1
		int x_=1, y_=1, z_=1;


		BayesianFactor counts = new BayesianFactor(bnet.getDomain(X, Z, Y),
				new double[]{1,	13,	313,114, 109, 107, 41, 2});


		int N = (int) counts.marginalize(X, Y, Z).getValueAt(0);

		BayesianFactor nz = counts.marginalize(X, Y);
		BayesianFactor nxz =  counts.marginalize(Y);
		BayesianFactor nxy = counts.marginalize(Z);


		BayesianFactor px_z = nxz.divide(nz);
		BayesianFactor py_xz = counts.divide(nxz);
		BayesianFactor pz = nz.scalarMultiply(1.0/N);
		BayesianFactor py_x = nxy.divide(nxy.marginalize(Y));
		BayesianFactor pxy = nxy.scalarMultiply(1.0/N);


		//BayesianFactor

		TIntIntMap[] data = DataUtil.dataFromCounts(counts);


		// Empirical network
		bnet.setFactor(Z, pz);
		bnet.setFactor(Y, py_xz);
		bnet.setFactor(X, px_z);


		int[] exoVarSizes = IntStream.of(bnet.getVariables())
				.mapToObj(v -> bnet.getDomain(ArrayUtils.add(bnet.getParents(v), v)))
				.mapToInt(dom -> dom.getCombinations()+1)
				.toArray();

		// Equationless
		StructuralCausalModel smodel = CausalBuilder.of(bnet)
				.build();

		int Ux = smodel.getExogenousParents(X)[0];
		int Uy = smodel.getExogenousParents(Y)[0];
		int Uz = smodel.getExogenousParents(Z)[0];


		smodel.removeVariable(Ux);
		// Add Ux with new cardinality
		smodel.addVariable(Ux, 2, true);
		smodel.addParents(X,Ux,Z);
		// Add new eq fx
		BayesianFactor fx = EquationBuilder.of(smodel).fromVector(X, 0,1,1,0);
		smodel.setFactor(X, fx);



		smodel.removeVariable(Uy);
		// Add Uz with new cardinality
		smodel.addVariable(Uy, 2, true);
		smodel.addParents(Y,X,Z,Uy);
		// Add new eq fz
		BayesianFactor fy = EquationBuilder.of(smodel).fromVector(Y, 0,1,0,0,  1,0,1,0);
		smodel.setFactor(Y, fy);


		smodel.fillExogenousWithRandomFactors(3);



		HashMap empData = DataUtil.getEmpiricalMap(smodel, data);
		empData = FactorUtil.fixEmpiricalMap(empData,6);

		System.out.println(empData);


		for(int v: smodel.getEndogenousVars()) {
			System.out.println("");
			FactorUtil.print(smodel.getFactor(v));
		}
		for(int i=0; i<empData.keySet().size(); i++) {
			System.out.println("");

			FactorUtil.print((BayesianFactor) empData.get(empData.keySet().toArray()[i]));
		}




		// EM

		EMCredalBuilder builder = EMCredalBuilder.of(smodel, data)
				.setMaxEMIter(200)
				.setNumTrajectories(20)
				//.setNumDecimalsRound(-1)
				.build();




		CausalMultiVE inf2 = new CausalMultiVE(builder.getSelectedPoints());

		// Get one of the precise models

		VertexFactor pns2 = (VertexFactor) inf2.probNecessityAndSufficiency(X, Y, 0, 1);
		System.out.println("\nInterval PSN=" + pns2);

		System.out.println("True empirical:");
		System.out.println(empData);

		System.out.println("Generated distribution by each precise model:");
		for(int i=0; i<inf2.getInferenceList().size(); i++) {
			CausalVE inf_i = inf2.getInferenceList().get(i);
			StructuralCausalModel m_i = inf_i.getModel();
/*
			System.out.println("Precise model");
			for (int u : m_i.getExogenousVars())
				FactorUtil.print(m_i.getFactor(u));
*/

			//System.out.println("pns_"+i+"= " + inf_i.probNecessityAndSufficiency(X, Y)+" "+builder.getTrajectories().get(i).size());
			System.out.println(m_i.getEmpiricalMap());
			//VertexFactor pn2 = (VertexFactor) inf2.probNecessity(X,Y);
			//VertexFactor ps2 = (VertexFactor) inf2.probSufficiency(X,Y);



			//System.out.println("pn2="+pn2);
			//System.out.println("ps2="+ps2);
		}


		CausalVE inf_i = inf2.getInferenceList().get(16);
		StructuralCausalModel m_i = inf_i.getModel();

		System.out.println("Precise model");
		for (int u : m_i.getExogenousVars())
			FactorUtil.print(m_i.getFactor(u));

		System.out.println(inf_i.probNecessityAndSufficiency(X,Y));




		/// Rounding m_i

		double t = 0.33333;
		m_i.getFactor(Ux).setData(new double[]{1./3, 2./3});
		m_i.getFactor(Uy).setData(new double[]{9./10, 1./10});
		m_i.getFactor(Uz).setData(new double[]{1./3, 2./3});

		System.out.println("Rounded Precise model");
		for(int u : m_i.getExogenousVars())
			FactorUtil.print(m_i.getFactor(u));

		CausalVE ve = new CausalVE(m_i);


		HashMap emp_i = m_i.getEmpiricalMap();

		for(int i=0; i<emp_i.keySet().size(); i++) {
			System.out.println("");
			FactorUtil.print((BayesianFactor) emp_i.get(empData.keySet().toArray()[i]));
		}

		System.out.println("Rounded model PSN:"+ve.probNecessityAndSufficiency(X,Y));





/*
		// Check EM convergency with that initialization

		FrequentistEM em = new FrequentistEM(m_i)
				.setTrainableVars(smodel.getExogenousVars())
				.setRecordIntermediate(true)
				.setStopAtConvergence(true)
				.setVerbose(false)
				.setRegularization(0.0)
				.setKlthreshold(0.0);


		em.run((Collection) Arrays.asList(data),200);

		System.out.println("iterations = "+em.getIntermediateModels().size());
		for(BayesianFactor f : em.getPosterior().getFactors())
			FactorUtil.print(f);

*/
	}
}



