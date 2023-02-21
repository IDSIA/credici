package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.credici.utility.reconciliation.SimpleIntegrator;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DrugGenderExampleHybrid {

    static int numruns = 300;
    static int maxiter = 300;

    // T = 0, S = 1, G = 2
    static int T = 0;  //  Treatment
    static int S = 1;  // Survival
    static int G = 2;  // Gender

    // states for G, T and S
    static int female = 1, drug = 1, survived = 1;
    static int male = 0, no_drug = 0, dead = 0;
    static TIntIntMap[] dataObs, dataDoDrug, dataDoNoDrug;


    static String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici";
    static String folder = Path.of(wdir, "papers/journalPGM/").toString();

    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {

        //for(int seed=0;seed<10; seed++)
        runEvol(0);
        //run(0);


        // todo: extend


    }


    private static void runEvol(int seed) throws IOException, CsvException, InterruptedException, ExecutionControl.NotImplementedException {
        System.out.println(seed);
        RandomUtil.setRandomSeed(seed);
        // Conservative SCM
        StructuralCausalModel model = (StructuralCausalModel) IO.read(folder + "/models/literature/consPearl.uai");
        model = Cofounding.mergeExoParents(model, new int[][]{{T,S}});

        // Define counts and data

        dataFromPaper(model);
        //dataFromPaper(model, ObservationBuilder.observe(G,female), model.getEndogenousVars());
        //dataFromPaper(model, ObservationBuilder.observe(G,male), model.getEndogenousVars());


        ///
        TIntIntMap[] interventions = null;
        TIntIntMap[][] datasets = null;

        interventions = new TIntIntMap[]{};
        datasets = new TIntIntMap[][]{};

        interventions = new TIntIntMap[]{};
        datasets = new TIntIntMap[][]{};

        int[][] assignments = {
                {0, 0, 0, 0, 0, 0, 0, 0},
     //         {0, 0, 0, 0, 1, 0, 0, 0},
                {0, 1, 0, 0, 1, 0, 0, 0},
         //       {0, 1, 0, 1, 1, 0, 0, 0},
                {0, 1, 0, 1, 1, 0, 1, 0},    // example
          //      {0, 1, 0, 1, 1, 0, 1, 1},
                {1, 1, 0, 1, 1, 0, 1, 1},
                {1, 1, 0, 1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1, 1}
        };



        List<double[]> res = new ArrayList<>();
        for (int[] sa : assignments) {
            res.add(calculatePNS("Observational", model, dataObs, interventions, datasets, sa));
        }

        String str = res.stream().map(r -> Arrays.toString(r)).collect(Collectors.joining(","));

        System.out.println("res = ["+str+"]");

    }






    private static void run(int seed) throws IOException, CsvException, InterruptedException, ExecutionControl.NotImplementedException {
        System.out.println(seed);
        RandomUtil.setRandomSeed(seed);
        // Conservative SCM
        StructuralCausalModel model = (StructuralCausalModel) IO.read(folder+"/models/literature/consPearl.uai");
        model = Cofounding.mergeExoParents(model, new int[][]{{T,S}});





        // Define counts and data
        dataFromPaper(model);
        //dataFromPaper(model, ObservationBuilder.observe(G,female), model.getEndogenousVars());
        //dataFromPaper(model, ObservationBuilder.observe(G,male), model.getEndogenousVars());
        //dataRandom(model);
        //dataFromObservational(model);

        System.out.println(DataUtil.getCFactorsSplittedMap(model, dataObs));

        ///
        TIntIntMap[] interventions= null;
        TIntIntMap[][] datasets = null;

        interventions = new TIntIntMap[]{};
        datasets = new TIntIntMap[][]{};


        interventions = new TIntIntMap[]{};
        datasets = new TIntIntMap[][]{};
        //calculateExactPNS("CCVE Observational", model, dataObs, interventions, datasets);
        calculatePNS("Observational", model, dataObs, interventions, datasets, null);


        int[][] hidden = {
                {no_drug, survived, male},
                {no_drug, dead, male},
                {drug, survived, female},
                {drug, dead, female},
                };
        int[] Sassig = SelectionBias.getAssignmentWithHidden(model, new int[]{T,S,G}, hidden);
        calculatePNS("Observational", model, dataObs, interventions, datasets, Sassig);

        interventions = new TIntIntMap[]{DataUtil.observe(T,drug), DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoDrug, dataDoNoDrug};
        TIntIntMap[] datasetX = DataUtil.vconcat(dataDoDrug, dataDoNoDrug);

        //calculatePNS("Observational + do(drug) + do(no_drug)", model, dataObs, interventions, datasets, null);
        //calculatePNS("Observational + do(drug) + do(no_drug)", model, dataObs, interventions, datasets, Sassig);
        calculatePNS("Observational + do(drug, no_drug)", model, dataObs, datasetX, null);
        calculatePNS("Observational + do(drug, no_drug)", model, dataObs, datasetX, Sassig);


        calculatePNSlocal("Observational + do(drug, no_drug)", model, dataObs, datasetX, null, 0);
        calculatePNSlocal("Observational + do(drug, no_drug)", model, dataObs, datasetX, Sassig, 0);
        calculatePNSlocal("Observational + do(drug, no_drug)", model, dataObs, datasetX, null, 1);
        calculatePNSlocal("Observational + do(drug, no_drug)", model, dataObs, datasetX, Sassig, 1);

        interventions = new TIntIntMap[]{DataUtil.observe(T,drug), DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoDrug, dataDoNoDrug};
        //calculatePNS("do(drug) + do(no_drug)", model, null, interventions, datasets, null);
        calculatePNS("do(drug, no_drug)", model, null, datasetX, null);




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
        dataFromPaper(model, null, model.getEndogenousVars());
    }



    private static void dataFromPaper(StructuralCausalModel model, TIntIntMap selection, int[] selectColumns) throws IOException {
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

        //// Interventional data do(T=drug)
        BayesianFactor countsDoDrug = new BayesianFactor(model.getDomain(T,G,S));
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,female, S,survived), 489);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,female, S,dead), 511);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,male, S,survived), 490);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,male, S,dead), 510);

        dataDoDrug = DataUtil.dataFromCounts(countsDoDrug);


        System.out.println("do_drug\t"+DataUtil.getCFactorsSplittedMap(model, dataDoDrug));;


        //// Interventional data do(T=no_drug)
        BayesianFactor countsDoNoDrug = new BayesianFactor(model.getDomain(T,G,S));
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,female, S,survived), 210);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,female, S,dead), 790);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,male, S,survived), 210);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,male, S,dead), 790);

        dataDoNoDrug = DataUtil.dataFromCounts(countsDoNoDrug);

        DataUtil.toCSV("./papers/journalPGM/data/dataObs.csv", dataObs);
        DataUtil.toCSV("./papers/journalPGM/data/dataDoDrug.csv", dataDoDrug);
        DataUtil.toCSV("./papers/journalPGM/data/dataDoNoDrug.csv", dataDoNoDrug);



        if(selection != null && selection.size()>0) {
            dataObs = DataUtil.selectByValue(dataObs, selection);
            dataDoDrug = DataUtil.selectByValue(dataDoDrug, selection);
            dataDoNoDrug = DataUtil.selectByValue(dataDoNoDrug, selection);
        }
        if(selectColumns.length>0){
            dataObs = DataUtil.selectColumns(dataObs, selectColumns);
            dataDoDrug = DataUtil.selectColumns(dataDoDrug, selectColumns);
            dataDoNoDrug = DataUtil.selectColumns(dataDoNoDrug, selectColumns);
        }




    }


    private static void dataRandom(StructuralCausalModel model) throws IOException {

        model.fillExogenousWithRandomFactors(5);
        dataObs = model.samples(10000, model.getEndogenousVars());

        dataDoDrug = model.intervention(T, drug).samples(1000, model.getEndogenousVars());
        dataDoNoDrug = model.intervention(T, no_drug).samples(1000, model.getEndogenousVars());

    }

    private static double[] calculatePNS(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets, int[] Sassig) throws InterruptedException, ExecutionControl.NotImplementedException {

        double ps1 = 1;

        DataIntegrator integrator = DataIntegrator.of(model);
        if(dataObs != null)
            integrator.setObservationalData(dataObs);

        for(int i = 0; i< interventions.length; i++)
            integrator.setData(datasets[i], interventions[i]);


        TIntIntMap[] dataExt = integrator.getExtendedData();
        StructuralCausalModel modelExt = integrator.getExtendedModel();



        if(Sassig != null) {
            // Integrate selection bias
            modelExt = SelectionBias.addSelector(modelExt, new int[]{T,S,G}, Sassig);
            int S = SelectionBias.findSelector(modelExt);
            dataExt = SelectionBias.applySelector(dataExt, modelExt, S);

            int N0 = (int) Arrays.stream(dataExt).filter(d -> d.containsKey(S) && d.get(S)==0).count();
            int N1 = (int) Arrays.stream(dataExt).filter(d -> d.containsKey(S) && d.get(S)==1).count();

            ps1 = (double)(N1)/(N0+N1);


        }




            RandomUtil.setRandomSeed(0);

            EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
            //        EMCredalBuilder builder = EMCredalBuilder.of(model, dataObs)
                    .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                    .setThreshold(0.0)
                    .setNumTrajectories(numruns)
                    .setWeightedEM(true)
                    .setVerbose(false)
                    .setMaxEMIter(maxiter);


            builder.build();
            List selectedPoints = builder.getSelectedPoints().stream().map(m -> m.subModel(model.getVariables())).collect(Collectors.toList());
            int u = model.getExogenousParents(T)[0];

            StructuralCausalModel m = (StructuralCausalModel) selectedPoints.get(0);
            SimpleIntegrator si = new SimpleIntegrator(m, dataObs, interventions, datasets);
            double llk = si.getExtendedModel().logLikelihood(si.getExtendedData());
            System.out.println(llk);

            CausalMultiVE inf = new CausalMultiVE(selectedPoints);
            VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);


            double pns_u = Math.max(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
            double pns_l = Math.min(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
            System.out.println("PNS=[" + pns_l + "," + pns_u + "]\t Datasets: " + description+" ps1 = " + ps1);


            return Doubles.concat(new double[]{ps1}, inf.getIndividualPNS(T, S, drug, no_drug, survived, dead));
    }


    private static double[] calculatePNS(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] datasetX, int[] Sassig) throws InterruptedException, ExecutionControl.NotImplementedException {

        double ps1 = 1;

        DataIntegrator integrator = DataIntegrator.of(model);
        if(dataObs != null)
            integrator.setObservationalData(dataObs);

        if(datasetX!=null)
            integrator.setData(datasetX, new int[] {T});


        TIntIntMap[] dataExt = integrator.getExtendedData();
        StructuralCausalModel modelExt = integrator.getExtendedModel();



        if(Sassig != null) {
            // Integrate selection bias
            modelExt = SelectionBias.addSelector(modelExt, new int[]{T,S,G}, Sassig);
            int S = SelectionBias.findSelector(modelExt);
            dataExt = SelectionBias.applySelector(dataExt, modelExt, S);

            int N0 = (int) Arrays.stream(dataExt).filter(d -> d.containsKey(S) && d.get(S)==0).count();
            int N1 = (int) Arrays.stream(dataExt).filter(d -> d.containsKey(S) && d.get(S)==1).count();

            ps1 = (double)(N1)/(N0+N1);


        }




        RandomUtil.setRandomSeed(0);

        EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
                //        EMCredalBuilder builder = EMCredalBuilder.of(model, dataObs)
                .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                .setThreshold(0.0)
                .setNumTrajectories(numruns)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(maxiter);


        builder.build();
        List selectedPoints = builder.getSelectedPoints().stream().map(m -> m.subModel(model.getVariables())).collect(Collectors.toList());
        int u = model.getExogenousParents(T)[0];


        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);


        double pns_u = Math.max(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
        double pns_l = Math.min(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
        System.out.println("PNS=[" + pns_l + "," + pns_u + "]\t Datasets: " + description+" ps1 = " + ps1);


        return Doubles.concat(new double[]{ps1}, inf.getIndividualPNS(T, S, drug, no_drug, survived, dead));
    }

    private static double[] calculatePNSlocal(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] datasetX, int[] Sassig, int localState) throws InterruptedException, ExecutionControl.NotImplementedException {



        double ps1 = 1;
        int localVar = G;

        DataIntegrator integrator = DataIntegrator.of(model, model.getExogenousParents(G)[0]);

        int study = 0;
        if(dataObs != null) {
            integrator.setObservationalData(dataObs, study);
            study++;
        }
        if(datasetX!=null) {
            integrator.setData(datasetX, new int[]{T}, study);
            study++;
        }

        TIntIntMap[] dataExt = integrator.getExtendedData(true);
        StructuralCausalModel modelExt = integrator.getExtendedModel(true);

        //System.out.println(Arrays.toString(DataUtil.variables(dataExt)));
        //Arrays.stream(dataExt).forEach();


        if(Sassig != null) {
            // Integrate selection bias
            modelExt = SelectionBias.addSelector(modelExt, new int[]{T,S,G}, Sassig);
            int S = SelectionBias.findSelector(modelExt);
            dataExt = SelectionBias.applySelector(dataExt, modelExt, S);

            int N0 = (int) Arrays.stream(dataExt).filter(d -> d.containsKey(S) && d.get(S)==0).count();
            int N1 = (int) Arrays.stream(dataExt).filter(d -> d.containsKey(S) && d.get(S)==1).count();

            ps1 = (double)(N1)/(N0+N1);


        }



        modelExt = modelExt.subModel(dataExt);


        RandomUtil.setRandomSeed(0);

        //System.out.println(modelExt.getExogenousVars().length+" exo vars");

        EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
                //        EMCredalBuilder builder = EMCredalBuilder.of(model, dataObs)
                .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                .setThreshold(0.0)
                .setNumTrajectories(numruns)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(maxiter);


        builder.build();
        List selectedPoints = builder.getSelectedPoints().stream().map(
                //m -> m.subModel(model.getVariables())
                m -> integrator.removeInterventionalFromMultiStudy(m,localState)
        ).collect(Collectors.toList());
        int u = model.getExogenousParents(T)[0];


        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);


        double pns_u = Math.max(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
        double pns_l = Math.min(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);

        String localtheta = "theta_I";
        if(localState==0) localtheta="theta_O";
        System.out.println("PNS=[" + pns_l + "," + pns_u + "]\t Datasets: " + description+" ps1 = " + ps1+"\t "+localtheta);


        return Doubles.concat(new double[]{ps1}, inf.getIndividualPNS(T, S, drug, no_drug, survived, dead));
    }

/*
    private static double[] calculatePNSlocal(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets, int[] Sassig, int localState) throws InterruptedException, ExecutionControl.NotImplementedException {

        double ps1 = 1;
        int localVar = G;

        DataIntegrator integrator = DataIntegrator.of(model, localVar);
        if(dataObs != null)
            integrator.setObservationalData(dataObs);

        for(int i = 0; i< interventions.length; i++)
            integrator.setData(datasets[i], interventions[i]);


        TIntIntMap[] dataExt = integrator.getExtendedData();
        StructuralCausalModel modelExt = integrator.getExtendedModel();



        if(Sassig != null) {
            // Integrate selection bias
            modelExt = SelectionBias.addSelector(modelExt, new int[]{T,S,G}, Sassig);
            int S = SelectionBias.findSelector(modelExt);
            dataExt = SelectionBias.applySelector(dataExt, modelExt, S);

            int N0 = (int) Arrays.stream(dataExt).filter(d -> d.containsKey(S) && d.get(S)==0).count();
            int N1 = (int) Arrays.stream(dataExt).filter(d -> d.containsKey(S) && d.get(S)==1).count();
            ps1 = (double)(N1)/(N0+N1);
        }




        RandomUtil.setRandomSeed(0);

        EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
                //        EMCredalBuilder builder = EMCredalBuilder.of(model, dataObs)
                .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                .setThreshold(0.0)
                .setNumTrajectories(numruns)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(maxiter);

        //if(dataObs==null)
        //    builder.setTrainableVars(model.getExogenousParents(G,S));

        builder.build();
        List selectedPoints = builder.getSelectedPoints().stream().map(
                m -> integrator.removeInterventionalFromMultiStudy(m,localState)
        ).collect(Collectors.toList());
        int u = model.getExogenousParents(T)[0];

        StructuralCausalModel m = (StructuralCausalModel) selectedPoints.get(0);
        SimpleIntegrator si = new SimpleIntegrator(m, dataObs, interventions, datasets);
        double llk = si.getExtendedModel().logLikelihood(si.getExtendedData());
        System.out.println(llk);

        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);


        double pns_u = Math.max(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
        double pns_l = Math.min(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);

        String localtheta = "theta_I";
        if(localState==0) localtheta="theta_O";


        System.out.println("PNS=[" + pns_l + "," + pns_u + "]\t Datasets: " + description+" ps1 = " + ps1+"\t "+localtheta);


        return Doubles.concat(new double[]{ps1}, inf.getIndividualPNS(T, S, drug, no_drug, survived, dead));
    }

*/
    private static void calculateExactPNS(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets) throws InterruptedException, ExecutionControl.NotImplementedException {


        TIntIntMap[] data = null;

        if(dataObs != null) {
            data = dataObs;
            for(int i = 0; i< interventions.length; i++)
                data = DataUtil.vconcat(data, datasets[i]);
        }else{
            data = DataUtil.vconcat(datasets);
        }



        CredalCausalVE inf = new CredalCausalVE(model, DataUtil.getEmpiricalMap(model, data).values());
        VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(T,S, 1, 0);


        //CredalCausalApproxLP inf = new CredalCausalApproxLP(model, DataUtil.getEmpiricalMap(model, data).values());
        //IntervalFactor ires_obs = (IntervalFactor) inf.probNecessityAndSufficiency(T,S, 1, 0);
        //System.out.println(ires_obs);

        double pns_u = Math.max(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
        double pns_l = Math.min(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
        System.out.println("PNS=[" + pns_l + "," + pns_u + "]\t Datasets: " + description);

    }


}
