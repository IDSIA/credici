import ch.idsia.credici.learning.WeightedCausalEM;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;
import java.util.stream.IntStream;

public class EMvariations {
    public static void main(String[] args) throws InterruptedException {

        int N = 2000;
        int numIterations = 1000; // EM internal iterations
        int n = 7;
        int endoVarSize = 2;
        int exoVarSize = 4;



        RandomUtil.setRandomSeed(1000);
        StructuralCausalModel causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);
        SparseModel vmodel = causalModel.toVCredal(causalModel.getEmpiricalProbs());


        TIntIntMap[] data = IntStream.range(0, N)
                .mapToObj(i -> causalModel.sample(causalModel.getEndogenousVars()))
                .toArray(TIntIntMap[]::new);




        for(int i=0; i<4; i++) {


            // randomize P(U)
            RandomUtil.setRandomSeed(0);
            StructuralCausalModel rmodel = (StructuralCausalModel) BayesianFactor.randomModel(causalModel,
                    5, false
                    ,causalModel.getExogenousVars()
            );

            //System.out.println(rmodel);
            // Run EM in the causal model
            ExpectationMaximization em =
                    new WeightedCausalEM(rmodel)
                            .setInferenceVariation(i)
                            .setVerbose(false)
                            .setRegularization(0.0)
                            .usePosteriorCache(false)
                            .setTrainableVars(causalModel.getExogenousVars());

            StopWatch watch = new StopWatch();
            watch.start();

            // run the method
            em.run(Arrays.asList(data), numIterations);

            watch.stop();
            System.out.println("EM variation "+i+": Time Elapsed: " + watch.getTime() + " ms.");

            System.out.println(em.getPosterior());
        }
    }
}
