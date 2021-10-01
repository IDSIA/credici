package ch.idsia.credici.learning;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.learning.DiscreteEM;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.IntStream;


public class WeightedCausalEM extends FrequentistCausalEM {

    private double regularization = 0.00001;

    private TIntObjectMap<BayesianFactor> counts;

    private HashMap<String, BayesianFactor> posteriorCache = new HashMap<>();

    private boolean usePosteriorCache = true;



    public WeightedCausalEM(StructuralCausalModel model,
                               JoinInference<BayesianFactor, BayesianFactor> inferenceEngine) {
        super(model, inferenceEngine);
    }

    public WeightedCausalEM(GraphicalModel<BayesianFactor> model, int[] elimSeq){
        super(model, elimSeq);
    }

    public WeightedCausalEM(GraphicalModel<BayesianFactor> model) {
        super(model);
    }


    protected TIntObjectMap<BayesianFactor> expectation(TIntIntMap[] observations) throws InterruptedException {

        Pair[] dataWeighted = DataUtil.getCounts(observations);

        TIntObjectMap<BayesianFactor> counts = new TIntObjectHashMap<>();
        for (int variable : posteriorModel.getVariables()) {
            counts.put(variable, new BayesianFactor(posteriorModel.getFactor(variable).getDomain(), false));
        }

        clearPosteriorCache();


        for(Pair p : dataWeighted){
            TIntIntMap observation = (TIntIntMap) p.getLeft();
            long w = ((Long)p.getRight()).longValue();

            for (int var : trainableVars) {


                int[] relevantVars = ArraysUtil.addToSortedArray(posteriorModel.getParents(var), var);
                int[] hidden =  IntStream.of(relevantVars).filter(x -> !observation.containsKey(x)).toArray();

                //int[] obsVars = IntStream.of(relevantVars).filter(x -> observation.containsKey(x)).toArray();
                // Consider only d-connected observed variables
                int[] obsVars = IntStream.of(observation.keys())
                        .filter(x -> !DAGUtil.dseparated(
                                ((StructuralCausalModel)priorModel).getNetwork(),
                                var,
                                x,
                                observation.keys()))
                        .toArray();

                if(hidden.length>0){
                    // Case with missing data
                    BayesianFactor phidden_obs = posteriorInference(hidden, observation);
                    phidden_obs = phidden_obs.scalarMultiply(w);
                    counts.put(var, counts.get(var).addition(phidden_obs));
                }else{
                    //fully-observable case
                    for(int index : counts.get(var).getDomain().getCompatibleIndexes(observation)){
                        double x = counts.get(var).getValueAt(index) + w;
                        counts.get(var).setValueAt(x, index);
                    }
                }
            }
        }

        return counts;
    }


    public static void main(String[] args) throws InterruptedException {


        int N = 5000;
        int numIterations = 1000; // EM internal iterations
        int n = 3;
        int endoVarSize = 2;
        int exoVarSize = 4;



        RandomUtil.setRandomSeed(1000);
        StructuralCausalModel causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);
        SparseModel vmodel = causalModel.toVCredal(causalModel.getEmpiricalProbs());

        System.out.println(vmodel);

        TIntIntMap[] data = IntStream.range(0, N)
                .mapToObj(i -> causalModel.sample(causalModel.getEndogenousVars()))
                .toArray(TIntIntMap[]::new);


        // randomize P(U)
        StructuralCausalModel rmodel = (StructuralCausalModel) BayesianFactor.randomModel(causalModel,
                5, false
                ,causalModel.getExogenousVars()
        );

        // Run EM in the causal model
        ExpectationMaximization em =
                new WeightedCausalEM(rmodel)
                        .setVerbose(false)
                        .setRegularization(0.0)
                        .usePosteriorCache(true)
                        .setTrainableVars(causalModel.getExogenousVars());

        StopWatch watch = new StopWatch();
        watch.start();

        // run the method
        em.run(Arrays.asList(data), numIterations);

        watch.stop();
        System.out.println("Time Elapsed: " + watch.getTime()+" ms.");

        System.out.println(em.getPosterior());


    }

}

