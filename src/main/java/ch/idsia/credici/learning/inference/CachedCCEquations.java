package ch.idsia.credici.learning.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class CachedCCEquations implements EMInference {


    private Map<String, BayesianFactor> equationsCache = new HashMap<>();


    /*
     * The model is simplified at the first posterior query and stored in a caché
     */
    @Override
    public BayesianFactor run(StructuralCausalModel posteriorModel, int U, TIntIntMap obs,  TIntIntMap filteredObs, String hash) throws InterruptedException {

        int[] chU = posteriorModel.getChildren(U);

        BayesianFactor pU = posteriorModel.getFactor(U);
        BayesianFactor pX = null;

        if (equationsCache.containsKey(hash)) {
            pX = equationsCache.get(hash);
        } else {
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
}
