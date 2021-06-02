package edu.neurips.causalem.learning;

import edu.neurips.causalem.model.StructuralCausalModel;
import edu.neurips.causalem.model.info.CausalInfo;
import edu.neurips.causalem.model.predefined.RandomChainNonMarkovian;
import edu.neurips.causalem.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.learning.DiscreteEM;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.RandomUtil;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.time.StopWatch;

import java.util.*;


public class BayesianCausalEM extends DiscreteEM<BayesianCausalEM> {

    private double regularization = 0.00001;

    private TIntObjectMap<BayesianFactor> counts;

    private HashMap<String, BayesianFactor> posteriorCache = new HashMap<>();

    private boolean usePosteriorCache = true;

    //private StructuralCausalModel priorModel;

    //private StructuralCausalModel posteriorModel;

    public HashMap<Set<Integer>, BayesianFactor> targetGenDist;





    public BayesianCausalEM(StructuralCausalModel model,
							JoinInference<BayesianFactor, BayesianFactor> inferenceEngine) {
        this.inferenceEngine = inferenceEngine;
        this.priorModel = model;
        this.trainableVars = CausalInfo.of((StructuralCausalModel) priorModel).getExogenousVars();
    }

    public BayesianCausalEM(StructuralCausalModel model, int[] elimSeq){
        this.inferenceEngine = getDefaultInference(model, elimSeq);;
        this.priorModel = model;
        this.trainableVars = CausalInfo.of(getPrior()).getExogenousVars();
    }

    public BayesianCausalEM(StructuralCausalModel model) {
        this(model, (new MinFillOrdering()).apply(model));
    }


    protected void stepPrivate(Collection stepArgs) throws InterruptedException {

        for (int u : trainableVars) {

            BayesianFactor pu = new BayesianFactor(priorModel.getDomain(u));
            Strides blanket = getPrior().endogenousMarkovBlanket(u);

            // get the empirical
            BayesianFactor emp = (BayesianFactor) stepArgs.stream().filter(p ->
                    ArraysUtil.equals(((BayesianFactor) p).getDomain().getVariables(),
                            blanket.getVariables(), true, false)
            ).toArray(BayesianFactor[]::new)[0];

            // iterate over each state in the markov blanket
            for (int i = 0; i < blanket.getCombinations(); i++) {
                TIntIntHashMap obs = ObservationBuilder.observe(blanket.getVariables(), blanket.statesOf(i));
                double emp_i = emp.filter(obs).getValueAt(0);
                if (emp_i > 0)
                    pu = pu.addition(((BayesianFactor) inferenceEngine.apply(posteriorModel, u, obs)).scalarMultiply(emp_i));
            }

            // Update the model
            if(pu.KLdivergence(posteriorModel.getFactor(u)) > klthreshold || targetGenDist != null) {
                posteriorModel.setFactor(u, pu);
                updated = true;
            }
        }

        if(targetGenDist != null){
            if(Probability.ratioLogLikelihood(
                    ((StructuralCausalModel)posteriorModel).getEmpiricalMap(), targetGenDist
                    , 1) == 1.0)
                updated = false;
        }

    }

    public Collection getJointProbs(StructuralCausalModel model) throws InterruptedException {

        List out = new ArrayList();
        for(int u: model.getExogenousVars()){
            Strides blanket = model.endogenousMarkovBlanket(u);
            out.add(inferenceEngine.apply(model, blanket.getVariables(), new TIntIntHashMap()));
        }

        return out;
    }


    public BayesianCausalEM setRegularization(double regularization) {
        this.regularization = regularization;
        return this;
    }

    public double getRegularization() {
        return regularization;
    }


    @Override
    public BayesianCausalEM setTrainableVars(int[] trainableVars) {

        for(int v: trainableVars)
            if(!CausalInfo.of((StructuralCausalModel) priorModel).isExogenous(v))
                throw new IllegalArgumentException("Only exogenous variables can be trainable. Error with "+v);

        return super.setTrainableVars(trainableVars);
    }



    BayesianFactor posteriorInference(int[] query, TIntIntMap obs) throws InterruptedException {

        String hash = Arrays.toString(Ints.concat(query,new int[]{-1}, obs.keys(), obs.values()));

        if(!posteriorCache.containsKey(hash) || !usePosteriorCache) {
            BayesianFactor p = inferenceEngine.apply(posteriorModel, query, obs);
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

    public BayesianCausalEM usePosteriorCache(boolean active) {
        this.usePosteriorCache = active;
        return this;
    }

    public StructuralCausalModel getPrior(){
        return (StructuralCausalModel) this.priorModel;
    }

    public StructuralCausalModel getPosterior(){
        return (StructuralCausalModel) this.posteriorModel;
    }

    public BayesianCausalEM setTargetGenDist(HashMap<Set<Integer>, BayesianFactor> targetGenDist) {
        this.targetGenDist = targetGenDist;
        return this;
    }

    public static void main(String[] args) throws InterruptedException {


        int N = 2500;
        int numIterations = 1000; // EM internal iterations
        int n = 3;
        int endoVarSize = 2;
        int exoVarSize = 4;

        StructuralCausalModel causalModel = null;


        RandomUtil.setRandomSeed(1000);
        causalModel = RandomChainNonMarkovian.buildModel(n, endoVarSize, exoVarSize);
        SparseModel vmodel = causalModel.toVCredal(causalModel.getEmpiricalProbs());

        System.out.println(vmodel);

        // randomize P(U)
        StructuralCausalModel rmodel = (StructuralCausalModel) BayesianFactor.randomModel(causalModel,
                5, false
                ,causalModel.getExogenousVars()
        );

        // Run EM in the causal model
        BayesianCausalEM em =
                new BayesianCausalEM(rmodel)
                        .setVerbose(false)
                        .setRegularization(0.0)
                        .usePosteriorCache(true)
                        .setTrainableVars(causalModel.getExogenousVars());

        StopWatch watch = new StopWatch();
        watch.start();

        // run the method
        em.run(em.getJointProbs(causalModel), numIterations);

        watch.stop();
        System.out.println("Time Elapsed: " + watch.getTime()+" ms.");

        System.out.println(em.getPosterior());


    }

}

