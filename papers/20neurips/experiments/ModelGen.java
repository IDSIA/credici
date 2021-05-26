package neurnips20.experiments;

import ch.idsia.credici.IO;
import ch.idsia.credici.inference.CausalVE;
import ch.idsia.credici.inference.CredalCausalVE;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ModelGen {
    public static void main(String[] args) throws InterruptedException {


        int N = 20000;
        int numIterations = 10; // EM internal iterations
        int numRuns = 10;
        int n = 6;
        int endoVarSize = 2;
        int exoVarSize = 5;

        List<Integer> seeds = new ArrayList<Integer>();


        while(seeds.size() < 20) {
            int s = RandomUtil.sampleUniform(1, 50000, false)[0];
            //int s = 1225;
            RandomUtil.setRandomSeed(s); // imprecise causal result
            StructuralCausalModel causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);
            // query info
            int target = 3;
            TIntIntMap intervention = ObservationBuilder.observe(0, 1);
            TIntIntMap obs = ObservationBuilder.observe(n-1, 1);

            System.out.println(s);
            try {
                CredalCausalVE inf = new CredalCausalVE(causalModel);
                VertexFactor res = (VertexFactor) inf.causalQuery()
                        .setTarget(target)
                        .setIntervention(intervention)
                        //.setEvidence(obs)
                        .run();

                System.out.println(res);
                if (res.getData()[0].length > 1 && !seeds.contains(s)) {
                    seeds.add(s);
                    IO.write(causalModel, "./papers/neurnips20/experiments/models/scm"+seeds.size()+".uai");
                }
            }catch(Exception e){
                System.out.println(e);
            }

        }

        System.out.println(seeds);


    }
}
