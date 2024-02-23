package ch.idsia.credici.model.eqmc;

import ch.idsia.credici.learning.eqem.Config;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class PearlMarkov {
	
	
	private int u_gender;
	private int u_treatment;
	private int u_recovery;
	
//	Gender (Z)	Treatment (X)	Recovery (Y)	#
//	0	0	0	2
//	0	0	1	114
//	0	1	0	41
//	0	1	1	313
//	1	0	0	107
//	1	0	1	13
//	1	1	0	109
//	1	1	1	1
	private DoubleTable createData() {
		DoubleTable table = new DoubleTable(new int[] { 0, 1, 2 });
		table.add(new int[] { 0, 0, 0 }, 2d);
		table.add(new int[] { 0, 0, 1 }, 114d);
		table.add(new int[] { 0, 1, 0 }, 41d);
		table.add(new int[] { 0, 1, 1 }, 313d);
		table.add(new int[] { 1, 0, 0 }, 107d);
		table.add(new int[] { 1, 0, 1 }, 13d);
		table.add(new int[] { 1, 1, 0 }, 109d);
		table.add(new int[] { 1, 1, 1 }, 1d);
		return table;
	}

	private StructuralCausalModel createModel() {
		StructuralCausalModel model = new StructuralCausalModel();
		int gender = model.addVariable(0, 2);
		int treatment = model.addVariable(1, 2);
		int recovery = model.addVariable(2, 2);

		u_gender = model.addVariable(2, true);
		u_treatment = model.addVariable(2, true);
		u_recovery = model.addVariable(2, true);

		model.addParents(gender, u_gender);
		model.addParents(treatment, gender, u_treatment);
		model.addParents(recovery, gender, treatment, u_recovery);

		return model;
	}

	public static void main(String[] args) throws InterruptedException {
		
		PearlMarkov sim = new PearlMarkov();
		
		var data = sim.createData();
		var model = sim.createModel();
		
		var canonical = Experiments.canonical(model);
		DetailedDotSerializer.saveModel("cano.png", new Info().model(canonical).data(data));
//		var ccve = Experiments.runccve(canonical, data, 0, 2);
//		System.out.println("ccve: " + Experiments.array2string(ccve));
//		
//		
		
		var emcc = Experiments.runemcc(canonical, data, 0, 2, 100);
		System.out.println("EMCC: " + Experiments.array2string(emcc));

		
		Config config_emcc = new Config().deterministic(false).freeEndogenous(false).numRun(5000).maxRun(30000).numIterations(10000).numPSCMRuns(0).numPSCMInterations(10000);
		var emmc = Experiments.runrelax(canonical, data, 0, 2, config_emcc, null, 10000, "scmem.png");
		System.out.println("EMCC: " + Experiments.array2string(emmc));

		TIntIntMap sizes = new TIntIntHashMap();
		sizes.put(sim.u_gender, 2);
		sizes.put(sim.u_treatment, 4);
		sizes.put(sim.u_recovery, 16);
		
		
		
// 100 runs
//		EMCC: 0,0.035
//		300 + 0 + 0
//		RELAX: 0.002,0.028
//	5000 runs 
//		1(5000); 2(5000); 3(5000)
//		RELAX: 0,0.028
		
		Config config_relax = new Config().deterministic(false).numRun(5000).maxRun(30000).numIterations(10000).numPSCMRuns(0).numPSCMInterations(10000);
		var relax = Experiments.runrelax(model, data, 0, 2, config_relax, sizes, 10000, "scmbn.png");
		System.out.println("RELAX: " + Experiments.array2string(relax));
		
//		0.03364706486183784,0.04963043245336572

		Config config_dete = new Config().deterministic(true).numRun(200).maxRun(30000).numIterations(10000).numPSCMRuns(20).numPSCMInterations(10000);
		var dete = Experiments.runrelax(model.copy(), data, 0, 2, config_dete, sizes, 10000, "scmem.png");
		System.out.println("DETE: " + Experiments.array2string(dete));
		
	}
}
