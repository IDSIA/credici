package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.learning.FrequentistCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.DataIntegrator;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class DrugGenderExample {


    static int T = 0;  //  Gender
    static int S = 1;  // Treatment
    static int G = 2;  // Survival

    // states for G, T and S
    static int female=1, drug=1, survived=1;
    static int male=0, no_drug=0, dead=0;

    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {

        String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici";
        String folder = Path.of(wdir, "papers/clear23/").toString();

        // Conservative SCM
        StructuralCausalModel model = (StructuralCausalModel) IO.read(folder+"/models/literature/consPearl.uai");
        TIntIntMap[] data = DataUtil.fromCSV(folder+"/models/literature/dataPearl.csv");

        // Define counts and data

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

        TIntIntMap[] dataObs = DataUtil.dataFromCounts(countsObs);
        DataUtil.toCSV(folder+"/models/literature/dataPearlObs.csv", dataObs);

        //// Interventional data do(T=drug)
        BayesianFactor countsDoDrug = new BayesianFactor(model.getDomain(T,G,S));
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,female, S,survived), 489);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,female, S,dead), 511);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,male, S,survived), 490);
        FactorUtil.setValue(countsDoDrug, DataUtil.observe(T,drug, G,male, S,dead), 510);

        TIntIntMap[] dataDoDrug = DataUtil.dataFromCounts(countsDoDrug);
        DataUtil.toCSV(folder+"/models/literature/dataPearlDoDrug.csv", dataDoDrug);


        //// Interventional data do(T=no_drug)
        BayesianFactor countsDoNoDrug = new BayesianFactor(model.getDomain(T,G,S));
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,female, S,survived), 210);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,female, S,dead), 790);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,male, S,survived), 210);
        FactorUtil.setValue(countsDoNoDrug, DataUtil.observe(T,no_drug, G,male, S,dead), 790);

        TIntIntMap[] dataDoNoDrug = DataUtil.dataFromCounts(countsDoNoDrug);
        DataUtil.toCSV(folder+"/models/literature/dataPearlDoNoDrug.csv", dataDoNoDrug);

        TIntIntMap[] interventions= null;
        TIntIntMap[][] datasets = null;

        interventions = new TIntIntMap[]{DataUtil.observe(T,drug), DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoDrug, dataDoNoDrug};
        calculatePNS("do(drug) + do(no_drug)", model, null, interventions, datasets);

        interventions = new TIntIntMap[]{DataUtil.observe(T,drug)};
        datasets = new TIntIntMap[][]{dataDoDrug};
        calculatePNS("do(drug)", model, null, interventions, datasets);

        interventions = new TIntIntMap[]{DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoNoDrug};
        calculatePNS("do(no_drug)", model, null, interventions, datasets);


        interventions = new TIntIntMap[]{DataUtil.observe(T,drug), DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoDrug, dataDoNoDrug};
        calculatePNS("Observational + do(drug) + do(no_drug)", model, dataObs, interventions, datasets);

        interventions = new TIntIntMap[]{DataUtil.observe(T,drug)};
        datasets = new TIntIntMap[][]{dataDoDrug};
        calculatePNS("Observational + do(drug)", model, dataObs, interventions, datasets);

        interventions = new TIntIntMap[]{DataUtil.observe(T,no_drug)};
        datasets = new TIntIntMap[][]{dataDoNoDrug};
        calculatePNS("Observational + do(no_drug)", model, dataObs, interventions, datasets);




        // todo: extend


    }

    private static void calculatePNS(String description, StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] interventions, TIntIntMap[][] datasets) throws InterruptedException, ExecutionControl.NotImplementedException {
        DataIntegrator integrator = DataIntegrator.of(model);
        if(dataObs != null)
            integrator.setObservationalData(dataObs);

        for(int i = 0; i< interventions.length; i++)
            integrator.setData(interventions[i], datasets[i]);


        TIntIntMap[] dataExt = integrator.getExtendedData();
        StructuralCausalModel modelExt = integrator.getExtendedModel();


        EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
        //EMCredalBuilder builder = EMCredalBuilder.of(model, data)
                .setStopCriteria(FrequentistCausalEM.StopCriteria.KL)
                .setThreshold(0.0)
                .setNumTrajectories(30)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(300);

        builder.build();


        List selectedPoints = builder.getSelectedPoints();//.stream().map(m -> integrator.removeInterventional(m)).collect(Collectors.toList());

        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res_obs = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug);
        //System.out.println(description);
        double pns_u = Math.max(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
        double pns_l = Math.min(res_obs.getData()[0][0][0], res_obs.getData()[0][1][0]);
        System.out.println("PNS=["+pns_l+","+pns_u+"]\t Datasets: "+description);
    }


}
