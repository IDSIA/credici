import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.predefined.RandomChainMarkovian;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.core.ObservationBuilder;
import ch.idsia.crema.data.WriterCSV;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.convert.BayesianToInterval;
import ch.idsia.crema.factor.convert.BayesianToVertex;

import ch.idsia.crema.factor.credal.vertex.separate.VertexFactor;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.learning.FrequentistEM;

import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.IntStream;

public class EMforCausal {
    public static void main(String[] args) throws InterruptedException, IOException {

        int N = 2500;
        int numIterations = 20; // EM internal iterations
        int numRuns = 1;
        int n = 3;
        int endoVarSize = 2;
        int exoVarSize = 6;

        int target;
        TIntIntMap intervention;

        // n=3, en=2, ex=6
        RandomUtil.setRandomSeed(1234); // imprecise causal result
        //RandomUtil.setRandomSeed(123); // precise causal result

        int k = 0;
        StructuralCausalModel causalModel = null;
        VertexFactor credalRes = null;

        int nvert = 1;

            // n=3, en=2, ex=4
            //RandomUtil.setRandomSeed(123); // precise causal result


            causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);
            //StructuralCausalModel causalModel = RandomChainMarkovian.buildModel(n, endoVarSize, exoVarSize);

            // query info
            target = causalModel.getExogenousVars().length - 1;
            intervention = ObservationBuilder.observe(0, 1);
            //intervention = new TIntIntHashMap();

            System.out.println(causalModel);
            // Exact query
            System.out.println("CausalVE");
            CausalVE cve = new CausalVE(causalModel);

            BayesianFactor res = cve.doQuery(target, intervention);
            System.out.println(res);

            System.out.println("CredalCausalVE");

            try {
                CredalCausalVE inf = new CredalCausalVE(causalModel);
                credalRes = (VertexFactor) inf.causalQuery().setTarget(target).setIntervention(intervention).run();
                System.out.println(credalRes);
                nvert = credalRes.getData().length;

            }catch (Exception e){
                nvert = 1;
            }



        ////// empirical


        BayesianNetwork bnet = (BayesianNetwork) causalModel.getEmpiricalNet();
        System.out.println("Obtained empiricals");
        System.out.println(bnet);




        /////// with  EM approximation


        // Sample from bnet

        //TIntIntMap[] data =  IntStream.range(0,N).mapToObj(i -> causalModel.sample(causalModel.getEndogenousVars())).toArray(TIntIntMap[]::new);
        TIntIntMap[] data =  IntStream.range(0,N).mapToObj(i -> bnet.sample()).toArray(TIntIntMap[]::new);
        HashMap empMap = causalModel.getEmpiricalMap();

        IntervalFactor[] ifactors = new IntervalFactor[numRuns];
        BayesianNetwork[] bnets = new BayesianNetwork[numRuns];

        for(int i=0; i<numRuns; i++) {

            // randomize P(U)
            StructuralCausalModel rmodel = (StructuralCausalModel) BayesianFactor.randomModel(causalModel,
                    5, false
                    ,causalModel.getExogenousVars()
            );

            // Run EM in the causal model
            ExpectationMaximization em =
                    new FrequentistEM(rmodel)
                    .setVerbose(false)
                    .setRegularization(0.0)
                    .setRecordIntermediate(true)
                    .setTrainableVars(causalModel.getExogenousVars());


            // run the method
            em.run(Arrays.asList(data), numIterations);

            System.out.println("Log-Likelihood ratio evolution");
            for(Object mt : em.getIntermediateModels() ){
                StructuralCausalModel model_t = (StructuralCausalModel) mt;
                HashMap probMap = model_t.getEmpiricalMap();
                double ratiolk = Probability.ratioLikelihood(probMap, empMap, 10);
                double ratiologlk = Probability.ratioLogLikelihood(probMap, empMap, 1);


                System.out.println("ratiolk10 = "+ratiolk+" ratiologlk = "+ratiologlk);
            }



            // Extract the learnt model
            StructuralCausalModel postModel = (StructuralCausalModel) em.getPosterior();
            System.out.println(postModel);

            bnets[i] = postModel.toBnet();

            // Run the  query
            CausalVE ve = new CausalVE(postModel);

            ifactors[i] = new BayesianToInterval().apply((BayesianFactor) ve.causalQuery().setIntervention(intervention).setTarget(target).run(), target);
            System.out.println(ifactors[i]);





        }

        System.out.println(IntervalFactor.mergeBounds(ifactors));

        SparseModel composed = VertexFactor.buildModel(true,bnets);

     //   for(int x: causalModel.getEndogenousVars())
     //       composed.setFactor(x, new BayesianToVertex().apply(causalModel.getFactor(x).reorderDomain(x), x));



        for(int v:composed.getVariables())
            System.out.println(composed.getFactor(v));

        CredalCausalVE credalVE = new CredalCausalVE(composed);
        System.out.println(credalVE.causalQuery().setIntervention(intervention).setTarget(target).run());






    }
}
