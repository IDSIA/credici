package repo.examples;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.HashMap;
import java.util.List;

public class ToyExample {
    public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException {

        Logger logger = new Logger();

        int maxIter = 50;
        int executions = 5;
        RandomUtil.setRandomSeed(0);

        // Variables IDs
        int X = 0, Y = 1;
        int U = 2, V = 3;

        // Define endogenous DAG and complete DAG of the SCM
        SparseDirectedAcyclicGraph endoDAG = DAGUtil.build("(0,1)");
        SparseDirectedAcyclicGraph causalDAG = DAGUtil.build("(0,1),(2,0),(3,1)");

        // Create the SCM with random probabilities for the exogenous variables
        StructuralCausalModel model = CausalBuilder.of(endoDAG, 2).setCausalDAG(causalDAG).build();
        model.fillExogenousWithRandomFactors(5);

        logger.info("Model structure: "+model.getNetwork());
        logger.info("Model parameters: "+model);

        // Sample a complete dataset
        TIntIntMap[] data = model.samples(1000, model.getEndogenousVars());
        // Empirical endogenous distribution from the data
        HashMap empiricalDist = DataUtil.getEmpiricalMap(model, data);
        empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,6);

        logger.info("Sampled complete data with size: "+data.length);


        // Non available configurations for X,Y
        int[][] hidden_conf = new int[][]{{0,0},{1,1}};
        int[] Sparents = new int[]{X,Y};

        // Model with Selection Bias structure
        StructuralCausalModel modelBiased = SelectionBias.addSelector(model, Sparents, hidden_conf);
        int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];

        // Biased data
        TIntIntMap[] dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);

        logger.info("Running EMCC with selected data.");

        // Learn the model
        List endingPoints = SelectionBias.runEM(modelBiased, selectVar, dataBiased, maxIter, executions);

        // Run inference
        CausalMultiVE multiInf = new CausalMultiVE(endingPoints);
        VertexFactor p = (VertexFactor) multiInf.probNecessityAndSufficiency(X, Y);
        logger.info("PNS: ["+p.getData()[0][0][0]+", "+p.getData()[0][1][0]+"]");



    }
}
