import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.IntStream;

public class EMstopCriteria {

    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {

        int maxEMIter = 500;
        int numPoints = 100;
        double threshold = 0.00001;
        FrequentistCausalEM.StopCriteria stopCriteria = FrequentistCausalEM.StopCriteria.KL; //LLratio or KL


        // Run this using the project path as working dir.
        String filename = "./papers/journalEM/models/synthetic/s1/random_mc2_n6_mid3_d1000_05_mr098_r10_17.uai";
        StructuralCausalModel model = (StructuralCausalModel) IO.read(filename);
        TIntIntMap[] data = DataUtil.fromCSV(filename.replace(".uai",".csv"));


        HashMap<Set<Integer>, BayesianFactor> empirical = FactorUtil.intMapToHashMap(DataUtil.getCFactorsSplittedMap(model, data));
        empirical =  FactorUtil.fixEmpiricalMap(empirical, 5);
        CredalCausalVE ve = new CredalCausalVE(model, empirical.values());

        int[] order = DAGUtil.getTopologicalOrder(model.getNetwork(), model.getEndogenousVars());
        int cause = order[0], effect = order[order.length-1];
        VertexFactor pns = ve.probNecessityAndSufficiency(cause, effect);

        System.out.println("Exact result");
        System.out.println(pns);


        Watch.start();

        EMCredalBuilder builder = EMCredalBuilder.of(model, data)
                .setMaxEMIter(maxEMIter)
                .setNumTrajectories(numPoints)
                .setWeightedEM(true)
                .setTrainableVars(model.getExogenousVars())
                .setThreshold(threshold)
                .setStopCriteria(stopCriteria)
                .build();

        long time = Watch.stop();

        int[] iterations = builder.getTrajectories().stream().mapToInt(t -> t.size() - 2).toArray();
        System.out.println("Iterations: "+Arrays.toString(iterations));
        int totalIter = IntStream.of(iterations).sum();
        System.out.println(totalIter+" iterations in "+time+" ms. => Time per iteration:"+(((double)time)/totalIter)+"ms.");


        CausalMultiVE multiVE = new CausalMultiVE(builder.getSelectedPoints());
        VertexFactor resEM = (VertexFactor) multiVE.probNecessityAndSufficiency(cause, effect);
        System.out.println("EM result");
        System.out.println(resEM);

        model.exoConnectComponents().toArray();  // 1,6,4

/*

        for(int t=0; t<tr.size()-2; t++) {
            double Rcurr = ((StructuralCausalModel) builder.getTrajectories().get(0).get(t)).ratioLogLikelihood(data);
            double Rcurr1 = ((StructuralCausalModel) builder.getTrajectories().get(0).get(t)).ratioLogLikelihood(data,1);
            double Rcurr6 = ((StructuralCausalModel) builder.getTrajectories().get(0).get(t)).ratioLogLikelihood(data,6);
            double Rcurr4 = ((StructuralCausalModel) builder.getTrajectories().get(0).get(t)).ratioLogLikelihood(data,4);
            System.out.println((Rcurr)+"\t\t"+(Rcurr1)+"\t\t"+(Rcurr6)+"\t\t"+(Rcurr4)+"\t" );
        }

*/

    }

}
