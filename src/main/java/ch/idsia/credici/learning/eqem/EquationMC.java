package ch.idsia.credici.learning.eqem;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.opencsv.exceptions.CsvException;

import cern.colt.Arrays;
import ch.idsia.credici.inference.CausalInference;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.learning.WeightedCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.CredalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder.SelectionPolicy;
import ch.idsia.credici.model.io.uai.CausalUAIParser;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.logger.DetailedDotSerializer;
import ch.idsia.credici.utility.logger.Info;
import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.io.uai.UAIParser;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "eqem", description = "Equation inferring EM", version = "0.1")
public class EquationMC {

	@Option(names = { "-r", "--relaxed" }, description = "Strict mode")
	private boolean relaxed = false;

	@Option(names = { "-p", "--path" }, description = "Model path", required = true)
	private String path;

	@Option(names = { "-n", "--name" }, description = "Problem name", required = true)
	private String name;

	@Option(names = { "-h", "--help", "-?", "-help" }, usageHelp = true, description = "Display this help and exit")
	private boolean help;

	public StructuralCausalModel loadModel() throws IOException {
		return CausalUAIParser.read(path + "/" + name + ".uai");
	}

	public DoubleTable loadData() throws IOException, CsvException {
		TIntIntMap[] data = DataUtil.fromCSV(path + "/" + name + ".csv");
		return new DoubleTable(data);
	}

	public static void main(String[] args) throws IOException, CsvException, InterruptedException {
		EquationMC emc = new EquationMC();
		CommandLine cmd = new CommandLine(emc);
		cmd.parse(args);
		StructuralCausalModel model = emc.loadModel();
		var datatable = emc.loadData();
		DetailedDotSerializer.saveModel("test.png", new Info().model(model).data(datatable));

		int[] order = DAGUtil.getTopologicalOrder(model.getNetwork(), model.getEndogenousVars(true));
		int cause = order[0];
		int effect = order[order.length - 1];

		CausalVE trueinf = new CausalVE(model);
		var tmppns = trueinf.probNecessityAndSufficiency(cause, effect);
		double truepns = tmppns.getData()[0];
		System.out.println("True: " + truepns);
		

		
		
		var c = new Config().deterministic(false).numPSCMRuns(0).numRun(1000).numIterations(1000);
		
		TDoubleList pnss = run(model, datatable, -1, c, cause, effect);
	
		System.out.println(Arrays.toString(pnss.toArray()));

		System.out.println(pnss.min() + " - " + pnss.max());

		
		
		
		c = new Config().deterministic(true).numPSCMRuns(10).numRun(100).numIterations(1000);
		
		pnss = run(model, datatable, +0, c, cause, effect);
	
		System.out.println(Arrays.toString(pnss.toArray()));

		System.out.println(pnss.min() + " - " + pnss.max());

		
		
		var builder = EMCredalBuilder.of(model, datatable.toMap(false))
				.setWeightedEM(true)

				.setNumTrajectories(100)
				.setMaxEMIter(1000)
				.setSelPolicy(SelectionPolicy.LAST)
				.build();
		

		CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints())
				.setToInterval(true);
		VertexFactor res = (VertexFactor) inf.probNecessityAndSufficiency(cause, effect);
		
		System.out.println("\n\nResult running multiple precise VE:\n\n" + res);


	}
	
	public static TDoubleList run(StructuralCausalModel model, DoubleTable datatable, int sizedelta, Config config, int cause, int effect) throws InterruptedException {
		TIntIntMap sizes = new TIntIntHashMap();

		for (int u : model.getExogenousVars()) {
			int size = model.getSize(u);
			size = size + sizedelta;
			System.out.println(size);
			sizes.put(u, size);
		}
		
		EQEMLearner learner = new EQEMLearner(model, datatable, sizes, true, config);
		
		var xx = learner.run();
		xx.simplify();

		var iter = xx.sobolIterator();

		TDoubleList pnss = new TDoubleArrayList(100);

		for (int i = 0; i < 10000 && iter.hasNext(); ++i) {
			var info = iter.next();
			CausalVE cve = new CausalVE(info);
			var pns = cve.probNecessityAndSufficiency(cause, effect);
			double dpns = pns.getData()[0];
			pnss.add(dpns);
		}
		return pnss;
	}
}
