package code;

import ch.idsia.credici.IO;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.ChainGenerator;
import ch.idsia.credici.model.builder.ReverseHMM;
import ch.idsia.credici.model.transform.ExogenousReduction;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class modelGen {

	static String wdir = "./";
	static String modelFolder = "papers/journalEM/models/";
	static String set = "a";
	static String fcount = "";
	static Boolean overwrite = true;
	static int datasize = 1000;


	public static void main(String[] args) throws IOException, InterruptedException {

		// set1
		String[] topologies = new String[]{"chain"};
		int[] treeWidthExo = new int[]{1}; //0,1
		int[] numEndogenous = new int[]{5,7,10};
		double[] reduction = {0.25, 0.75};
		int[] index = IntStream.range(0,20).toArray();



		for(String top : topologies){
					for(int idx: index){
						for(int nEndo: numEndogenous){
							for(int twExo : treeWidthExo){
								for(double k : reduction) {
									buildModel(top, twExo, nEndo,k, idx);
								}
					}
				}
			}
		}

	}

	private static void buildModel(String top, int twExo, int nEndo, double reduction, int idx) throws IOException {
		boolean feasible = true;

		StructuralCausalModel m = null;
		RandomUtil.setRandomSeed((top+"_twExo"+twExo+"_nEndo"+nEndo+"_"+idx).hashCode());
		int maxDist = 2;

		boolean doubleCard = false;
		if(twExo==1)
			doubleCard = false;


		String reductionStr = String.valueOf(reduction).replace(".","");
		String name = top+"_twExo"+twExo+"_nEndo"+nEndo+"_k"+reductionStr+"_"+idx;
		String filename = wdir+modelFolder+set+twExo+fcount+"/"+name;
		System.out.println("\nGenerating:");
		System.out.println(filename);

		if(!overwrite && new File(filename+".uai").exists())
			return;

		// Random DAG
		if(top == "rhmm") {
			m = new ReverseHMM(nEndo, twExo).setMaxDist(maxDist).setDoubleCard(doubleCard).build();
			//m = ReverseHMM.build(nEndo, twExo, maxDist);
		}
		else // chain
			m = ChainGenerator.build(nEndo, twExo, maxDist);



		// Conservative specification
		SparseDirectedAcyclicGraph endoDag = DAGUtil.getSubDAG(m.getNetwork(), m.getEndogenousVars());
		m = CausalBuilder.of(endoDag, 2).setCausalDAG(m.getNetwork()).build();
		m.fillExogenousWithRandomFactors(5);

		System.out.println(m.getNetwork());
		System.out.println(m.getExogenousDAG());

		TIntIntMap[] data = m.samples(10000, m.getEndogenousVars());
		HashMap empProbs = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(m, data),10);

		System.out.println(m.isCompatible(data,3));
		System.out.println(m.isCompatible(data,5));
		System.out.println(m.isCompatible(data,8));
		System.out.println(m.isCompatible(data,20));

		// Reduce the model
		StructuralCausalModel reducedModel =
				new ExogenousReduction(m, empProbs)
						.removeWithZeroLower(reduction)
						.removeWithZeroUpper().getModel();


		// SparseModel vmodel = m.toVCredal(empProbs.values());

		double usize = IntStream.of(reducedModel.getExogenousVars())
						.map(exoVar -> reducedModel.getDomain(exoVar).getCardinality(exoVar))
						.sum() * 1.0 / m.getExogenousVars().length;


		System.out.println("tw="+reducedModel.getExogenousTreewidth());
		System.out.println(reducedModel.getExogenousDAG());
		System.out.println("Average U-size:"+usize);

		System.out.println(filename+".uai");
		IO.write(reducedModel, filename+".uai");

		if(data != null) {
			System.out.println(filename + ".csv");
			DataUtil.toCSV(filename+".csv", data);
		}

	}
}
