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

public class PearlExample {
	public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException {

		String wdir = ".";
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


		BayesianFactor counts = new BayesianFactor(bnet.getDomain(Z,X,Y));
		//new double[]{1,	13,	313,114, 109, 107, 41, 2});


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


		// Empirical network
		bnet.setFactor(Z, pz);
		bnet.setFactor(Y, py_xz);
		bnet.setFactor(X, px_z);


		int[] exoVarSizes = IntStream.of(bnet.getVariables())
				.mapToObj(v -> bnet.getDomain(ArrayUtils.add(bnet.getParents(v), v)))
				.mapToInt(dom -> dom.getCombinations()+1)
				.toArray();





		// Equationless
		StructuralCausalModel m_eqless = CausalBuilder.of(bnet).build();

		BayesianFactor fx = EquationBuilder.of(m_eqless).fromVector(X, 1,0, 0,0, 0,1, 1,1);
		m_eqless.setFactor(X, fx);


		m_eqless.fillExogenousWithRandomFactors(3);


		m_eqless.getFactor(X).filter(X, x_).filter(Z, z_);
		m_eqless.getFactor(X).filter(X, x).filter(Z, z_);
		m_eqless.getFactor(X).filter(X, x_).filter(Z, z);
		m_eqless.getFactor(X).filter(X, x).filter(Z, z);





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

		//System.out.println("pn2="+pn2);
		//System.out.println("ps2="+ps2);
		System.out.println("pns2="+pns2);




	}
}
