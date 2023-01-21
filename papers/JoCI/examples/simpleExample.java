package examples;
import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.credici.utility.apps.SelectionBias;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.math3.optim.MaxIter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class simpleExample {

    static int X = 0, Z=1, W=2, Y=3;
    static StructuralCausalModel model = null;
    static int maxIter = 1;//300;
    static int EMruns = 1;//200;

    static int[] Sassig = new int[]{0,0,0,0,0,0,1,0}; // new int[]{0,0,0,1,0,1,1,0};



    public static void main(String[] args) throws InterruptedException, ExecutionControl.NotImplementedException, IOException, CsvException {



        String endoArcs = "(0,1),(1,2),(2,3)";
        SparseDirectedAcyclicGraph endoDag = DAGUtil.build(endoArcs);
        SparseDirectedAcyclicGraph exoDAG = endoDag.copy();
        for(int x : endoDag.getVariables()) {
            int u = exoDAG.addVariable();
            exoDAG.addLink(u,x);
        }


        model = CausalBuilder.of(endoDag, 2)
                .setCausalDAG(exoDAG)
                .build();

        model = Cofounding.mergeExoParents(model, X, Z);



        int dataSize = 1000;

        for(int i = 0; i<1; i++) {
            System.out.println("\n\n Sampling "+i);
            RandomUtil.setRandomSeed(i);
            model.fillExogenousWithRandomFactors(3);

            TIntIntMap[] dataObs = model.samples(dataSize, model.getEndogenousVars());
            TIntIntMap[] dataInterX = model.samplesIntervened(dataSize / 2, X, model.getEndogenousVars());
            TIntIntMap[] dataInterXb = model.samplesIntervened(dataSize / 2, X, model.getEndogenousVars());
            TIntIntMap[] dataInterZ = model.samplesIntervened(dataSize / 2, Z, model.getEndogenousVars());

            CausalVE exactInf = new CausalVE(model);
            BayesianFactor res = exactInf.probNecessityAndSufficiency(X,Y);
            System.out.println("Sampling model: "+res);

            System.out.println(Arrays.stream(DataUtil.selectColumns(dataObs, X)).filter(d -> d.get(0)==0).count());

            /*
            Expected

            - Removing Dxb should improve
                Dobs + Dx + Dxb > Dobs + Dx


             */

            learnSingleStudyModelbiased(null, null, dataInterXb, "PNS1: Dxb");




  //          learnSingleStudyModel(dataObs, null, X, "Dobs");
  //          learnSingleStudyModel(null, dataInterX, X, "Dx");
            learnMultiStudyModelbiased(dataObs, null, dataInterXb, "PNS2':     Dobs + Dxb");
            learnMultiStudyModelbiased(null, dataInterX, dataInterXb, "PNS2:    Dx + Dxb");

            learnMultiStudyModelbiased(dataObs, dataInterX, dataInterXb, "PNS3: Dobs + Dx + Dxb ");
            //learnMultiStudyModelbiased(dataObs, dataInterX, null, "Dobs + Dx");

            //learnMultiStudyModelbiased(null, null, dataInterXb, "Dxb");

//            learnSingleStudyModel(null, dataInterX, X, "Dx");



            /*

            learnSingleStudyModel(null, dataInterZ, Z, "Dz");
            learnSingleStudyModel(dataObs, dataInterZ, Z, "Dobs + Dz");
            System.out.println("----");

            learnMultiStudyModel(null, dataInterX, dataInterZ, "Dx + Dz");
            learnMultiStudyModel(dataObs, dataInterX, dataInterZ, "Dobs + Dx + Dz");

*/

        }


    }

    private static void learnMultiStudyModelbiased(TIntIntMap[] dataObs, TIntIntMap[] dataInterX, TIntIntMap[] dataInterXb, String descr) throws InterruptedException, ExecutionControl.NotImplementedException {


        int locaVar = Y;
        int[] Sparents = new int[]{X,Y,W};


        //DataIntegrator integrator = DataIntegrator.of(model, model.getExogenousParents(locaVar)[0]);
        DataIntegrator integrator = DataIntegrator.of(model);

        //integrator.setData(dataInterX2, new int[]{X}, 0);
        int s = 0;


        if(dataObs!=null) {
            integrator.setObservationalData(dataObs, s);
            s++;
        }
        if(dataInterX!=null) {
            integrator.setData(dataInterX, new int[]{X}, s);
            s++;
        }

        boolean biased = false;
        if(dataInterXb!=null) {
            biased = true;
            integrator.setData(dataInterXb, new int[]{X}, s);
            s++;
        }


        integrator.compile();


        StructuralCausalModel extModel = integrator.getExtendedModel(true);
        TIntIntMap[] extData = integrator.getExtendedData(true);
        //integrator.summary();


        if(biased){
            int[] extVars = extModel.getEndogenousVars();
            int[] endoVarsb = IntStream.range(extVars.length-model.getEndogenousVars().length, extVars.length)
                    .map(i -> extVars[i])
                    .toArray();

            int[] SparentsExt = ArraysUtil.slice(endoVarsb, Sparents);

            // Integrate selection bias
            extModel = SelectionBias.addSelector(extModel, SparentsExt, Sassig);
            int S = SelectionBias.findSelector(extModel);
            extData = SelectionBias.applySelector(extData, extModel, S);

            int N0 = (int) Arrays.stream(extData).filter(d -> d.containsKey(S) && d.get(S)==0).count();
            int N1 = (int) Arrays.stream(extData).filter(d -> d.containsKey(S) && d.get(S)==1).count();
            System.out.println("N0: "+N0+" N1: "+N1);

        }

        // Simplify
        extModel = extModel.subModel(extData);
        //System.out.println(extModel.getNetwork());


        // Add S only to variables from dataInterXb


        System.out.println("Running EM");
        EMCredalBuilder builder = new EMCredalBuilder(extModel, extData)
                .setWeightedEM(true)
                .setMaxEMIter(maxIter)
                .setNumTrajectories(EMruns)
                .setTrainableVars(extModel.getExogenousVars());

        builder.build();

        builder.getSelectedPoints();

        System.out.println("Finished EM");

        int finalS = s;
        List selectedPoints =
                builder.getSelectedPoints().stream()
                        .map(m -> {
                                    StructuralCausalModel mout = integrator.removeInterventionalFromMultiStudy(m, finalS -1);
                                    //StructuralCausalModel mout = integrator.removeInterventionalFromMultiStudy(m, 0);

                            return mout;
                                }
                        )
                        .collect(Collectors.toList());


        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res = (VertexFactor) inf.probNecessityAndSufficiency(X, Y);

        double[] vals = new double[]{res.getData()[0][0][0], res.getData()[0][1][0]};
        vals = new double[]{Arrays.stream(vals).min().getAsDouble(),Arrays.stream(vals).max().getAsDouble()};
        System.out.println(descr+"\t"+Arrays.toString(vals));

    }



    private static void learnSingleStudyModelbiased(TIntIntMap[] dataObs, TIntIntMap[] dataInterX, TIntIntMap[] dataInterXb, String descr) throws InterruptedException, ExecutionControl.NotImplementedException {


        int locaVar = W;
        int[] Sparents = new int[]{X,Y,W};


        DataIntegrator integrator = DataIntegrator.of(model);

        if(dataObs!=null) {
            integrator.setObservationalData(dataObs);
        }
        if(dataInterX!=null) {
            integrator.setData(dataInterX, new int[]{X});
        }

        boolean biased = false;
        if(dataInterXb!=null) {
            biased = true;
            integrator.setData(dataInterXb, new int[]{X});
        }


        integrator.compile();


        StructuralCausalModel extModel = integrator.getExtendedModel();
        TIntIntMap[] extData = integrator.getExtendedData();
        //integrator.summary();


        if(biased){
            int[] extVars = extModel.getEndogenousVars();
            int[] endoVarsb = IntStream.range(extVars.length-model.getEndogenousVars().length, extVars.length)
                    .map(i -> extVars[i])
                    .toArray();

            int[] SparentsExt = ArraysUtil.slice(endoVarsb, Sparents);

            // Integrate selection bias
            extModel = SelectionBias.addSelector(extModel, SparentsExt, Sassig);
            int S = SelectionBias.findSelector(extModel);
            extData = SelectionBias.applySelector(extData, extModel, S);

            int N0 = (int) Arrays.stream(extData).filter(d -> d.containsKey(S) && d.get(S)==0).count();
            int N1 = (int) Arrays.stream(extData).filter(d -> d.containsKey(S) && d.get(S)==1).count();
            System.out.println("N0: "+N0+" N1: "+N1);

        }

        // Simplify
        //extModel = extModel.subModel(extData);
        //System.out.println(extModel.getNetwork());


        // Add S only to variables from dataInterXb


        System.out.println("Running EM");
        EMCredalBuilder builder = new EMCredalBuilder(extModel, extData)
                .setWeightedEM(true)
                .setMaxEMIter(maxIter)
                .setNumTrajectories(EMruns)
                .setTrainableVars(extModel.getExogenousVars());

        builder.build();

        builder.getSelectedPoints();

        System.out.println("Finished EM");

        List selectedPoints =
                builder.getSelectedPoints().stream()
                        .map(m -> {
                                    StructuralCausalModel mout = m.subModel(model.getVariables());
                                    return mout;
                                }
                        )
                        .collect(Collectors.toList());


        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res = (VertexFactor) inf.probNecessityAndSufficiency(X, Y);

        double[] vals = new double[]{res.getData()[0][0][0], res.getData()[0][1][0]};
        vals = new double[]{Arrays.stream(vals).min().getAsDouble(),Arrays.stream(vals).max().getAsDouble()};
        System.out.println(descr+"\t"+Arrays.toString(vals));

    }


    private static void learnMultiStudyModel(TIntIntMap[] dataObs, TIntIntMap[] dataInterX, TIntIntMap[] dataInterZ, String descr) throws InterruptedException, ExecutionControl.NotImplementedException {


        DataIntegrator integrator = DataIntegrator.of(model, model.getExogenousParents(W)[0]);
        //integrator.setData(dataInterX2, new int[]{X}, 0);
        int s = 0;
        if(dataInterZ!=null) {
            integrator.setData(dataInterZ, new int[]{Z}, s);
            s++;
        }
        if(dataInterX!=null) {
            integrator.setData(dataInterX, new int[]{X}, s);
            s++;
        }
        if(dataObs!=null)
            integrator.setObservationalData(dataObs, s);


        StructuralCausalModel extModel = integrator.getExtendedModel(true);
        TIntIntMap[] extData = integrator.getExtendedData(true);
        //integrator.summary();

        // Simplify
        extModel = extModel.subModel(extData);
        System.out.println(extModel.getNetwork());

        System.out.println("Endo vars:"+model.getEndogenousVars());

        if(model.getEndogenousVars().length*integrator.getStudies().size() != DataUtil.variables(extData).length)
            throw new IllegalStateException("Wrong variables in data");


        System.out.println("Running EM");
        EMCredalBuilder builder = new EMCredalBuilder(extModel, extData)
                .setWeightedEM(true)
                .setMaxEMIter(maxIter)
                .setNumTrajectories(EMruns)
                .setTrainableVars(extModel.getExogenousVars());

        builder.build();
        builder.getSelectedPoints();

        System.out.println("Finished EM");
        List selectedPoints =
                builder.getSelectedPoints().stream()
                        .map(m -> {
                                    //System.out.println(m.getFactors(model.getExogenousVars()));
                                    StructuralCausalModel mout = integrator.removeInterventionalFromMultiStudy(m, 0);
                                    //System.out.println(mout.getFactors(model.getExogenousVars()));
                                    //        System.out.println("=====");
                                    return mout;
                                }
                        )
                        .collect(Collectors.toList());

        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res = (VertexFactor) inf.probNecessityAndSufficiency(X, Y);

        double[] vals = new double[]{res.getData()[0][0][0], res.getData()[0][1][0]};
        vals = new double[]{Arrays.stream(vals).min().getAsDouble(),Arrays.stream(vals).max().getAsDouble()};
        System.out.println(descr+"\t"+Arrays.toString(vals));    }





    private static void learnSingleStudyModel(TIntIntMap[] dataObs, TIntIntMap[] dataInter, int interVar,  String descr) throws InterruptedException, ExecutionControl.NotImplementedException {


        DataIntegrator integrator = DataIntegrator.of(model);
        //integrator.setData(dataInterX2, new int[]{X}, 0);
        if(dataInter!=null)
            integrator.setData(dataInter, new int[]{interVar});

        if(dataObs!=null)
            integrator.setObservationalData(dataObs);


        StructuralCausalModel extModel = integrator.getExtendedModel();
        TIntIntMap[] extData = integrator.getExtendedData();


        // Simplify
        //extModel = extModel.subModel(extData);


        System.out.println("Running EM");
        EMCredalBuilder builder = new EMCredalBuilder(extModel, extData)
                .setWeightedEM(true)
                .setMaxEMIter(maxIter)
                .setNumTrajectories(EMruns)
                .setTrainableVars(extModel.getExogenousVars());

        builder.build();
        builder.getSelectedPoints();

        System.out.println("Finished EM");
        List selectedPoints =
                builder.getSelectedPoints().stream()
                        .map(m -> integrator.removeInterventional(m))
                        .collect(Collectors.toList());

        CausalMultiVE inf = new CausalMultiVE(selectedPoints);
        VertexFactor res = (VertexFactor) inf.probNecessityAndSufficiency(X, Y);

        double[] vals = new double[]{res.getData()[0][0][0], res.getData()[0][1][0]};
        vals = new double[]{Arrays.stream(vals).min().getAsDouble(),Arrays.stream(vals).max().getAsDouble()};
        System.out.println(descr+"\t"+Arrays.toString(vals));    }


}

/*

 Sampling 0
N0: 766 N1: 234
Running EM
Finished EM
Dxb	[6.102371824240614E-4, 0.2832135383146393]
Running EM
Finished EM
Dobs	[6.173370732711504E-5, 0.11370628771420846]
Running EM
Finished EM
Dx	[0.0040881951676458765, 0.05025534175483071]
N0: 766 N1: 234
Running EM
Finished EM
Dobs + Dx + Dxb	[8.12478575874094E-5, 0.07204500064821694]
Running EM
Finished EM
Dobs + Dx	[0.0026391660917713493, 0.049973485248125976]
N0: 766 N1: 234
Running EM
Finished EM
Dx + Dxb	[0.0015149877565522605, 0.0954461724610981]
N0: 766 N1: 234
Running EM
Finished EM
Dobs + Dxb	[2.818254330121411E-4, 0.05494191745869961]



-----
Dobs	[1.183910615097216E-4, 0.10711738846389875]
Running EM
Finished EM
Dx	[0.0032360752480892385, 0.0492434400712054]
N0: 522 N1: 478
Running EM
Finished EM
Dobs + Dx + Dxb	[0.0023820951109548025, 0.04977280782851055]
Running EM
Finished EM
Dobs + Dx	[0.002431796735301642, 0.048046171687328734]
N0: 522 N1: 478
Running EM
Finished EM
Dx + Dxb	[0.003952680589829673, 0.05227079450265634]
N0: 522 N1: 478
Running EM
Finished EM
Dobs + Dxb	[0.003235688065956855, 0.03795950236889081]


 */