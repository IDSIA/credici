package ch.idsia.credici.learning;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.model.tools.CausalInfo;
import ch.idsia.credici.model.predefined.RandomChainNonMarkovian;
import ch.idsia.credici.utility.DAGUtil;
import ch.idsia.credici.utility.experiments.Logger;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.learning.DiscreteEM;
import ch.idsia.crema.learning.ExpectationMaximization;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang3.time.StopWatch;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class FrequentistCausalEM extends DiscreteEM<FrequentistCausalEM> {

    private double regularization = 0.00001;

    private TIntObjectMap<BayesianFactor> counts;

    private HashMap<String, BayesianFactor> posteriorCache = new HashMap<>();

    private boolean usePosteriorCache = true;

    private int inferenceVariation = 2;

    TIntObjectHashMap replacedFactors = null;

    StopCriteria stopCriteria = StopCriteria.KL;

    protected double klthreshold = Double.NaN;

    private double threshold = 0.0;

    public enum StopCriteria {
        KL,
        L1,
        LLratio
    }


    public FrequentistCausalEM(StructuralCausalModel model,
                               JoinInference<BayesianFactor, BayesianFactor> inferenceEngine) {
        this.inferenceEngine = inferenceEngine;
        this.priorModel = model;
        this.trainableVars = CausalInfo.of((StructuralCausalModel) priorModel).getExogenousVars();
    }

    public FrequentistCausalEM(GraphicalModel<BayesianFactor> model, int[] elimSeq){
        this.inferenceEngine = getDefaultInference(model, elimSeq);
        this.priorModel = model;
        this.trainableVars = CausalInfo.of((StructuralCausalModel) priorModel).getExogenousVars();
    }

    public FrequentistCausalEM(GraphicalModel<BayesianFactor> model) {
        this(model, (new MinFillOrdering()).apply(model));
    }


    protected void stepPrivate(Collection stepArgs) throws InterruptedException {
        // E-stage
        TIntObjectMap<BayesianFactor> counts = expectation((TIntIntMap[]) stepArgs.toArray(TIntIntMap[]::new));
        // M-stage
        maximization(counts);

    }

    protected TIntObjectMap<BayesianFactor> expectation(TIntIntMap[] observations) throws InterruptedException {

        TIntObjectMap<BayesianFactor> counts = new TIntObjectHashMap<>();
        for (int variable : posteriorModel.getVariables()) {
            counts.put(variable, new BayesianFactor(posteriorModel.getFactor(variable).getDomain(), false));
        }

        clearPosteriorCache();

        for (TIntIntMap observation : observations) {
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

                    /*if(obsVars.length>0)
                        phidden_obs = phidden_obs.combine(
                                BayesianFactor.getJoinDeterministic(posteriorModel.getDomain(obsVars), observation));
                    */
                    counts.put(var, counts.get(var).addition(phidden_obs));
                }else{
                    //fully-observable case
                    for(int index : counts.get(var).getDomain().getCompatibleIndexes(observation)){
                        double x = counts.get(var).getValueAt(index) + 1;
                        counts.get(var).setValueAt(x, index);
                    }
                }
            }
        }

        return counts;
    }


    void maximization(TIntObjectMap<BayesianFactor> counts){

        replacedFactors = new TIntObjectHashMap();
        updated = false;
        for (int var : trainableVars) {
            BayesianFactor countVar = counts.get(var);
            if(regularization>0.0) {
                BayesianFactor reg = posteriorModel.getFactor(var).scalarMultiply(regularization);
                countVar = countVar.addition(reg);
            }
            BayesianFactor f = countVar.divide(countVar.marginalize(var));

            // Store the previous factor and set the new one
            replacedFactors.put(var, posteriorModel.getFactor(var));
            posteriorModel.setFactor(var, f);
        }
        // Determine which trainable variables should not be trained anymore
        if(stopAtConvergence)
            for (int[] exoCC : getTrainableExoCC())
                if (hasConverged(exoCC)) trainableVars = ArraysUtil.difference(trainableVars, exoCC);


    }
    private List<int[]> getTrainableExoCC(){
        return ((StructuralCausalModel)posteriorModel)
                .exoConnectComponents()
                .stream()
                .filter(c -> Arrays.stream(c).allMatch(u -> ArraysUtil.contains(u, trainableVars)))
                .collect(Collectors.toList());
    }

    private boolean hasConverged(int... exoCC) {
        if(stopCriteria == StopCriteria.KL) {
            return TrajectoryAnalyser.hasConvergedKL((StructuralCausalModel) posteriorModel, replacedFactors, threshold, exoCC);
        }else if(stopCriteria == StopCriteria.L1) {
                return TrajectoryAnalyser.hasConvergedL1((StructuralCausalModel) posteriorModel, replacedFactors, threshold, exoCC);
        }else if(stopCriteria == StopCriteria.LLratio){
            return TrajectoryAnalyser.hasConvergedLLratio((StructuralCausalModel) posteriorModel, data, threshold, exoCC);

        }else{
            throw new IllegalArgumentException("Wrong stopping Criteria");
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
            if(!CausalInfo.of((StructuralCausalModel) priorModel).isExogenous(v)) {
                String msg = "Only exogenous variables can be trainable. Error with "+v;
                Logger.getGlobal().severe(msg);
                throw new IllegalArgumentException(msg);
            }

        return super.setTrainableVars(trainableVars);
    }




    BayesianFactor posteriorInference(int[] query, TIntIntMap obs) throws InterruptedException {
        String hash = Arrays.toString(Ints.concat(query,new int[]{-1}, obs.keys(), obs.values()));


        if(!posteriorCache.containsKey(hash) || !usePosteriorCache) {

            BayesianFactor p = null;
            switch (this.inferenceVariation){
                case 0: p = inferenceVariation0(query, obs); break;
                case 1: p = inferenceVariation1(query, obs); break;
                case 2: p = inferenceVariation2(query, obs, hash); break;
                case 3: p = inferenceVariation3(query, obs); break;
                case 4: p = inferenceVariation4(query, obs, hash); break;

            }

            if(usePosteriorCache)
                posteriorCache.put(hash, p);
            else
                return p;
        }
        return posteriorCache.get(hash);

    }

    /*
        Original implementation: inference engine is invoked without any simplification of the model
     */
    BayesianFactor inferenceVariation0(int[] query, TIntIntMap obs) throws InterruptedException {
        return inferenceEngine.apply(posteriorModel, query, obs);  // P(U|X=obs)
    }


    /*
    * The model is simplified at each posterior query
    * */
    BayesianFactor inferenceVariation1(int[] query, TIntIntMap obs) throws InterruptedException {
        GraphicalModel infModel = new CutObserved().execute(posteriorModel, obs);
        infModel = new RemoveBarren().execute(infModel, query, obs);

        TIntIntMap newObs = new TIntIntHashMap();
        for(int x: obs.keys())
            if(ArraysUtil.contains(x, infModel.getVariables()))
                newObs.put(x, obs.get(x));
        return inferenceEngine.apply(infModel, query, newObs);  // P(U|X=obs)
    }


    private HashMap<String, StructuralCausalModel> modelCache = new HashMap<>();


    /*
     * The model is simplified at the first posterior query and stored in a caché
     * */
    BayesianFactor inferenceVariation2(int[] query, TIntIntMap obs, String hash) throws InterruptedException {
        StructuralCausalModel infModel = null;

        if(!modelCache.containsKey(hash)) {
            infModel = (StructuralCausalModel) new CutObserved().execute(posteriorModel, obs);
            infModel = new RemoveBarren().execute(infModel, query, obs);
        } else{
            infModel = modelCache.get(hash);
            for(int u: infModel.getExogenousVars()){
                infModel.setFactor(u, posteriorModel.getFactor(u));
            }
        }

        TIntIntMap newObs = new TIntIntHashMap();
        for(int x: obs.keys())
            if(ArraysUtil.contains(x, infModel.getVariables()))
                newObs.put(x, obs.get(x));
        return inferenceEngine.apply(infModel, query, newObs);  // P(U|X=obs)
    }

    /*
     The inference engine is not invoked, operations over factors are directly perfomed.
     */
    BayesianFactor inferenceVariation3(int[] query, TIntIntMap obs) throws InterruptedException {
        // todo: only with M and QM... set checks

        int U = query[0];
        int[] chU = posteriorModel.getChildren(U);

        ArrayList<BayesianFactor> factors = new ArrayList<>();
        factors.add(posteriorModel.getFactor(U));

        for(int X: chU){
            TIntIntHashMap newObs = new TIntIntHashMap();
            BayesianFactor fx = posteriorModel.getFactor(X);
            for(int x: obs.keys()) {
                if (ArraysUtil.contains(x, fx.getDomain().getVariables()))
                    newObs.put(x, obs.get(x));
            }
            factors.add(fx.filter(newObs));
        }

        BayesianFactor pjoin = BayesianFactor.combineAll(factors);
        return pjoin.divide(pjoin.marginalize(U));
    }


    private HashMap<String, BayesianFactor> equationsCache = new HashMap<>();


    /*
     * The model is simplified at the first posterior query and stored in a caché
     * */
    BayesianFactor inferenceVariation4(int[] query, TIntIntMap obs, String hash) throws InterruptedException {

        int U = query[0];
        int[] chU = posteriorModel.getChildren(U);

        BayesianFactor pU = posteriorModel.getFactor(U);
        BayesianFactor pX = null;

        if (equationsCache.containsKey(hash)) {
            pX = equationsCache.get(hash);
        }else {
            ArrayList<BayesianFactor> factors = new ArrayList<>();
            for (int X : chU) {
                TIntIntHashMap newObs = new TIntIntHashMap();
                BayesianFactor fx = posteriorModel.getFactor(X);
                for (int x : obs.keys()) {
                    if (ArraysUtil.contains(x, fx.getDomain().getVariables()))
                        newObs.put(x, obs.get(x));
                }
                factors.add(fx.filter(newObs));
            }
            pX = BayesianFactor.combineAll(factors);
            equationsCache.put(hash, pX);
        }

        BayesianFactor pjoin = pX.combine(pU);
        return pjoin.divide(pjoin.marginalize(U));

    }


    void clearPosteriorCache(){
        posteriorCache.clear();
    }

    public FrequentistCausalEM usePosteriorCache(boolean active) {
        this.usePosteriorCache = active;
        return this;
    }


    protected TIntIntMap[] data = null;

    protected void setData(TIntIntMap[] data) {
        this.data = data;
    }

    public void run(Collection stepArgs, int iterations) throws InterruptedException {

        if(data == null)
            data = (TIntIntMap[]) stepArgs.toArray(TIntIntMap[]::new);

        StopWatch watch = null;
        if(verbose) {
            watch = new StopWatch();
            watch.start();
        }
        init();
        for(int i=1; i<=iterations; i++) {
            if(verbose){
                if(i % 10 == 0) {
                    watch.stop();
                    long time = watch.getTime();
                    Logger.getGlobal().debug(i + " EM iterations in "+time+" ms.");
                    watch.reset();
                    watch.start();
                }
            }
            step(stepArgs);
            if(trainableVars.length==0)
                break;

        }
        if(verbose && !watch.isStopped()) watch.stop();

    }

    private void init(){

        if(!Double.isNaN(klthreshold)) {
            throw new IllegalArgumentException("The usage of klthreshold is not allowed anymore. Use threshold instead.");
        }

        if(!inline)
            this.posteriorModel = priorModel.copy();
        else
            this.posteriorModel = priorModel;

        if(trainableVars == null)
            trainableVars = posteriorModel.getVariables();

        if(recordIntermediate) {
            intermediateModels = new ArrayList<GraphicalModel<BayesianFactor>>();
            addIntermediateModels(priorModel);
        }

    }


    public FrequentistCausalEM setInferenceVariation(int inferenceVariation) {
        this.inferenceVariation = inferenceVariation;
        return this;
    }

    public FrequentistCausalEM setStopCriteria(StopCriteria stopCriteria) {
        this.stopCriteria = stopCriteria;
        return this;
    }

    public FrequentistCausalEM setThreshold(double threshold) {
        this.threshold = threshold;
        return this;
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
                new FrequentistCausalEM(rmodel)
                        .setInferenceVariation(0)
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

