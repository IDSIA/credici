package ch.idsia.credici.learning.inference;

import java.util.HashMap;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class CachedModels implements EMInference {

    private JoinInference<BayesianFactor, BayesianFactor> inferenceEngine;
    private HashMap<String, StructuralCausalModel> modelCache = new HashMap<>();

    public CachedModels(JoinInference<BayesianFactor, BayesianFactor> inferenceEngine) {
        this.inferenceEngine = inferenceEngine;
    } 
    
    public CachedModels() {
    }
    
    /*
     * The model is simplified at the first posterior query and stored in a caché
     * */
    public BayesianFactor run(StructuralCausalModel posteriorModel, int U, TIntIntMap obs,  TIntIntMap filteredObs, String hash) throws InterruptedException {

        if (inferenceEngine == null) {
            inferenceEngine = getDefaultInference(posteriorModel, (new MinFillOrdering()).apply(posteriorModel));
        }
        StructuralCausalModel infModel = null;

        if(!modelCache.containsKey(hash)) {
            infModel = new CutObserved().execute(posteriorModel, obs);
            infModel = new RemoveBarren().execute(infModel, new int[] {U}, obs);
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
        return inferenceEngine.apply(infModel, new int[] {U}, newObs);  // P(U|X=obs)
    }


    protected JoinInference<BayesianFactor, BayesianFactor> getDefaultInference(GraphicalModel<BayesianFactor> model, int[] elimSeq) {
        return (m, query, obs) ->
        {
            CutObserved co = new CutObserved();
            GraphicalModel coModel = co.execute(m, obs);

            RemoveBarren rb = new RemoveBarren();
            GraphicalModel infModel = rb.execute(coModel, query, obs);
            rb.filter(elimSeq);

            FactorVariableElimination fve = new FactorVariableElimination(elimSeq);
            fve.setNormalize(true);

            return (BayesianFactor) fve.apply(infModel, query, obs);
        };
    }

}