package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.model.info.StatisticsModel;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.model.transform.ExogenousReduction;
import ch.idsia.credici.utility.Combinatorial;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.experiments.AsynQuery;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.InvokerWithTimeout;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.idsia.credici.utility.Assertion.assertTrue;


public class ModelDataGenerator extends Terminal {

	/*
	-o outputfolder
	-n nEndo
	-d datasize
	-m maxDist
	-mk markovianity
	-r reduction
	chain

	-n 10 -d 500 --mk 1 --query -r 0.8 chain
	 */


	@CommandLine.Parameters(description = "Topology of the endogenous model")
	private String topology;

	@CommandLine.Option(names={"-o", "--output"}, description = "Output folder for the results.")
	String outputPath = ".";

	@CommandLine.Option(names = {"-n", "--nEndo"}, description = "Number of endogenous variables. Default 5")
	private int nEndo = 5;

	@CommandLine.Option(names = {"-d", "--datasize"}, description = "Size of the sampled data. Default 1000")
	private int dataSize = 1000;

	@CommandLine.Option(names = {"-m", "--maxDist"}, description = "Maximum distance of the cofounded endogenous variables.")
	private int maxDist = 3;

	@CommandLine.Option(names = {"-t", "--timeout"}, description = "Timeout in seconds for running the queries. Default to 60.")
	private int timeout = 60;

	@CommandLine.Option(names = {"--mk", "--markovianity"}, description = "Markovianity: 0 for markovian and 1 for quasi-markovian. Defaults to 1")
	private int markovianity = 1;

	@CommandLine.Option(names = {"-r", "--reduction"}, description = "Reduction constant for the exogenous domains.")
	private double reductionK = 1.0;


	@CommandLine.Option(names={"--query"}, description = "Run and store ACE and PNS queries. Defaults to false")
	boolean query = false;

	String[][] queries = null;
	String[][] info = null;


	@Override
	protected void checkArguments() {
		logger.info("Checking arguments");

		assertTrue( Arrays.asList("chain").stream().anyMatch(s -> s.equals(topology)), " Wrong value for topology: "+topology);
		assertTrue( nEndo>2, " Wrong value for nEndo: "+nEndo);
		assertTrue( dataSize>0, " Wrong value for dataSize: "+dataSize);
		assertTrue( markovianity ==0 || markovianity ==1, " Wrong value for markovianity: "+ markovianity);
		assertTrue( reductionK>=0.0 && reductionK<=1.0, " Wrong value for reductionK: "+reductionK);


	}

	private String getGenID(){
		String id = "";
		id += topology;
		id += "_mk"+ markovianity;
		if(markovianity >0)
			id += "_maxDist"+maxDist;
		id += "_nEndo"+nEndo;
		id += "_k"+String.valueOf(reductionK).replace(".","");
		id += "_"+seed;
		return id;
	}

	// Global variables
	private static StructuralCausalModel model = null;
	private static TIntIntMap[] data = null;
	Path wdir = null;

	public static void main(String[] args) {
		argStr = String.join(";", args);
		CommandLine.run(new ModelDataGenerator(), args);
		if(errMsg!="")
			System.exit(-1);
	}



	@Override
	protected void entryPoint() throws IOException {

		wdir = Paths.get(".");
		RandomUtil.setRandomSeed(seed);
		logger.info("Starting logger with seed "+seed);
		RandomUtil.setRandomSeed(seed);

		buildBaseModel();
		transformAndSample();
		statistics();
		save();
	}

	private void save() throws IOException {
		String filename = getGenID();
		String fullpath;
		wdir = Paths.get(this.outputPath);


		fullpath = wdir.resolve(filename+".csv").toAbsolutePath().toString();
		logger.info("Saving data at:" +fullpath);
		DataUtil.toCSV(fullpath, data);

		fullpath = wdir.resolve(filename+"_info.csv").toAbsolutePath().toString();
		logger.info("Saving info at:" +fullpath);
		DataUtil.toCSV(fullpath, info);


		fullpath = wdir.resolve(filename+".uai").toAbsolutePath().toString();
		logger.info("Saving model at:" +fullpath);
		IO.write(model, fullpath);

		if(query) {
			fullpath = wdir.resolve(filename + "_queries.csv").toAbsolutePath().toString();
			logger.info("Saving queries at:" + fullpath);
			DataUtil.toCSV(fullpath, queries);
		}


	}

	private void statistics() {

		List<String> infoValues = new ArrayList<>();

		logger.info("Resulting model:");

		String avgExoCard = String.valueOf(StatisticsModel.of(model).avgExoCardinality());
		String numExoVars = String.valueOf(model.getExogenousVars().length);
		String numEndoVars = String.valueOf(model.getEndogenousVars().length);
		String markovianity = String.valueOf(this.markovianity);

		logger.info("- Average U size: "+ avgExoCard);
		logger.info("- ExoDAG: "+model.getExogenousDAG());
		logger.info("- Number of U vars: "+numExoVars);
		logger.info("- U set: "+Arrays.toString(model.getExogenousVars()));
		logger.info("- Number of X vars: "+numEndoVars);
		logger.info("- X set: "+Arrays.toString(model.getEndogenousVars()));
		logger.info("- ExoTreewidth: "+model.getExogenousTreewidth());
		logger.info("- Markovian: "+ CausalInfo.of(model).isMarkovian());


		if(query) {
			List<String[]> data = new ArrayList<>();
			data.add(new String[]{"topology", "avg_exo_card", "num_exo_vars", "num_endo_vars", "markovianity",});
			data.add(new String[]{topology, avgExoCard, numExoVars, numEndoVars, markovianity});
			info = data.toArray(String[][]::new);
		}

	}

	private void transformAndSample() {
		int k = 0;
		StructuralCausalModel candidateModel = null;
		do {
			k++;
			try {
				if((k % 5)==1) logger.info("Generating candidate model number "+k);
				// Create a model with cofounders
				candidateModel = addCofounders(model);
				// sample data
				data = DataUtil.SampleCompatible(candidateModel, dataSize, 5);
				//Reduce the model
				if (data != null) {
					candidateModel = reduce(candidateModel);
					if(query) {
						runQueries(candidateModel);
					}
				}

			}catch (Exception e) {
				logger.info("Exception when generating candidate model");
				data = null;
			}

		}while(data==null || !candidateModel.isCompatible(data));

		model = candidateModel;
		logger.info("Found compatibility in model "+k);

		logger.info(String.valueOf(model));
	}

	private void runQueries(StructuralCausalModel model) throws ExecutionControl.NotImplementedException, InterruptedException, ExecutionException, TimeoutException {

		// runQueries(CausalInference inf)
		List res = new ArrayList();
		VertexFactor p = null;
		double[] v = null;

		//todo: see for other topologies
		int[] endoOrder = DAGUtil.getTopologicalOrder(model.getNetwork(), model.getEndogenousVars());
		int cause = endoOrder[0];
		int effect = endoOrder[endoOrder.length-1];

		logger.info("Running queries with cause="+cause+" and effect="+effect);
		CredalCausalVE inf = new CredalCausalVE(model);

		res.add(cause);
		res.add(effect);


		AsynQuery.setArgs(inf, "ace", cause, effect);
		IntervalFactor ace = (IntervalFactor) new InvokerWithTimeout<GenericFactor>().run(AsynQuery::run, timeout);
		//IntervalFactor ace = (IntervalFactor) AsynQuery.run();
		res.add(ace.getDataLower()[0][0]);
		res.add(ace.getDataUpper()[0][0]);
		logger.info("ACE: "+Arrays.toString(new double[]{ace.getDataLower()[0][0], ace.getDataUpper()[0][0]}));

		AsynQuery.setArgs(inf, "pns", cause, effect);
		p = (VertexFactor) new InvokerWithTimeout<GenericFactor>().run(AsynQuery::run, timeout);
		v =  Doubles.concat(p.getData()[0]);
		if(v.length==1) v = new double[]{v[0],v[0]};
		Arrays.sort(v);
		for(double val : v) res.add(val);
		logger.info("PNS: "+Arrays.toString(v));

		List<String[]> data = new ArrayList<>();
		data.add(new String[]{"cause", "effect",  "ace_l", "ace_u","pns_l", "pns_u",});
		data.add((String[]) res.stream().map(i -> String.valueOf(i)).toArray(String[]::new));
		queries = data.toArray(String[][]::new);

		// return
		//return res.stream().mapToDouble(d -> (double)d).toArray();
	}

	private StructuralCausalModel reduce(StructuralCausalModel candidateModel) {

		logger.info("Reducing model with average U size: "+StatisticsModel.of(candidateModel).avgExoCardinality());
		logger.info("ExoDAG: "+candidateModel.getExogenousDAG());
		ExogenousReduction reducer = new ExogenousReduction(candidateModel, data)
				.removeRedundant()
				.removeWithZeroUpper();
		if(reductionK<1.0)
			reducer = reducer.removeWithZeroLower(reductionK);
		return reducer.getModel();
	}

	@NotNull
	private StructuralCausalModel addCofounders(StructuralCausalModel model) {
		// Select cofounders
		int[][] pairsX = Combinatorial.randomPairs(model.getEndogenousVars(), maxDist);
		int numMerged = (RandomUtil.getRandom().nextInt(pairsX.length-1) + 2) * markovianity;
		int[][] finalPairsX = pairsX;
		pairsX = IntStream.range(0, numMerged).mapToObj(i -> finalPairsX[i]).toArray(int[][]::new);
		StructuralCausalModel newModel = Cofounding.mergeExoParents(model, pairsX);
		// Fill marginal distributions
		newModel.fillExogenousWithRandomFactors(FactorUtil.DEFAULT_DECIMALS);
		return newModel;
	}

	private void buildBaseModel() {
		// Build markovian model
		if(topology.equals("chain")) {
			String endoArcs = IntStream.rangeClosed(1, nEndo - 1).mapToObj(i -> "(" + (i - 1) + "," + i + ")").collect(Collectors.joining(","));
			SparseDirectedAcyclicGraph endoDAG = DAGUtil.build(endoArcs);
			int Xsize = Arrays.stream(endoDAG.getVariables()).max().getAsInt() + 1;
			String exoArcs = Arrays.stream(endoDAG.getVariables()).mapToObj(x -> "(" + (x + Xsize) + "," + x + ")").collect(Collectors.joining(","));
			SparseDirectedAcyclicGraph causalDAG = DAGUtil.build(endoArcs + exoArcs);
			model = CausalBuilder.of(endoDAG, 2).setCausalDAG(causalDAG).build();
		}else{
			throw new IllegalArgumentException("Unknown topology: "+topology);
		}
	}

}
