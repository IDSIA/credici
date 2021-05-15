package neurips21.examples;

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

import java.util.HashMap;
import java.util.stream.IntStream;

public class PearlExample {
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
		StructuralCausalModel m_eqless = CausalBuilder.of(bnet)
				.build();

		m_eqless.fillExogenousWithRandomFactors(3);



		HashMap empData = DataUtil.getEmpiricalMap(m_eqless, data);
		empData = FactorUtil.fixEmpiricalMap(empData,6);

		System.out.println(empData);

		SparseModel vmodelPGM =  m_eqless.toVCredal(empData.values());

		System.out.println(m_eqless);

		System.out.println("vmodel PGM:");
		System.out.println(vmodelPGM);

		CredalCausalVE inf = new CredalCausalVE(m_eqless, empData.values());


		VertexFactor pn = (VertexFactor) inf.probNecessity(X,Y,0,1);
		VertexFactor ps = (VertexFactor) inf.probSufficiency(X,Y,0,1);
		VertexFactor pns = (VertexFactor) inf.probNecessityAndSufficiency(X,Y,0,1);


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

		//VertexFactor pn2 = (VertexFactor) inf2.probNecessity(X,Y);
		//VertexFactor ps2 = (VertexFactor) inf2.probSufficiency(X,Y);
		VertexFactor pns2 = (VertexFactor) inf2.probNecessityAndSufficiency(X,Y,0,1);

		//System.out.println("pn2="+pn2);
		//System.out.println("ps2="+ps2);
		System.out.println("pns2="+pns2);






	}
}
