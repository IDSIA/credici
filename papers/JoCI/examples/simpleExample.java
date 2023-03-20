package examples;
import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalMultiVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.builder.EMCredalBuilder;
import ch.idsia.credici.model.transform.Cofounding;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.FactorUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.credici.utility.reconciliation.DataIntegrator;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.SparseDirectedAcyclicGraph;
import ch.idsia.crema.utility.RandomUtil;
import com.opencsv.exceptions.CsvException;
import gnu.trove.map.TIntIntMap;
import jdk.jshell.spi.ExecutionControl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class simpleExample {

    static int X = 0, Z=1, W=2, Y=3;
    static StructuralCausalModel model = null;


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

        model = Cofounding.mergeExoParents(model, 0,1);


        int dataSize = 1000;

        for(int i = 0; i<5; i++) {
            System.out.println("\n\n Sampling "+i);
            //todo: samples 50 50
            RandomUtil.setRandomSeed(i);
            model.fillExogenousWithRandomFactors(3);

            TIntIntMap[] dataObs = model.samples(dataSize, model.getEndogenousVars());
            TIntIntMap[] dataInterX = model.samplesIntervened(dataSize / 2, X, model.getEndogenousVars());
            TIntIntMap[] dataInterX2 = model.samplesIntervened(dataSize / 2, X, model.getEndogenousVars());

            TIntIntMap[] dataInterZ = model.samplesIntervened(dataSize / 2, Z, model.getEndogenousVars());


            learnSingleStudyModel(null, dataInterZ, Z, "Dz");
            learnMultiStudyModel(null, dataInterX, dataInterZ, "Dx + Dz");
            learnMultiStudyModel(dataObs, dataInterX, dataInterZ, "Dobs + Dx + Dz");

        }


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
            integrator.setObservationalData(dataObs, 2);


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
                .setMaxEMIter(100)
                .setNumTrajectories(20)
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

        System.out.println(descr);
        System.out.println(res);
    }




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
                .setMaxEMIter(100)
                .setNumTrajectories(20)
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

        System.out.println(descr);
        System.out.println(res);
    }


}

