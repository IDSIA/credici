package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.factor.EquationBuilder;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class Example2_3_PearlModel {
	public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException {

		String wdir = ".";
		String folder = Path.of(wdir, "papers/21why/").toString();

		BayesianNetwork bnet = new BayesianNetwork();

		int X = bnet.addVariable(2);
		int Y = bnet.addVariable(2);
		int Z = bnet.addVariable(2);

		bnet.addParents(X, Z);
		bnet.addParents(Y, X, Z);


		// Variable names
		HashMap varNames = new HashMap();
		varNames.put(Z,"Z");
		varNames.put(X,"X");
		varNames.put(Y,"Y");

		// states x, y, z = True = 1
		int x=1, y=1, z=1;

		// states x', y', z' = False = 0
		int x_=0, y_=0, z_=0;


		BayesianFactor counts = new BayesianFactor(bnet.getDomain(Z,X,Y));

		counts.setValue(2, z_, x_, y_);
		counts.setValue(114, z_, x_, y);
		counts.setValue(41, z_, x, y_);
		counts.setValue(313, z_, x, y);

		counts.setValue(107, z, x_, y_);
		counts.setValue(13, z, x_, y);
		counts.setValue(109, z, x, y_);
		counts.setValue(1, z, x, y);

		FactorUtil.print(counts.reorderDomain(Y,X,Z), varNames);

		int N = (int) counts.marginalize(X, Y, Z).getValueAt(0);
		Assert.assertEquals(700, N, 0);


		BayesianFactor nz = counts.marginalize(X, Y);
		BayesianFactor nxz =  counts.marginalize(Y);
		BayesianFactor nxy = counts.marginalize(Z);


		BayesianFactor px_z = nxz.divide(nz);
		BayesianFactor py_xz = counts.reorderDomain(0,1,2).divide(nxz.reorderDomain(X,Z));
		BayesianFactor pz = nz.scalarMultiply(1.0/N);
		BayesianFactor py_x = nxy.divide(nxy.marginalize(Y));
		BayesianFactor pxy = nxy.scalarMultiply(1.0/N);


		//BayesianFactor

		TIntIntMap[] data = DataUtil.dataFromCounts(counts);

		// Empirical Bayesian network
		bnet.setFactor(Z, pz);
		bnet.setFactor(Y, py_xz);
		bnet.setFactor(X, px_z);


		// Conservative SCM
		StructuralCausalModel conservSCM = CausalBuilder.of(bnet).build();
		BayesianFactor fx = EquationBuilder.of(conservSCM).fromVector(X, 1,0, 0,0, 0,1, 1,1);
		conservSCM.setFactor(X, fx);
		conservSCM.fillExogenousWithRandomFactors(3);


		// Empirical endogenous distribution from the data
		HashMap empiricalDist = DataUtil.getEmpiricalMap(conservSCM, data);
		empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,6);

		System.out.println("Empirical distribution");
		System.out.println(empiricalDist);

		// Save into disk the model and the data
		IO.write(conservSCM, folder+ "/examples/consPearl.uai");
		DataUtil.toCSV(folder+ "/examples/dataPearl.csv", data);



    /*
     Baseline method:
     Zaffalon, M., Antonucci, A., & Cabañas, R. (2020, February).
     Structural causal models are (solvable by) credal networks.
     In International Conference on Probabilistic Graphical Models
     (pp. 581-592). PMLR.
     */



		// Transformation to a H-model (constraints)
		SparseModel hmodelPGM =  conservSCM.toHCredal(empiricalDist.values());
		System.out.println("H-model PGM:");
		for(int v : hmodelPGM.getVariables())
			((SeparateHalfspaceFactor)hmodelPGM.getFactor(v)).printLinearProblem();

		// Transformation to a V-model (extreme points)
		SparseModel vmodelPGM =  conservSCM.toVCredal(empiricalDist.values());
		System.out.println(conservSCM);
		System.out.println("V-model PGM:");
		System.out.println(vmodelPGM);

		// Exact inference with variable elimination
		CredalCausalVE inf = new CredalCausalVE(conservSCM, empiricalDist.values());
		VertexFactor pns = (VertexFactor) inf.probNecessityAndSufficiency(X,Y,1,0);


		System.out.println("PGM exact method");
		System.out.println("====================");
		System.out.println("pns="+pns);



		/** Causal Expectation Maximization **/

		// Computation of the multiple EM executions
		EMCredalBuilder builder = EMCredalBuilder.of(conservSCM, data)
				.setMaxEMIter(200)
				.setNumTrajectories(20)
				.build();

		// Inference with the results from the EM exectuions
		CausalMultiVE inf2 = new CausalMultiVE(builder.getSelectedPoints());
		VertexFactor pns2 = (VertexFactor) inf2.probNecessityAndSufficiency(X,Y,1,0);
		System.out.println("pns2="+pns2);

	}
}
