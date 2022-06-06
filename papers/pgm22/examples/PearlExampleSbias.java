package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.CollectionTools;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PearlExampleSbias {
    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {


        int maxIter = 500;
        int executions = 100;

        Logger logger = new Logger();

        String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici";
        String folder = Path.of(wdir, "papers/pgm22/").toString();


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

        // Exact inference with variable elimination
        CredalCausalVE inf = new CredalCausalVE(model, empiricalDist.values());
        VertexFactor pns = (VertexFactor) inf.probNecessityAndSufficiency(X,Y,1,0);
        double[] res_exact = new double[]{pns.getData()[0][0][0], pns.getData()[0][1][0]};
        Arrays.sort(res_exact);
        logger.info("Exact interval (data-based) with linear programming:");
        logger.info("PNS = "+Arrays.toString(res_exact));


        int assigList[][] = new int[4][8];

        int assignments[] = new int[]{1,1,1,1, 1,1,1,1};
        int [] Sparents = model.getEndogenousVars();
        int idx;

        // Full dataset available
        assigList[0] = Ints.concat(assignments);


        // Hide data from patients that did not get the treatment and did not recover
        idx = model.getDomain(Sparents).getOffset(0, 0, 0); // x, y, z
        assignments[idx] = 0;
        idx = model.getDomain(Sparents).getOffset(0, 0, 1); // x, y, z
        assignments[idx] = 0;
        assigList[1] = Ints.concat(assignments);

        // Hide data from patients that did not get the treatment
        idx = model.getDomain(Sparents).getOffset(0, 1, 0); // x, y, z
        assignments[idx] = 0;
        idx = model.getDomain(Sparents).getOffset(0, 1, 1); // x, y, z
        assignments[idx] = 0;
        assigList[2] = Ints.concat(assignments);

        // No data
        assigList[3] = new int[]{0,0,0,0, 0,0,0,0};

        String[] description = new String[]{
                "Full dataset available",
                "No data from those that did not get the treatment and did not recover (X=0 & Y=0 hidden)",
                "No data from those that did not get the treatment. (X=0 hidden)",
                "No data at all"
        };

        int i = 0;
        for(int[] s : assigList) {

            RandomUtil.setRandomSeed(0);

            logger.info("--------------------");
            logger.info(description[i]);
            logger.info("assignment: "+ Arrays.toString(s));
            StructuralCausalModel modelBiased = SelectionBias.addSelector(model, Sparents, s);
            int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];
            TIntIntMap[] dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);
            int n1 = (int) Stream.of(dataBiased).filter(d -> d.get(selectVar) == 1).count();
            double ps1 = (1.0 * n1) / dataBiased.length;


            logger.info("Learning model with p(S=1)= "+((int)n1)+"/"+dataBiased.length+"="+ ps1);

            int[] trainable = Arrays.stream(modelBiased.getExogenousVars())
                    .filter(v -> !ArraysUtil.contains(selectVar, modelBiased.getChildren(v)))
                    .toArray();

            Watch.start();

            EMCredalBuilder builder = EMCredalBuilder.of(modelBiased, dataBiased)
                    .setMaxEMIter(maxIter)
                    .setNumTrajectories(executions)
                    .setWeightedEM(true)
                    .build();

            Watch.stopAndLog(logger, "Performed "+executions+" EM runs in ");
            long time_learn = Watch.getWatch().getTime();

            List endingPoints = builder.getSelectedPoints().stream().map(m -> {
                m = m.copy();
                m.removeVariable(m.getExogenousParents(selectVar)[0]);
                m.removeVariable(selectVar);
                return m;
            }).collect(Collectors.toList());

            CausalMultiVE multiInf = new CausalMultiVE(endingPoints);
            VertexFactor p = (VertexFactor) multiInf.probNecessityAndSufficiency(X, Y, 1, 0);
            double[] res = new double[]{p.getData()[0][0][0], p.getData()[0][1][0]};
            Arrays.sort(res);
            logger.info("PNS = "+Arrays.toString(res));
            i++;
        }

    }
}
