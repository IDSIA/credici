package examples;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Prototype2vars {


    static int Y = 0;
    static int  X = 1;  // Survival

    // states for G, T and S
    static TIntIntMap[] dataObs, dataY0, dataY1;



    static String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici";
    static String folder = Path.of(wdir, "papers/clear23/").toString();

    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {

        //for(int seed=0;seed<10; seed++)
        run(0);


        // todo: extend


    }

    private static void run(int seed) throws IOException, CsvException, InterruptedException, ExecutionControl.NotImplementedException {
        System.out.println(seed);
        RandomUtil.setRandomSeed(seed);
        // Conservative SCM

        StructuralCausalModel model = CausalBuilder.of(DAGUtil.build("(0,1)"),2)
                .setCausalDAG(DAGUtil.build("(0,1),(2,0),(3,1)")).build();
        //model = Cofounding.mergeExoParents(model, new int[][]{{Y,X}});

        // Define counts and data


        dataRandom(model);


        ///
        TIntIntMap[] interventions= null;
        TIntIntMap[][] datasets = null;



        interventions = new TIntIntMap[]{DataUtil.observe(Y,0)};
        datasets = new TIntIntMap[][]{dataY0};
        calculatePNS(" do(Y0)", model, null, interventions, datasets);


        interventions = new TIntIntMap[]{DataUtil.observe(Y,1)};
        datasets = new TIntIntMap[][]{dataY1};
        calculatePNS(" do(Y1)", model, null, interventions, datasets);

        interventions = new TIntIntMap[]{DataUtil.observe(Y,0), DataUtil.observe(Y,1)};
        datasets = new TIntIntMap[][]{dataY0, dataY1};
        calculatePNS(" do(Y0) + do(Y1)", model, null, interventions, datasets);


        interventions = new TIntIntMap[]{DataUtil.observe(Y,0), DataUtil.observe(Y,1)};
        datasets = new TIntIntMap[][]{dataY0, dataY1};
        calculatePNS("Observational + do(Y0) + do(Y1)", model, dataObs, interventions, datasets);

    }


    private static void dataRandom(StructuralCausalModel model) throws IOException {

        model.fillExogenousWithRandomFactors(5);
        dataObs = model.samples(2000, model.getEndogenousVars());
        dataY0 = model.intervention(Y, 0).samples(1000, model.getEndogenousVars());
        dataY1 = model.intervention(Y, 1).samples(1000, model.getEndogenousVars());

    }

    private static void calculatePNS(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets) throws InterruptedException, ExecutionControl.NotImplementedException {
        DataIntegrator integrator = DataIntegrator.of(model);
        if(dataObs != null)
            integrator.setObservationalData(dataObs);

        for(int i = 0; i< interventions.length; i++)
            integrator.setData(interventions[i], datasets[i]);


        TIntIntMap[] dataExt = integrator.getExtendedData();
        StructuralCausalModel modelExt = integrator.getExtendedModel();

        //System.out.println("datasize: "+dataExt.length);
        //System.out.println("model size: "+modelExt.getVariables().length);

        for(int s=0; s<1; s++) {
            RandomUtil.setRandomSeed(s);

            EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
                    //EMCredalBuilder builder = EMCredalBuilder.of(model, data)
                    .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                    .setThreshold(0.0)
                    .setNumTrajectories(50)
                    .setWeightedEM(true)
                    .setVerbose(false)
                    .setMaxEMIter(200);

            builder.build();


            List selectedPoints = builder.getSelectedPoints();//.stream().map(m -> integrator.removeInterventional(m)).collect(Collectors.toList());


            CausalMultiVE inf = new CausalMultiVE(selectedPoints);
            VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(Y, X, 1, 0);
            //System.out.println(description);
            double pns_u = Math.max(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
            double pns_l = Math.min(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
            System.out.println(s);
            System.out.println("PNS=[" + pns_l + "," + pns_u + "]\t Datasets: " + description);

        }
    }


}
