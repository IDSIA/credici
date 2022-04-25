package code;

import ch.idsia.credici.IO;
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
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.credici.utility.experiments.Terminal;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;


import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ch.idsia.credici.utility.Assertion.assertTrue;


public class ModelDataGenerator extends Terminal {

	/*
	-o outputfolder
	-n nEndo
	-d datasize
	-m maxDist
	-t twExo
	-r reduction
	chain
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

	@CommandLine.Option(names = {"-t", "--exoTreewidth"}, description = "Exogenous treewidth: 0 for markovian and 1 for quasi-markovian. Defaults to 1")
	private int twExo = 1;

	@CommandLine.Option(names = {"-r", "--reduction"}, description = "Reduction constant for the exogenous domains.")
	private double reductionK = 1.0;

	@Override
	protected void checkArguments() {
		logger.info("Checking arguments");

		assertTrue( Arrays.asList("chain").stream().anyMatch(s -> s.equals(topology)), " Wrong value for topology: "+topology);
		assertTrue( nEndo>2, " Wrong value for nEndo: "+nEndo);
		assertTrue( dataSize>0, " Wrong value for dataSize: "+dataSize);
		assertTrue( twExo ==0 || twExo ==1, " Wrong value for twExo: "+ twExo);
		assertTrue( reductionK>=0.0 && reductionK<=1.0, " Wrong value for reductionK: "+reductionK);


	}

	private String getGenID(){
		String id = "";
		id += topology;
		id += "_twExo"+ twExo;
		if(twExo >0)
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
		plotStats();
		save();
	}

	private void save() throws IOException {
		String filename = getGenID();
		String fullpath;
		wdir = Paths.get(this.outputPath);


		fullpath = wdir.resolve(filename+".csv").toAbsolutePath().toString();
		logger.info("Saving data at:" +fullpath);
		DataUtil.toCSV(fullpath, data);

		fullpath = wdir.resolve(filename+".uai").toAbsolutePath().toString();
		logger.info("Saving model at:" +fullpath);
		IO.write(model, fullpath);

	}

	private void plotStats() {
		// Reduction
		logger.info("Resulting model:");
		logger.info("- Average U size: "+ StatisticsModel.of(model).avgExoCardinality());
		logger.info("- ExoDAG: "+model.getExogenousDAG());
		logger.info("- Number of U vars: "+model.getExogenousVars().length);
		logger.info("- U set: "+Arrays.toString(model.getExogenousVars()));
		logger.info("- Number of X vars: "+model.getEndogenousVars().length);
		logger.info("- X set: "+Arrays.toString(model.getEndogenousVars()));
		logger.info("- ExoTreewidth: "+model.getExogenousTreewidth());
		logger.info("- Markovian: "+ CausalInfo.of(model).isMarkovian());
	}

	private void transformAndSample() {
		int k = 0;
		do {
			k++;
			try {
				if((k % 5)==1) logger.info("Generating candidate model number "+k);
				// Create a model with cofounders
				StructuralCausalModel candidateModel = addCofounders(model);
				// sample data
				data = DataUtil.SampleCompatible(candidateModel, dataSize, 5);
				//Reduce the model
				if (data != null)
					model = reduce(candidateModel);
			}catch (Exception e) {}

		}while(data==null || !model.isCompatible(data));
		logger.info("Found compatibility in model "+k);
		logger.info(String.valueOf(model));
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
		int numMerged = (RandomUtil.getRandom().nextInt(pairsX.length-1) + 2) * twExo;
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
