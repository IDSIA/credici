package ch.idsia.credici.learning;

import ch.idsia.credici.factor.BayesianFactorFactoryMutableExt;
import ch.idsia.credici.factor.Operations;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.builder.CausalBuilder;
import ch.idsia.credici.model.info.CausalInfo;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianDefaultFactor;
import ch.idsia.crema.factor.bayesian.BayesianDeterministicFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.learning.DiscreteEM;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.graphical.DAGModel;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.ProbabilityUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.IntStream;


public class FrequentistCausalEM extends DiscreteEM{

    private double regularization = 0.00001;

    private TIntObjectMap<BayesianFactor> counts;

    private HashMap<String, BayesianFactor> posteriorCache = new HashMap<>();

    private boolean usePosteriorCache = true;



    public FrequentistCausalEM(DAGModel<BayesianFactor> model, int[] elimSeq){
        this.inferenceEngine = getDefaultInference(elimSeq);;
        this.priorModel = model;
        this.trainableVars = CausalInfo.of((StructuralCausalModel) priorModel).getExogenousVars();
    }

    public FrequentistCausalEM(DAGModel<BayesianFactor> model) {
        this(model, (new MinFillOrdering()).apply(model));
    }


    protected void stepPrivate(Collection stepArgs) throws InterruptedException {
        // E-stage
        TIntObjectMap<BayesianFactor> counts = expectation((TIntIntMap[]) stepArgs.toArray(TIntIntMap[]::new));
        // M-stage
        maximization(counts);

    }

    protected TIntObjectMap<BayesianFactor> expectation(TIntIntMap[] observations) throws InterruptedException {


        TIntObjectMap<BayesianFactorFactoryMutableExt> counts = new TIntObjectHashMap<>();
        for (int variable : posteriorModel.getVariables()) {
            counts.put(variable, BayesianFactorFactoryMutableExt.factory().domain(posteriorModel.getFactor(variable).getDomain()));
        }

        clearPosteriorCache();


        for (TIntIntMap observation : observations) {
            for (int var : trainableVars) {


                int[] relevantVars = ArraysUtil.addToSortedArray(posteriorModel.getParents(var), var);
                int[] hidden =  IntStream.of(relevantVars).filter(x -> !observation.containsKey(x)).toArray();
                int[] obsVars = IntStream.of(relevantVars).filter(x -> observation.containsKey(x)).toArray();


                if(hidden.length>0){
                    // Case with missing data
                    BayesianFactor phidden_obs = posteriorInference(hidden, observation);
                    if(obsVars.length>0)
                        phidden_obs = phidden_obs.combine(
                                BayesianDeterministicFactor.getJoinDeterministic(
                                        posteriorModel.getDomain(obsVars), observation));


                    BayesianFactor p = counts.get(var).get().addition(phidden_obs);
                    counts.put(var, BayesianFactorFactoryMutableExt
                            .factory()
                            .domain(p.getDomain())
                            .data(p.getData()));
                }else{
                    //fully-observable case
                    for(int index : posteriorModel.getFactor(var).getDomain().getCompatibleIndexes(observation)){
                        double x = counts.get(var).getData()[index] + 1;
                        counts.get(var).valueAt(x, index);
                    }
                }
            }
        }

        // Generate all the counts factors as output
        TIntObjectMap<BayesianFactor> output = new TIntObjectHashMap<>();
        for (int variable : posteriorModel.getVariables()) {
            output.put(variable, counts.get(variable).get());
        }
        return output;


    }

    private void maximization(TIntObjectMap<BayesianFactor> counts){

        updated = false;
        for (int var : trainableVars) {
            BayesianFactor countVar = counts.get(var);

            if(regularization>0.0) {
                BayesianFactor reg = Operations.scalarMultiply(posteriorModel.getFactor(var), regularization);
                countVar = countVar.addition(reg);
            }
            BayesianFactor f = countVar.divide(countVar.marginalize(var));

            double kl = Probability.KL(f, posteriorModel.getFactor(var));
            if(kl > klthreshold) {
                posteriorModel.setFactor(var, f);
                updated = true;
            }
        }
    }


    public FrequentistCausalEM setRegularization(double regularization) {
        this.regularization = regularization;
        return this;
    }

    public double getRegularization() {
        return regularization;
    }

    @Override
    public FrequentistCausalEM setTrainableVars(int[] trainableVars) {

        for(int v: trainableVars)
            if(!CausalInfo.of((StructuralCausalModel) priorModel).isExogenous(v))
                throw new IllegalArgumentException("Only exogenous variables can be trainable. Error with "+v);

        return (FrequentistCausalEM) super.setTrainableVars(trainableVars);
    }




    BayesianFactor posteriorInference(int[] query, TIntIntMap obs) throws InterruptedException {
        String hash = Arrays.toString(Ints.concat(query,new int[]{-1}, obs.keys(), obs.values()));

        if(!posteriorCache.containsKey(hash) || !usePosteriorCache) {
            BayesianFactor p = inferenceEngine.query(posteriorModel, obs, query);
            if(usePosteriorCache)
                posteriorCache.put(hash, p);
            else
                return p;
        }
        return posteriorCache.get(hash);


    }


    private void clearPosteriorCache(){
        posteriorCache.clear();
    }

    public FrequentistCausalEM usePosteriorCache(boolean active) {
        this.usePosteriorCache = active;
        return this;
    }

    public static void main(String[] args) throws InterruptedException {


        int N = 2500;
        int numIterations = 1000; // EM internal iterations
        int n = 3;
        int endoVarSize = 2;
        int exoVarSize = 4;



        RandomUtil.setRandomSeed(1000);
        StructuralCausalModel causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);
        DAGModel vmodel = causalModel.toVCredal(causalModel.getEmpiricalProbs());

        System.out.println(vmodel);

        TIntIntMap[] data = IntStream.range(0, N)
                .mapToObj(i -> causalModel.sample(causalModel.getEndogenousVars()))
                .toArray(TIntIntMap[]::new);


        // randomize P(U)
        StructuralCausalModel rmodel = (StructuralCausalModel) CausalBuilder.random(causalModel,
                5, false
                ,causalModel.getExogenousVars()
        );

        // Run EM in the causal model
        ExpectationMaximization em =
                new FrequentistCausalEM(rmodel)
                        .setRegularization(0.0)
                        .usePosteriorCache(true)
                        .setVerbose(false)
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

