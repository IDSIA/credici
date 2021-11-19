package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class Example4_PearlRestrictedU {
	public static void main(String[] args) throws IOException, CsvException {


		String wdir = ".";
		String folder = Path.of(wdir, "papers/21why/").toString();


		int X = 0;
		int Y = 1;
		int Z = 2;


		// Conservative SCM
		StructuralCausalModel conservSCM = (StructuralCausalModel) IO.read(folder+"/examples/consPearl.uai");
		TIntIntMap[] data = DataUtil.fromCSV(folder+"/examples/dataPearl.csv");

		// Exogenous variables
		int U = conservSCM.getExogenousParents(X)[0];

		// Empirical endogenous distribution from the data
		HashMap empiricalDist = DataUtil.getEmpiricalMap(conservSCM, data);
		empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,6);

		System.out.println("Empirical distribution");
		System.out.println(empiricalDist);

		//Start from the non-conservative model
		StructuralCausalModel m_reduced = conservSCM.copy();
		// Modify the SEs and exogenous domains
		m_reduced.removeVariable(U);
		m_reduced.addVariable(U,3,true);
		m_reduced.addParents(X,U);

		BayesianFactor fx = EquationBuilder.of(m_reduced).fromVector(X, 1,0, 0,0, 0,1);
		m_reduced.setFactor(X, fx);
		m_reduced.setFactor(U, new BayesianFactor(m_reduced.getDomain(U), new double[]{0.677,0.000,0.323}));


		// Check compatibility
		System.out.println("Compatible: "+m_reduced.isCompatible(data, 6));

		// Try to solve the LP problems
		try {
			m_reduced.toVCredal(empiricalDist.values());
		}catch(Exception e){
			System.out.println("Exception: "+e.getMessage());
		}

	}
}
