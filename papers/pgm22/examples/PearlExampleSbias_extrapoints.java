package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PearlExampleSbias_extrapoints {
    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {



        int maxIter = 500;
        int executions = 200;

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

        // states x, y, z = True = 1  female, treated, recover
        int x=1, y=1, z=1;


        // states x', y', z' = False = 0
        int x_=0, y_=0, z_=0;



        // Conservative SCM
        StructuralCausalModel model = (StructuralCausalModel) IO.read(folder+"/models/literature/consPearl.uai");
        TIntIntMap[] data = DataUtil.fromCSV(folder+"/models/literature/dataPearl.csv");



        // Empirical endogenous distribution from the data
        HashMap empiricalDist = DataUtil.getEmpiricalMap(model, data);
        empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,6);



        /// model, Sparents, list hidden_conf  -> assignment list


        int [] Sparents = new int[]{Z,X,Y};


        int[][][] mechanisims = new int[][][]{
                {},
                {{0,0,0},{0,0,1}},
                {{0,0,0},{0,0,1},{1,1,0},{1,1,1}},
                {{0,0,0},{0,0,1},{1,1,0},{1,1,1},{1,0,0},{1,0,1}},
                {{0,0,0},{0,0,1},{1,1,0},{1,1,1},{1,0,0},{1,0,1},{0,1,0}},
                {{0,0,0},{0,0,1},{1,1,0},{1,1,1},{1,0,0},{1,0,1},{0,1,0},{0,1,1}}
        };

        List results = new ArrayList();

        for(int[][] hidden_conf : mechanisims) {

            int[] s = SelectionBias.getAssignmentWithHidden(model, Sparents, hidden_conf);
            String hidden_conf_str = Arrays.stream(hidden_conf).map(c -> Arrays.toString(c).replace(",", "")).collect(Collectors.joining());

            RandomUtil.setRandomSeed(0);

            logger.info("--------------------");
            logger.info("assignment: " + Arrays.toString(s));
            logger.info("Hidden configurations for (Z,X,Y): " + hidden_conf_str);
            StructuralCausalModel modelBiased = SelectionBias.addSelector(model, Sparents, s);
            int selectVar = ArraysUtil.difference(modelBiased.getEndogenousVars(), model.getEndogenousVars())[0];
            TIntIntMap[] dataBiased = SelectionBias.applySelector(data, modelBiased, selectVar);
            int n1 = (int) Stream.of(dataBiased).filter(d -> d.get(selectVar) == 1).count();
            double ps1 = (1.0 * n1) / dataBiased.length;


            logger.info("Learning model");
            logger.info("p(S=0)= " + (dataBiased.length-n1) + "/" + dataBiased.length + "=" + (1-ps1));
            logger.info("p(S=1)= " + n1 + "/" + dataBiased.length + "=" + ps1);

            int[] trainable = Arrays.stream(modelBiased.getExogenousVars())
                    .filter(v -> !ArraysUtil.contains(selectVar, modelBiased.getChildren(v)))
                    .toArray();

            Watch.start();

            EMCredalBuilder builder = EMCredalBuilder.of(modelBiased, dataBiased)
                    .setMaxEMIter(maxIter)
                    .setNumTrajectories(executions)
                    .setWeightedEM(true)
                    .build();

            Watch.stopAndLog(logger, "Performed " + executions + " EM runs in ");
            long time_learn = Watch.getWatch().getTime();

            List endingPoints = builder.getSelectedPoints().stream().map(m -> {
                m = m.copy();
                m.removeVariable(m.getExogenousParents(selectVar)[0]);
                m.removeVariable(selectVar);
                return m;
            }).collect(Collectors.toList());

            CausalMultiVE multiInf = new CausalMultiVE(endingPoints);
            VertexFactor p = (VertexFactor) multiInf.probNecessityAndSufficiency(X, Y, 1, 0);

            double[] pns = multiInf.getIndividualPNS(X, Y, 1, 0);
            double[] interval = new double[]{Arrays.stream(pns).min().getAsDouble(), Arrays.stream(pns).max().getAsDouble()};
            logger.info("PNS = " + Arrays.toString(interval));

            HashMap r = new HashMap<>();
            r.put("size_data_visible", n1);
            r.put("hidden_states_ZXY", hidden_conf_str);

            for (int k = 0; k < pns.length; k++)
                r.put("pns_" + k, String.valueOf(pns[k]));

            results.add(r);
        }

        DataUtil.toCSV(Path.of(folder,"examples/peal_results.csv").toString(), results);
    }
}
