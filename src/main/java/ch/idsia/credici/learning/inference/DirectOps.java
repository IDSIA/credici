package ch.idsia.credici.learning.inference;

import java.util.ArrayList;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class DirectOps implements EMInference {
     /*
     The inference engine is not invoked, operations over factors are directly perfomed.
     */
    @Override
    public BayesianFactor run(StructuralCausalModel posteriorModel, int U, TIntIntMap obs, String hash) throws InterruptedException {
        // todo: only with M and QM... set checks

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

}
