import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.predefined.RandomChainMarkovian;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.crema.data.WriterCSV;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.IOException;
import java.util.stream.IntStream;

public class EMforCausal {
    public static void main(String[] args) throws InterruptedException, IOException {

        int N = 1500;
        int numIterations = 50;
        int n = 3;
        int endoVarSize = 2;
        int exoVarSize = 6;

        RandomUtil.setRandomSeed(3223);



        StructuralCausalModel causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);
        //StructuralCausalModel causalModel = RandomChainMarkovian.buildModel(n, endoVarSize, exoVarSize);

        // query info
        int target = causalModel.getExogenousVars().length-1;
        TIntIntMap intervention = ObservationBuilder.observe(0,1);
        //intervention = new TIntIntHashMap();

        System.out.println(causalModel);
        // Exact query
        System.out.println("CausalVE");
        CausalVE cve = new CausalVE(causalModel);
        System.out.println(cve.doQuery(target, intervention));

        System.out.println("CredalCausalVE");

        CredalCausalVE inf = new CredalCausalVE(causalModel);
        System.out.println(inf.causalQuery().setTarget(target).setIntervention(intervention).run());


        ////// empirical


        BayesianNetwork bnet = causalModel.getEmpiricalNet();
        System.out.println("Obtained empiricals");
        System.out.println(bnet);




        /////// with  EM approximation


        // Sample from bnet

        //TIntIntMap[] data =  IntStream.range(0,N).mapToObj(i -> causalModel.sample()).toArray(TIntIntMap[]::new);
        TIntIntMap[] data =  IntStream.range(0,N).mapToObj(i -> bnet.sample()).toArray(TIntIntMap[]::new);


        for(int i=0; i<5; i++) {

            // randomize P(U)
            StructuralCausalModel rmodel = (StructuralCausalModel) BayesianFactor.randomModel(causalModel,
                    5, false
                    ,causalModel.getExogenousVars()
            );

            // Run EM in the causal model
            ExpectationMaximization em = new ExpectationMaximization(rmodel);
            em.setVerbose(false);
            // this is the value added to avoid counts equal to 0
            em.setRegularization(0.00000000001);

            // run the method
            em.run(data, numIterations);

            // Extract the learnt model
            StructuralCausalModel postModel = (StructuralCausalModel) em.getPosterior();
            System.out.println(postModel);

            for(int x: causalModel.getEndogenousVars())
                postModel.setFactor(x, causalModel.getFactor(x));

            // Run the  query
            CausalVE ve = new CausalVE(postModel);
            System.out.println(ve.doQuery(target, intervention));
        }

    }
}
