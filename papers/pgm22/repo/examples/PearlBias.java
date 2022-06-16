package repo.examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.credici.utility.experiments.Watch;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
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

public class PearlBias {

    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {


        int maxIter = 500;
        int executions = 10;

        Logger logger = new Logger();
        String prjPath = ".";
        String wdir = Path.of(prjPath).toString();

        int X = 0;
        int Y = 1;
        int Z = 2;

        // states x, y, z = True = 1  female, treated, recover
        int x=1, y=1, z=1;

        // states x', y', z' = False = 0
        int x_=0, y_=0, z_=0;

        // Conservative SCM
        StructuralCausalModel model = (StructuralCausalModel) IO.read(wdir+"/models/literature/consPearl.uai");
        TIntIntMap[] data = DataUtil.fromCSV(wdir+"/models/literature/dataPearl.csv");

        // Empirical endogenous distribution from the data
        HashMap empiricalDist = DataUtil.getEmpiricalMap(model, data);
        empiricalDist = FactorUtil.fixEmpiricalMap(empiricalDist,6);

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


            Watch.start();
            List<StructuralCausalModel> endingPoints = SelectionBias.runEM(modelBiased, selectVar, dataBiased, maxIter, executions);
            Watch.stopAndLog(logger, "Performed " + executions + " EM runs in ");

            long time_learn = Watch.getWatch().getTime();

            CausalMultiVE multiInf = new CausalMultiVE(endingPoints);
            VertexFactor p = (VertexFactor) multiInf.probNecessityAndSufficiency(X, Y, 1, 0);

            double[] pns = multiInf.getIndividualPNS(X, Y, 1, 0);
            double[] interval = new double[]{Arrays.stream(pns).min().getAsDouble(), Arrays.stream(pns).max().getAsDouble()};
            logger.info("PNS = " + Arrays.toString(interval));

            HashMap finalEmpiricalDist = empiricalDist;
            double[] ratios = endingPoints.stream().mapToDouble(m -> Probability.ratioLogLikelihood(
                    m.getEmpiricalMap(true),
                    finalEmpiricalDist, 1
            )).toArray();

            TIntIntMap[] D1 = Arrays.stream(dataBiased).filter(d -> d.get(selectVar)==1).map(d -> DataUtil.select(d, Sparents)).toArray(TIntIntMap[]::new);



            HashMap r = new HashMap<>();
            r.put("size_data_visible", n1);
            r.put("hidden_states_ZXY", hidden_conf_str);

            for (int k = 0; k < pns.length; k++)
                r.put("pns_" + k, String.valueOf(pns[k]));

            results.add(r);
        }

        DataUtil.toCSV(Path.of(wdir,"./examples/peal_results.csv").toString(), results);
    }


}
