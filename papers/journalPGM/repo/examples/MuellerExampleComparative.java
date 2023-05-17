package repo.examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/** Example with model from:
 *  Mueller, S., Li, A., Pearl, J., 2021. Causes of effects: Learning individual
 *  responses from population data. arXiv:2104.13730 .
 *
 */

public class MuellerExampleComparative {
    static int numEMruns = 100;
    static int maxIter = 300;


    static int T = 0;  //  Treatment
    static int S = 1;  // Survival
    static int G = 2;  // Gender


    static int X = T, Y = S;

    // states for G, T and S
    static int female = 1, drug = 1, survived = 1, x = 1, y = 1;
    static int male = 0, no_drug = 0, dead = 0, x_ = 0, y_ = 0;


    static StructuralCausalModel modelMarkovian = null;

    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {


        //// Numeric values identifying the variables and states in the model


        // Relevant paths (update)
        String wdir = ".";
        String dataPath = Path.of(wdir, "./papers/journalPGM/models/literature/").toString();

        /* Example selection bias */

        // Load the data and the model
        modelMarkovian = (StructuralCausalModel) IO.readUAI(Path.of(dataPath, "consPearl.uai").toString());
        StructuralCausalModel model = modelMarkovian;
        //model = Cofounding.mergeExoParents(modelMarkovian, new int[][]{{T,S}});
        //model = Cofounding.mergeExoParents(modelMarkovian, new int[][]{{G,T}});
        //model = Cofounding.mergeExoParents(modelMarkovian, new int[][]{{G,S}});


        //StructuralCausalModel model = Cofounding.mergeExoParents(modelMarkovian, new int[][]{{G, T}});

        TIntIntMap[] dataObs = DataUtil.fromCSV(Path.of(dataPath, "dataPearlObs.csv").toString());

        // Load the intervened data
        TIntIntMap[] dataDoDrug = DataUtil.fromCSV(Path.of(dataPath, "dataPearlDoDrug.csv").toString());
        TIntIntMap[] dataDoNoDrug = DataUtil.fromCSV(Path.of(dataPath, "dataPearlDoNoDrug.csv").toString());
        TIntIntMap[] dataInt = DataUtil.vconcat(dataDoDrug, dataDoNoDrug);
        String descr = "";



        //exact(model, dataObs);
        //approxLP(model, dataObs);


        TianBounds(model, dataObs, null, null, "Tian bounds with Dobs (Eq12)");
       TianBounds(model, null, dataDoDrug, dataDoNoDrug, "Tian bounds with Dx (Eq14)");
       TianBounds(model, dataObs, dataDoDrug, dataDoNoDrug, "Tian bounds with Dobs+Dx (Eq15-16)");

        //TianBoundsExogenity(model, dataObs, null, null, "Tian bounds with Dobs (Eq20)");




        EMCC(model, dataObs, null, "EMCC with Dobs");
       EMCC(model, null, dataInt, "EMCC with Dx");
       EMCC(model, dataObs, dataInt, "EMCC with Dobs+Dx");


    }

    private static void TianBounds(StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] dataDoDrug, TIntIntMap[] dataDoNoDrug, String descr) {
        double pxy=Double.NaN, px_y_=Double.NaN, px_y=Double.NaN, pxy_ = Double.NaN, py=Double.NaN;
        double pydox=Double.NaN, py_dox_=Double.NaN, pydox_=Double.NaN;

        if(dataObs != null) {
            BayesianNetwork bnet = model.getEmpiricalNet(dataObs);
            VariableElimination ve = new FactorVariableElimination(model.getVariables());
            ve.setFactors(bnet.getFactors());
            BayesianFactor joint = (BayesianFactor) ve.conditionalQuery(new int[]{X, Y});
            BayesianFactor marg = (BayesianFactor) ve.conditionalQuery(new int[]{Y});

            pxy = joint.filter(X, x).filter(Y, y).getValue(0);
            px_y_ = joint.filter(X, x_).filter(Y, y_).getValue(0);
            px_y = joint.filter(X, x_).filter(Y, y).getValue(0);
            pxy_ = joint.filter(X, x).filter(Y, y_).getValue(0);
            py = marg.filter(Y, y).getValueAt(0);
        }

        if (dataDoDrug !=null && dataDoNoDrug !=null) {

            BayesianNetwork bnetDoDrug = modelMarkovian.getEmpiricalNet(dataDoDrug);
            VariableElimination ve1 = new FactorVariableElimination(bnetDoDrug.getVariables());
            ve1.setFactors(bnetDoDrug.getFactors());
            ve1.setEvidence(ObservationBuilder.observe(T, drug));
            BayesianFactor pDoDrug = (BayesianFactor) ve1.run(Y);

            BayesianNetwork bnetDoNoDrug = modelMarkovian.getEmpiricalNet(dataDoNoDrug);
            VariableElimination ve2 = new FactorVariableElimination(bnetDoNoDrug.getVariables());
            ve2.setFactors(bnetDoNoDrug.getFactors());
            ve2.setEvidence(ObservationBuilder.observe(T, no_drug));
            BayesianFactor pDoNoDrug = (BayesianFactor) ve2.run(Y);

            /*
            BayesianFactor pDoDrug = DataUtil.getJointProb(dataDoDrug, model.getDomain(Y));
            BayesianFactor pDoNoDrug = DataUtil.getJointProb(dataDoNoDrug, model.getDomain(Y));
*/
            pydox = pDoDrug.getValue(y);
            py_dox_ = pDoNoDrug.getValue(y_);
            pydox_ = pDoNoDrug.getValue(y);
        }

        double lb=Double.NaN, ub=Double.NaN;


        // Only with observational data (non experimental data)
        // EMCC: PNS in [8.981822388340003E-4,0.42096057188645847]


        // With Eq12, Only observational data  Dobs

        if(dataObs !=null && dataDoDrug ==null && dataDoNoDrug ==null) {
            lb = 0;
            ub = pxy + px_y_;
        }else if(dataObs ==null && dataDoDrug !=null && dataDoNoDrug !=null) {
            //Only interventional   Dx
            lb = Math.max(0, pydox - pydox_);
            ub = Math.min(pydox, py_dox_);
        }else if(dataObs !=null && dataDoDrug !=null && dataDoNoDrug !=null) {
            // Eq 15-16 Observational and Interventional    Dobs + Dx
            lb = DoubleStream.of(0, pydox - pydox_, py - pydox_, pydox - py).max().getAsDouble();
            ub = DoubleStream.of(pydox, py_dox_, pxy + px_y_, pydox - pydox_ + pxy_ + px_y).min().getAsDouble();
        }
        System.out.println(descr +" [" + lb + "," + ub + "]");
    }



    private static void TianBoundsExogenity(StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] dataDoDrug, TIntIntMap[] dataDoNoDrug, String descr) {
        double pxy=Double.NaN, px_y_=Double.NaN, px_y=Double.NaN, pxy_ = Double.NaN, py=Double.NaN;
        double pydox=Double.NaN, py_dox_=Double.NaN, pydox_=Double.NaN;
        double pyGx=Double.NaN, py_Gx_=Double.NaN, py_Gx=Double.NaN, pyGx_ = Double.NaN;

        if(dataObs != null) {
            BayesianNetwork bnet = model.getEmpiricalNet(dataObs);
            VariableElimination ve = new FactorVariableElimination(model.getVariables());
            ve.setFactors(bnet.getFactors());
            BayesianFactor joint = (BayesianFactor) ve.conditionalQuery(new int[]{X, Y});
            BayesianFactor marg = (BayesianFactor) ve.conditionalQuery(new int[]{Y});
            BayesianFactor cond = (BayesianFactor) ve.conditionalQuery(new int[]{Y}, X);


            pxy = joint.filter(X, x).filter(Y, y).getValue(0);
            px_y_ = joint.filter(X, x_).filter(Y, y_).getValue(0);
            px_y = joint.filter(X, x_).filter(Y, y).getValue(0);
            pxy_ = joint.filter(X, x).filter(Y, y_).getValue(0);
            py = marg.filter(Y, y).getValueAt(0);

            pyGx = cond.filter(X, x).filter(Y, y).getValue(0);
            py_Gx_ = cond.filter(X, x_).filter(Y, y_).getValue(0);
            py_Gx = cond.filter(X, x).filter(Y, y_).getValue(0);
            pyGx_ = cond.filter(X, x_).filter(Y, y).getValue(0);


        }

        if (dataDoDrug !=null && dataDoNoDrug !=null) {

            BayesianNetwork bnetDoDrug = modelMarkovian.getEmpiricalNet(dataDoDrug);
            VariableElimination ve1 = new FactorVariableElimination(bnetDoDrug.getVariables());
            ve1.setFactors(bnetDoDrug.getFactors());
            ve1.setEvidence(ObservationBuilder.observe(T, drug));
            BayesianFactor pDoDrug = (BayesianFactor) ve1.run(Y);

            BayesianNetwork bnetDoNoDrug = modelMarkovian.getEmpiricalNet(dataDoNoDrug);
            VariableElimination ve2 = new FactorVariableElimination(bnetDoNoDrug.getVariables());
            ve2.setFactors(bnetDoNoDrug.getFactors());
            ve2.setEvidence(ObservationBuilder.observe(T, no_drug));
            BayesianFactor pDoNoDrug = (BayesianFactor) ve2.run(Y);

            /*
            BayesianFactor pDoDrug = DataUtil.getJointProb(dataDoDrug, model.getDomain(Y));
            BayesianFactor pDoNoDrug = DataUtil.getJointProb(dataDoNoDrug, model.getDomain(Y));
*/
            pydox = pDoDrug.getValue(y);
            py_dox_ = pDoNoDrug.getValue(y_);
            pydox_ = pDoNoDrug.getValue(y);
        }

        double lb=Double.NaN, ub=Double.NaN;


        // Only with observational data (non experimental data)
        // EMCC: PNS in [8.981822388340003E-4,0.42096057188645847]


        // With Eq12, Only observational data  Dobs

        if(dataObs !=null && dataDoDrug ==null && dataDoNoDrug ==null) {
            lb = Math.max(0, pyGx - pyGx_);
            ub = Math.min(pyGx, py_Gx_);
/*        }else if(dataObs ==null && dataDoDrug !=null && dataDoNoDrug !=null) {
            //Only interventional   Dx
            lb = Math.max(0, pydox - pydox_);
            ub = Math.min(pydox, py_dox_);
        }else if(dataObs !=null && dataDoDrug !=null && dataDoNoDrug !=null) {
            // Eq 15-16 Observational and Interventional    Dobs + Dx
            lb = DoubleStream.of(0, pydox - pydox_, py - pydox_, pydox - py).max().getAsDouble();
            ub = DoubleStream.of(pydox, py_dox_, pxy + px_y_, pydox - pydox_ + pxy_ + px_y).min().getAsDouble();*/
        }else{
            throw new IllegalArgumentException("Not implemented");
        }
        System.out.println(descr +" [" + lb + "," + ub + "]");
    }

    private static void EMCC(StructuralCausalModel model, TIntIntMap[] dataObs, TIntIntMap[] dataInt, String descr) throws InterruptedException, ExecutionControl.NotImplementedException {
        // Set integrator object to build the extended model and data
        DataIntegrator integrator = DataIntegrator.of(model);
        if (dataObs != null)
            integrator.setObservationalData(dataObs);

        if (dataInt != null)
            integrator.setData(dataInt, new int[]{T});

        TIntIntMap[] dataExt = integrator.getExtendedData();
        StructuralCausalModel modelExt = integrator.getExtendedModel();


        EMCredalBuilder builder = EMCredalBuilder.of(modelExt, dataExt)
                .setNumTrajectories(numEMruns)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(maxIter)
                .build();

        // Rebuild a simple model with the learned parameters
        StructuralCausalModel finalModel = model;
        List selectedPoints = builder.getSelectedPoints().stream().map(m -> m.subModel(finalModel.getVariables())).collect(Collectors.toList());

        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor resEM = (VertexFactor) inf.probNecessityAndSufficiency(X, Y, x, x_, y, y_);

        double lb = DoubleStream.of(resEM.getData()[0][0][0], resEM.getData()[0][1][0]).min().getAsDouble();
        double ub = DoubleStream.of(resEM.getData()[0][0][0], resEM.getData()[0][1][0]).max().getAsDouble();
        System.out.println(descr + " [" + lb + "," + ub + "]");
    }



    private static void exact(StructuralCausalModel model, TIntIntMap[] dataObs) throws InterruptedException, ExecutionControl.NotImplementedException {

        CredalCausalVE inf = new CredalCausalVE(model, dataObs);
        VertexFactor res = inf.probNecessityAndSufficiency(X, Y, x, x_);


        double lb = DoubleStream.of(res.getData()[0][0][0], res.getData()[0][1][0]).min().getAsDouble();
        double ub = DoubleStream.of(res.getData()[0][0][0], res.getData()[0][1][0]).max().getAsDouble();
        System.out.println("Exact CVE with Dobs [" + lb + "," + ub + "]");

    }


    private static void approxLP(StructuralCausalModel model, TIntIntMap[] dataObs) throws InterruptedException, ExecutionControl.NotImplementedException {

        CredalCausalApproxLP inf = new CredalCausalApproxLP(model, dataObs);
        IntervalFactor res = inf.probNecessityAndSufficiency(X, Y, x, x_);



        double lb = res.getLower(0)[0];
        double ub = res.getUpper(0)[0];
        System.out.println("ApproxLP with Dobs [" + lb + "," + ub + "]");
    }
}

