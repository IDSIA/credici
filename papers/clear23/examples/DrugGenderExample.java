package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.DataIntegrator;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToInterval;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DrugGenderExample {


    static int T = 0;  //  Treatment
    static int S = 1;  // Survival
    static int G = 2;  // Gender

    // states for G, T and S
    static int female=1, drug=1, survived=1;
    static int male=0, no_drug=0, dead=0;
    static TIntIntMap[] dataObs, dataDoDrug, dataDoNoDrug;



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
        StructuralCausalModel model = (StructuralCausalModel) IO.read(folder+"/models/literature/consPearl.uai");
        //model = Cofounding.mergeExoParents(model, new int[][]{{T,S}});

        // Define counts and data

        dataFromPaper(model);
        //dataRandom(model);
        //dataFromObservational(model);


        ///
        TIntIntMap[] interventions= null;
        TIntIntMap[][] datasets = null;


        interventions = new TIntIntMap[]{};
        datasets = new TIntIntMap[][]{};
        //calculatePNS("Observational", model, dataObs, interventions, datasets);


        interventions = new TIntIntMap[]{DataUtil.observe(T,drug), DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoDrug, dataDoNoDrug};
        calculatePNS("Observational + do(drug) + do(no_drug)", model, dataObs, interventions, datasets);


        interventions = new TIntIntMap[]{DataUtil.observe(T,drug), DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoDrug, dataDoNoDrug};
        calculatePNS("do(drug) + do(no_drug)", model, null, interventions, datasets);

        interventions = new TIntIntMap[]{DataUtil.observe(T,drug)};
        datasets = new TIntIntMap[][]{dataDoDrug};
        calculatePNS("do(drug)", model, null, interventions, datasets);

        interventions = new TIntIntMap[]{DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoNoDrug};
        calculatePNS("do(no_drug)", model, null, interventions, datasets);



        interventions = new TIntIntMap[]{DataUtil.observe(T,drug)};
        datasets = new TIntIntMap[][]{dataDoDrug};
        calculatePNS("Observational + do(drug)", model, dataObs, interventions, datasets);

        interventions = new TIntIntMap[]{DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoNoDrug};
        calculatePNS("Observational + do(no_drug)", model, dataObs, interventions, datasets);
    }

    private static void dataFromObservational(StructuralCausalModel model) throws IOException, CsvException, InterruptedException {

        dataObs = DataUtil.fromCSV(folder+"/models/literature/dataPearl.csv");
        EMCredalBuilder builder = EMCredalBuilder.of(model, dataObs)
                //EMCredalBuilder builder = EMCredalBuilder.of(model, data)
                .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                .setThreshold(0.0)
                .setNumTrajectories(1)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(200);
        builder.build();
        StructuralCausalModel m = builder.getSelectedPoints().get(0);



        dataObs = m.samples(2000, model.getEndogenousVars());
        dataDoDrug = m.intervention(T, drug).samples(1000, model.getEndogenousVars());
        dataDoNoDrug = m.intervention(T, no_drug).samples(1000, model.getEndogenousVars());
        System.out.println("sampled data interventional");
    }

    private static void dataFromPaper(StructuralCausalModel model) throws IOException {
        //// Observational data
        BayesianFactor countsObs = new BayesianFactor(model.getDomain(T,G,S));
        FactorUtil.setValue(countsObs, DataUtil.observe(T,drug, G,female, S,survived), 378);
        FactorUtil.setValue(countsObs, DataUtil.observe(T,drug, G,female, S,dead), 1022);
        FactorUtil.setValue(countsObs, DataUtil.observe(T,drug, G,male, S,survived), 980);
        FactorUtil.setValue(countsObs, DataUtil.observe(T,drug, G,male, S,dead), 420);
        FactorUtil.setValue(countsObs, DataUtil.observe(T,no_drug, G,female, S,survived), 420);
        FactorUtil.setValue(countsObs, DataUtil.observe(T,no_drug, G,female, S,dead), 180);
        FactorUtil.setValue(countsObs, DataUtil.observe(T,no_drug, G,male, S,survived), 420);
        FactorUtil.setValue(countsObs, DataUtil.observe(T,no_drug, G,male, S,dead), 180);

        dataObs = DataUtil.dataFromCounts(countsObs);
        DataUtil.toCSV(folder+"/models/literature/dataPearlObs.csv", dataObs);
        System.out.println(DataUtil.getCFactorsSplittedMap(model, dataObs));;

        //// Interventional data do(T=drug)
        BayesianFactor countsDoDrug = new BayesianFactor(model.getDomain(T,G,S));
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,female, S,survived), 489);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,female, S,dead), 511);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,male, S,survived), 490);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,male, S,dead), 510);

        dataDoDrug = DataUtil.dataFromCounts(countsDoDrug);
        DataUtil.toCSV(folder+"/models/literature/dataPearlDoDrug.csv", dataDoDrug);
        System.out.println(DataUtil.getCFactorsSplittedMap(model, dataDoDrug));;


        //// Interventional data do(T=no_drug)
        BayesianFactor countsDoNoDrug = new BayesianFactor(model.getDomain(T,G,S));
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,female, S,survived), 210);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,female, S,dead), 790);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,male, S,survived), 210);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,male, S,dead), 790);

        dataDoNoDrug = DataUtil.dataFromCounts(countsDoNoDrug);
        DataUtil.toCSV(folder+"/models/literature/dataPearlDoNoDrug.csv", dataDoNoDrug);
        System.out.println(DataUtil.getCFactorsSplittedMap(model, dataDoNoDrug));;

    }


    private static void dataRandom(StructuralCausalModel model) throws IOException {

        model.fillExogenousWithRandomFactors(5);
        dataObs = model.samples(10000, model.getEndogenousVars());

        dataDoDrug = model.intervention(T, drug).samples(1000, model.getEndogenousVars());
        dataDoNoDrug = model.intervention(T, no_drug).samples(1000, model.getEndogenousVars());

    }

    private static void calculatePNS(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets) throws InterruptedException, ExecutionControl.NotImplementedException {

//        System.out.println("\n\n===="+description+"======");

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
                    .setNumTrajectories(30)
                    .setWeightedEM(true)
                    .setVerbose(false)
                    .setMaxEMIter(200);

            //if(dataObs==null)
            //    builder.setTrainableVars(model.getExogenousParents(G,S));

            builder.build();
            List selectedPoints = builder.getSelectedPoints().stream().map(m -> integrator.removeInterventional(m)).collect(Collectors.toList());
            int u = model.getExogenousParents(T)[0];
/*
            if(dataObs==null)
                builder.getSelectedPoints().stream().forEach(m -> m.randomizeExoFactor(u, 5));


            System.out.println("Intevals for exoparent of T:"+
                    IntervalFactor.mergeBounds(builder.getSelectedPoints().stream().map(m -> new BayesianToInterval().apply(m.getFactor(u), u)).toArray(IntervalFactor[]::new))
            );

*/
            CausalMultiVE inf = new CausalMultiVE(selectedPoints);
            //VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(G, S, female, male, survived, dead);
            VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);

            // P(survival_{G=female}, death_{G=male})
            // P(survival_{drug}, death_{nodrug} )
            //P(survival_{drug}, death_{nodrug} | )

            //System.out.println(description);
            double pns_u = Math.max(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
            double pns_l = Math.min(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
            System.out.println("PNS=[" + pns_l + "," + pns_u + "]\t Datasets: " + description);

        }
    }


}
