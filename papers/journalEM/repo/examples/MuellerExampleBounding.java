package repo.examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

/** Example with model from:
 *  Mueller, S., Li, A., Pearl, J., 2021. Causes of effects: Learning individual
 *  responses from population data. arXiv:2104.13730 .
 *
 */

public class MuellerExampleBounding {
    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {


        int maxIter = 100;
        int executions = 20;

        Logger logger = new Logger();

        // set here the repository path
        String folder = Path.of("./papers/journalEM/repo/").toString();


        int X = 0;
        int Y = 1;
        int Z = 2;

        // Variable names
        HashMap varNames = new HashMap();
        varNames.put(Z,"Z");
        varNames.put(X,"X");
        varNames.put(Y,"Y");

        // states x, y, z = True = 1
        int x=1, y=1, z=1;

        // states x', y', z' = False = 0
        int x_=0, y_=0, z_=0;


        // Conservative SCM
        StructuralCausalModel model = (StructuralCausalModel) IO.read(folder+"/models/literature/consPearl.uai");
        TIntIntMap[] data = DataUtil.fromCSV(folder+"/models/literature/dataPearl.csv");


        // Empirical endogenous distribution from the data
        HashMap empiricalDist = DataUtil.getEmpiricalMap(model, data);
        empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,6);

        // Determine the cause and the effect variables
        int cause = X, effect = Y;

        // Credal vausal VE
        CredalCausalVE ccve = new CredalCausalVE(model, empiricalDist.values());
        VertexFactor pnsCCVE = ccve.probNecessityAndSufficiency(cause,effect);
        logger.info("CCVE result: \n"+ pnsCCVE);

        // Approx LP
        CredalCausalApproxLP alp = new CredalCausalApproxLP(model, empiricalDist.values());
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
