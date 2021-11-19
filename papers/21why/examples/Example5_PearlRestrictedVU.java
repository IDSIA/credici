package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class Example5_PearlRestrictedVU {
	public static void main(String[] args) throws IOException, CsvException, ExecutionControl.NotImplementedException, InterruptedException {

		String wdir = ".";
		String folder = Path.of(wdir, "papers/21why/").toString();

		int X = 0;
		int Y = 1;
		int Z = 2;


		// states x, y, z = True = 1
		int x=1, y=1, z=1;

		// states x', y', z' = False = 0
		int x_=0, y_=0, z_=0;

		// Conservative SCM
		StructuralCausalModel conservSCM = (StructuralCausalModel)IO.read(folder+"/examples/consPearl.uai");
		TIntIntMap[] data = DataUtil.fromCSV(folder+"/examples/dataPearl.csv");

		// Exogenous variables
		int U = conservSCM.getExogenousParents(X)[0];
		int V = conservSCM.getExogenousParents(Y)[0];
		int W = conservSCM.getExogenousParents(Z)[0];

		// Variable names
		HashMap varNames = new HashMap();
		varNames.put(Z,"Z");
		varNames.put(X,"X");
		varNames.put(Y,"Y");
		varNames.put(U,"U");
		varNames.put(V,"V");
		varNames.put(W, "W");


		// Empirical endogenous distribution from the data
		HashMap empiricalDist = DataUtil.getEmpiricalMap(conservSCM, data);
		empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,6);

		System.out.println("Empirical distribution");
		System.out.println(empiricalDist);



		//Start from the non-conservative model
		StructuralCausalModel m_reduced = conservSCM.copy();
		// Modify the SEs and exogenous domains
		m_reduced.removeVariable(U);
		m_reduced.removeVariable(V);
		m_reduced.addVariable(U,3,true);
		m_reduced.addVariable(V,3,true);
		m_reduced.addParents(X,U);
		m_reduced.addParents(Y,V);

		BayesianFactor fx = EquationBuilder.of(m_reduced).fromVector(X, 1,0, 0,0, 0,1);
		m_reduced.setFactor(X, fx);
		BayesianFactor fy = EquationBuilder.of(m_reduced).fromVector(Y, 1,1,0,1, 1,1,1,0, 0,0,1,0);
		m_reduced.setFactor(Y, fy);

		// Set P(U)
		m_reduced.setFactor(W, new BayesianFactor(m_reduced.getDomain(W), new double[]{0.6714289999999999, 0.328571}));
		m_reduced.setFactor(V, new BayesianFactor(m_reduced.getDomain(V), new double[]{0.47, 0.439, 0.091}));
		m_reduced.setFactor(U, new BayesianFactor(m_reduced.getDomain(U), new double[]{0.677,0.0,0.323}));

		//
		FactorUtil.print(m_reduced.getFactor(Y).filter(Y, y).reorderDomain(Z,X,V),varNames);
		FactorUtil.print(m_reduced.getFactor(X).filter(X, x).reorderDomain(Z,U),varNames);




		// Check compatibility
		System.out.println("Compatible: "+m_reduced.isCompatible(data, 6));


		// Try to solve the LP problems
		try {
			m_reduced.toVCredal(empiricalDist.values());
		}catch(Exception e){
			System.out.println("Exception: "+e.getMessage());
		}




		/* Do-calculus in the non-compatible fully defined SCM */
		CausalVE cve = new CausalVE(m_reduced);
		double pns_nocompatible = cve.probNecessityAndSufficiency(X,Y, x,x_).getValueAt(0);


		double maxll = Probability.logLikelihood(empiricalDist, empiricalDist, 1);
		double mll = Probability.logLikelihood(m_reduced.getEmpiricalMap(), empiricalDist, 1);
		double ratio = maxll / mll;


		System.out.println("PNS in non-compatible model = "+pns_nocompatible);
		System.out.println("Bounds in the conservative model: [0.0, 0.01456314635]");
		System.out.println("Max LL by this data = "+maxll);
		System.out.println("LL by this model and data = "+mll);
		System.out.println("Ratio =  "+ratio);


	}
}
