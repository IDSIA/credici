package examples;

import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.credici.utility.reconciliation.IntegrationChecker;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DrugGender2varsExample {

    // T = 0, S = 1, G = 2
    static int T = 0;  //  Treatment
    static int S = 1;  // Survival
    static int G = 2;  // Gender

    // states for G, T and S
    static int female=1, drug=1, survived=1;
    static int male=0, no_drug=0, dead=0;
    static TIntIntMap[] dataObs, dataDoDrug, dataDoNoDrug, dataAll;


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
        StructuralCausalModel model = CausalBuilder.of(DAGUtil.build("("+T+","+S+")"),2)
                .setCausalDAG(DAGUtil.build("("+T+","+S+"),(2,"+T+"),(3,"+S+")")).build();
        model = Cofounding.mergeExoParents(model, new int[][]{{T,S}});

        // Define counts and data

        dataFromPaper(model, null, model.getEndogenousVars());
        //dataFromPaper(model, ObservationBuilder.observe(G,female), model.getEndogenousVars());
        //dataFromPaper(model, ObservationBuilder.observe(G,male), model.getEndogenousVars());



        TIntIntMap[] interventions= null;
        TIntIntMap[][] datasets = null;

        interventions = new TIntIntMap[]{DataUtil.observe(T,drug), DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoDrug, dataDoNoDrug};


        IntegrationChecker checker = new IntegrationChecker(model, dataObs, interventions, datasets);
        System.out.println("Integration metric: "+checker.getMetric());
        //calculateExactPNS("CCVE Observational + do(drug) + do(no_drug)", model, dataObs, interventions, datasets);
        calculatePNS("Observational + do(drug) + do(no_drug)", model, dataObs, interventions, datasets);


        interventions = new TIntIntMap[]{};
        datasets = new TIntIntMap[][]{};
        //calculateExactPNS("CCVE Observational", model, dataObs, interventions, datasets);
        calculatePNS("Observational", model, dataObs, interventions, datasets);



        interventions = new TIntIntMap[]{DataUtil.observe(T,drug), DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoDrug, dataDoNoDrug};
        //calculateExactPNS("CCVE do(drug) + do(no_drug)", model, null, interventions, datasets);
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

        dataAll = DataUtil.vconcat(dataObs, dataDoDrug, dataDoNoDrug);



    }



    private static void calculatePNS(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets) throws InterruptedException, ExecutionControl.NotImplementedException {


        System.out.println("");

        DataIntegrator integrator = DataIntegrator.of(model);
        if(dataObs != null)
            integrator.setObservationalData(dataObs);

        for(int i = 0; i< interventions.length; i++)
            integrator.setData(interventions[i], datasets[i]);



        TIntIntMap[] dataExt = integrator.getExtendedData();
        StructuralCausalModel modelExt = integrator.getExtendedModel();

        System.out.println(integrator);

        //System.out.println("datasize: "+dataExt.length);
        //System.out.println("model size: "+modelExt.getVariables().length);

        for(int s=0; s<1; s++) {
            RandomUtil.setRandomSeed(s);

            EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
            //        EMCredalBuilder builder = EMCredalBuilder.of(model, dataObs)
                    .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                    .setThreshold(0.0)
                    .setNumTrajectories(20)
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

            TIntObjectMap dataDist = null, modelDist = null;



        }
    }


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
