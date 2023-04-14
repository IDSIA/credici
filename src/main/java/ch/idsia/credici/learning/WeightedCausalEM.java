package ch.idsia.credici.learning;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;


public class WeightedCausalEM extends FrequentistCausalEM {


    public WeightedCausalEM(GraphicalModel<BayesianFactor> model) {
        super(model);
    }


    @Override
    public void step(Collection stepArgs) throws InterruptedException {
        stepPrivate(stepArgs);

        performedIterations++;
        if(recordIntermediate)
            addIntermediateModels(posteriorModel);

    }

    @Override
    public void run(Collection stepArgs, int iterations) throws InterruptedException {
        setData((TIntIntMap[]) stepArgs.toArray(TIntIntMap[]::new));

        List<Pair> pairs = Arrays.asList(DataUtil.getCounts(data));
        super.run(pairs, iterations);
    }



    @Override
    protected void stepPrivate(Collection stepArgs) throws InterruptedException {
        try {
            // E-stage
            TIntObjectMap<BayesianFactor> counts = expectation(stepArgs);
            // M-stage
            maximization(counts);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    protected TIntObjectMap<BayesianFactor> expectation(Collection<Pair> dataWeighted) throws InterruptedException, IOException {

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

                if(hidden.length>0){
                    // Case with missing data
                    BayesianFactor phidden_obs = posteriorInference(hidden, observation);
                    phidden_obs = phidden_obs.scalarMultiply(w);
                    //System.out.println(phidden_obs);
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

}

