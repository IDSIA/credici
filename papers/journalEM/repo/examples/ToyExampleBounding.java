package repo.examples;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.util.HashMap;
import java.util.List;

public class ToyExampleBounding {
    public static void main(String[] args) throws ExecutionControl.NotImplementedException, InterruptedException {

        Logger logger = new Logger();

        int maxIter = 100;
        int executions = 10;
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
        empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,5);

        logger.info("Sampled complete data with size: "+data.length);


        // Determine the cause and the effect variables
        int cause = 0, effect = 1;

        // Credal vausal VE
        CredalCausalVE ccve = new CredalCausalVE(model, empiricalDist.values());
        VertexFactor pnsCCVE = ccve.probNecessityAndSufficiency(cause,effect);
        logger.info("CCVE result: \n"+ pnsCCVE);

        // Approx LP
        CredalCausalApproxLP  alp = new CredalCausalApproxLP(model, empiricalDist.values());
        IntervalFactor pnsALP = alp.probNecessityAndSufficiency(cause,effect);
        logger.info("CCALP result: \n"+ pnsALP);

        // EMCC
        EMCredalBuilder builder = EMCredalBuilder.of(model, data)
                .setMaxEMIter(maxIter)
                .setNumTrajectories(executions)
                .setWeightedEM(true)
                .build();
        CausalMultiVE multiVE = new CausalMultiVE(builder.getSelectedPoints());
        VertexFactor pnsEMCC = (VertexFactor) multiVE.probNecessityAndSufficiency(cause,effect);
        logger.info("EMCC result: \n"+ pnsEMCC);


    }
}
