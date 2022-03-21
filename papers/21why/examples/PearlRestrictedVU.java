package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.IntStream;

public class PearlRestrictedVU {
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionControl.NotImplementedException {

		String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici";
		String folder = Path.of(wdir, "papers/21why/examples/").toString();

		BayesianNetwork bnet = new BayesianNetwork();

		int X = bnet.addVariable(2);
		int Y = bnet.addVariable(2);
		int Z = bnet.addVariable(2);

		bnet.addParents(X, Z);
		bnet.addParents(Y, X, Z);


		// states x, y, z = True = 1
		int x=1, y=1, z=1;

		// states x', y', z' = False = 0
		int x_=0, y_=0, z_=0;


		// Set data counts
		BayesianFactor counts = new BayesianFactor(bnet.getDomain(Z,X,Y));
		counts.setValue(2, z_, x_, y_);
		counts.setValue(114, z_, x_, y);
		counts.setValue(41, z_, x, y_);
		counts.setValue(313, z_, x, y);

		counts.setValue(107, z, x_, y_);
		counts.setValue(13, z, x_, y);
		counts.setValue(109, z, x, y_);
		counts.setValue(1, z, x, y);

		FactorUtil.print(counts);


		int N = (int) counts.marginalize(X, Y, Z).getValueAt(0);
		Assert.assertEquals(700, N, 0);

		// Compute the CPTs of the endogenous model
		BayesianFactor nz = counts.marginalize(X, Y);
		BayesianFactor nxz =  counts.marginalize(Y);
		BayesianFactor nxy = counts.marginalize(Z);
		BayesianFactor px_z = nxz.divide(nz);
		BayesianFactor py_xz = counts.reorderDomain(0,1,2).divide(nxz.reorderDomain(X,Z));
		BayesianFactor pz = nz.scalarMultiply(1.0/N);
		BayesianFactor py_x = nxy.divide(nxy.marginalize(Y));
		BayesianFactor pxy = nxy.scalarMultiply(1.0/N);


		//Generate a dataset with the require counts
		TIntIntMap[] data = DataUtil.dataFromCounts(counts);

		// Empirical network
		bnet.setFactor(Z, pz);
		bnet.setFactor(Y, py_xz);
		bnet.setFactor(X, px_z);


		// Conservative model
		StructuralCausalModel m_eqless = CausalBuilder.of(bnet).build();
		BayesianFactor fx = EquationBuilder.of(m_eqless).fromVector(X, 1,0, 0,0, 0,1, 1,1);
		m_eqless.setFactor(X, fx);
		m_eqless.fillExogenousWithRandomFactors(3);

		m_eqless.getFactor(X).filter(X, x_).filter(Z, z_);
		m_eqless.getFactor(X).filter(X, x).filter(Z, z_);
		m_eqless.getFactor(X).filter(X, x_).filter(Z, z);
		m_eqless.getFactor(X).filter(X, x).filter(Z, z);

		// Exogenous variables
		int U = m_eqless.getExogenousParents(X)[0];
		int V = m_eqless.getExogenousParents(Y)[0];
		int W = m_eqless.getExogenousParents(Z)[0];

		HashMap empData = DataUtil.getEmpiricalMap(m_eqless, data);
		empData = FactorUtil.fixEmpiricalMap(empData,6);

		System.out.println(empData);

		SparseModel vmodelPGM =  m_eqless.toVCredal(empData.values());
		System.out.println(m_eqless);
		System.out.println("vmodel PGM:");
		System.out.println(vmodelPGM);


		SparseModel hmodelPGM =  m_eqless.toHCredal(empData.values());
		System.out.println("hmodel PGM:");
		System.out.println(hmodelPGM);
		((SeparateHalfspaceFactor)hmodelPGM.getFactor(U)).printLinearProblem();


		CredalCausalVE inf = new CredalCausalVE(m_eqless, empData.values());
		IO.write(m_eqless, folder+"consPearl.uai");
		DataUtil.toCSV(folder+"dataPearl.csv", data);


		VertexFactor pn = (VertexFactor) inf.probNecessity(X,Y,1,0);
		VertexFactor ps = (VertexFactor) inf.probSufficiency(X,Y,1,0);
		VertexFactor pns = (VertexFactor) inf.probNecessityAndSufficiency(X,Y,1,0);


		System.out.println("PGM exact method");
		System.out.println("====================");
		System.out.println("pn="+pn);
		System.out.println("ps="+ps);
		System.out.println("pns="+pns);



		// EM
		for(int u: m_eqless.getExogenousVars())
			System.out.println(u+": "+m_eqless.getFactor(u));

		EMCredalBuilder builder = EMCredalBuilder.of(m_eqless, data)
				.setMaxEMIter(200)
				.setNumTrajectories(20)
				//.setNumDecimalsRound(-1)
				.build();


		CausalMultiVE inf2 = new CausalMultiVE(builder.getSelectedPoints());
		VertexFactor pns2 = (VertexFactor) inf2.probNecessityAndSufficiency(X,Y,1,0);
		System.out.println("pns2="+pns2);



		//Start from the non-conservative model
		StructuralCausalModel m_reduced = m_eqless.copy();
		// Modify the SEs and exogenous domains
		m_reduced.removeVariable(U);
		m_reduced.removeVariable(V);
		m_reduced.addVariable(U,3,true);
		m_reduced.addVariable(V,3,true);
		m_reduced.addParents(X,U);
		m_reduced.addParents(Y,V);

		fx = EquationBuilder.of(m_reduced).fromVector(X, 1,0, 0,0, 0,1);
		m_reduced.setFactor(X, fx);
		BayesianFactor fy = EquationBuilder.of(m_reduced).fromVector(Y, 0,0,1,0, 0,0,0,0, 1,1,0,1);
		m_reduced.setFactor(Y, fy);

		// Set P(U)
		m_reduced.setFactor(W, new BayesianFactor(m_reduced.getDomain(W), new double[]{0.6714289999999999, 0.328571}));
		m_reduced.setFactor(V, new BayesianFactor(m_reduced.getDomain(V), new double[]{0.091,0.448,0.462}));
		m_reduced.setFactor(U, new BayesianFactor(m_reduced.getDomain(U), new double[]{0.677,0.000,0.323}));

		m_reduced.getFactor(X);

		CausalVE cve = new CausalVE(m_reduced);
		double pns_nocompatible = cve.probNecessityAndSufficiency(X,Y, x,x_).getValueAt(0);



		double maxll = Probability.logLikelihood(empData, empData, 1);
		double mll = Probability.logLikelihood(m_reduced.getEmpiricalMap(), empData, 1);
		double ratio = maxll / mll;



		System.out.println("PNS in non-compatible model = "+pns_nocompatible);
		System.out.println("PNS intervals = "+pns );

		System.out.println("Max LL by this data = "+maxll);
		System.out.println("LL by this model and data = "+mll);
		System.out.println("Ratio =  "+ratio);


	}
}
