package examples;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CredalCausalApproxLP;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.VariableElimination;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.DoubleStream;

public class DrugGendersTianPearlIntervals {
    public static void main(String[] args) throws Exception {

//// Numeric values identifying the variables and states in the model
        int T = 0;  //  Treatment
        int S = 1;  // Survival
        int G = 2;  // Gender

        int X = T, Y=S;

        // states for G, T and S
        int female=1, drug=1, survived=1, x=1, y=1;
        int male=0, no_drug=0, dead=0, x_=0, y_=0;

        // Relevant paths (update)
        String wdir = "/Users/rcabanas/GoogleDrive/IDSIA/causality/dev/credici/";
        String dataPath = Path.of(wdir, "/papers/journalPGM/models/literature/").toString();

        /* Example selection bias */

        // Load the data and the model
        StructuralCausalModel model = (StructuralCausalModel) IO.readUAI(Path.of(dataPath, "consPearl.uai").toString());
        model = Cofounding.mergeExoParents(model, new int[][]{{T,S}});

        TIntIntMap[] dataObs = DataUtil.fromCSV(Path.of(dataPath, "dataPearlObs.csv").toString());


        // Set the hidden configurations
        int[][] hidden = {
                {no_drug, survived, male},
                {no_drug, dead, male},
                {drug, survived, female},
                {drug, dead, female},
        };
        int[] Sassig = SelectionBias.getAssignmentWithHidden(model, new int[]{T,S,G}, hidden);


    /*// Get the extended biased model and the biased data
    StructuralCausalModel modelBiased = SelectionBias.addSelector(model, new int[]{T,S,G}, Sassig);
    int Svar = SelectionBias.findSelector(modelBiased);
    TIntIntMap[] biasedData = SelectionBias.applySelector(dataObs, modelBiased, Svar);
*/
        EMCredalBuilder builder = EMCredalBuilder.of(model, dataObs)
                .setNumTrajectories(100)
                .setWeightedEM(true)
                .setVerbose(false)
                .setMaxEMIter(200)
                .build();

        CausalMultiVE inf = new CausalMultiVE(builder.getSelectedPoints());
        VertexFactor resEM = (VertexFactor) inf.probNecessityAndSufficiency(T, S, drug, no_drug, survived, dead);





        // https://arxiv.org/pdf/1301.3898.pdf

        // Eq (12)

        BayesianNetwork bnet = model.getEmpiricalNet(dataObs);
        VariableElimination ve = new FactorVariableElimination(model.getVariables());
        ve.setFactors(bnet.getFactors());
        BayesianFactor joint = (BayesianFactor) ve.conditionalQuery(new int[]{T,S});
        BayesianFactor marg = (BayesianFactor) ve.conditionalQuery(new int[]{S});
        BayesianFactor cond = (BayesianFactor) ve.conditionalQuery(Y,X);


        double pxy = joint.filter(X,x).filter(Y,y).getValue(0);
        double px_y_ = joint.filter(X,x_).filter(Y,y_).getValue(0);
        double px_y = joint.filter(X,x_).filter(Y,y).getValue(0);
        double pxy_ = joint.filter(X,x).filter(Y,y_).getValue(0);

        double pygivenx = cond.filter(Y,y).filter(X,x).getValueAt(0);
        double py_givenx = cond.filter(Y,y).filter(X,x).getValueAt(0);
        double pygivenx_ = cond.filter(Y,y).filter(X,x_).getValueAt(0);
        double py_givenx_ = cond.filter(Y,y_).filter(X,x_).getValueAt(0);

        double py = marg.filter(Y, y).getValueAt(0);

        HashMap empirical = FactorUtil.fixEmpiricalMap(DataUtil.getEmpiricalMap(model, dataObs),5);
        System.out.println(empirical);

        //CredalCausalVE cve = new CredalCausalVE(model, empirical.values());

/*
        double pydox = ((VertexFactor)cve.causalQuery().setTarget(Y).setIntervention(X, x).run()).filter(Y, y).getVerticesAt(0)[0][0];
        double py_dox_ = ((VertexFactor)cve.causalQuery().setTarget(Y).setIntervention(X, x_).run()).filter(Y, y_).getVerticesAt(0)[0][0];
        double pydox_ = ((VertexFactor)cve.causalQuery().setTarget(Y).setIntervention(X, x_).run()).filter(Y, y).getVerticesAt(0)[0][0];


        CredalCausalApproxLP credal_inf = new CredalCausalApproxLP(model, empirical.values());

        double pydox = ((IntervalFactor)credal_inf.causalQuery().setTarget(Y).setIntervention(X, x).run()).filter(Y, y).getVerticesAt(0)[0][0];
        double py_dox_ = ((IntervalFactor)credal_inf.causalQuery().setTarget(Y).setIntervention(X, x_).run()).filter(Y, y_).getVerticesAt(0)[0][0];
        double pydox_ = ((IntervalFactor)credal_inf.causalQuery().setTarget(Y).setIntervention(X, x_).run()).filter(Y, y).getVerticesAt(0)[0][0];


        double lb, ub;

/*

        VertexFactor resCCVE = (VertexFactor) cve.probNecessityAndSufficiency(X, Y, x,x_);
        lb = DoubleStream.of(resCCVE.getData()[0][0][0], resCCVE.getData()[0][1][0]).min().getAsDouble();
        ub = DoubleStream.of(resCCVE.getData()[0][0][0], resCCVE.getData()[0][1][0]).max().getAsDouble();
        System.out.println("CCVE (pgm20): PNS in ["+lb+","+ub+"]");

        lb = DoubleStream.of(resEM.getData()[0][0][0], resEM.getData()[0][1][0]).min().getAsDouble();
        ub = DoubleStream.of(resEM.getData()[0][0][0], resEM.getData()[0][1][0]).max().getAsDouble();
        System.out.println("EMCC: PNS in ["+lb+","+ub+"]");



        lb = 0;
        ub = pxy+px_y_;
        System.out.println("Eq 12: PNS in ["+lb+","+ub+"]");

        lb = Math.max(0, pydox-py_dox_);
        ub = Math.min(pydox, py_dox_);
        System.out.println("Eq 14: PNS in ["+lb+","+ub+"]");



        lb = DoubleStream.of(0, pydox-pydox_, py-pydox_, pydox-py).max().getAsDouble();
        ub = DoubleStream.of(pydox, py_dox_, pxy+px_y_, pydox-pydox_+ pxy_ + px_y).min().getAsDouble();
        System.out.println("Eq 15-16: PNS in ["+lb+","+ub+"]");

        lb = DoubleStream.of(0, pygivenx-pygivenx_).max().getAsDouble();
        ub = DoubleStream.of(pygivenx, py_givenx_).min().getAsDouble();
        System.out.println("Eq 20,21: PNS in ["+lb+","+ub+"]");
*/
    }
}
