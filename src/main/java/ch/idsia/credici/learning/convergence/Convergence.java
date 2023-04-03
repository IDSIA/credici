package ch.idsia.credici.learning.convergence;

import ch.idsia.credici.Table;
import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.Strides;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;


/**
 * Convergence test Strategies. 
 * A convergence strategy is tested by providing the structural model, the data 
 */
public interface Convergence {
    boolean hasConverged(StructuralCausalModel model, Table data, TIntObjectHashMap<BayesianFactor> replacedFactors, double epsilon, int...exoCC);

    public static class L1 implements Convergence {
        @Override
        public boolean hasConverged(StructuralCausalModel model, Table data, TIntObjectHashMap<BayesianFactor> replacedFactors, double epsilon, int...exoCC){
            for (int u : exoCC) {
                if (Probability.manhattanDist(model.getFactor(u), replacedFactors.get(u)) >= epsilon) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class KL implements Convergence {
        @Override
        public boolean hasConverged(StructuralCausalModel model, Table data, TIntObjectHashMap<BayesianFactor> replacedFactors, double epsilon, int...exoCC){
            for (int u : exoCC) {
                if (Probability.KLsymmetrized(model.getFactor(u), replacedFactors.get(u), true) >= epsilon) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class LLRatio implements Convergence {
        @Override
        public boolean hasConverged(StructuralCausalModel model, Table data, TIntObjectHashMap<BayesianFactor> replacedFactors, double epsilon, int...exoCC){
            double ratio = Probability.ratioLogLikelihood(
                        model.getCFactorsSplittedMap(exoCC),
                        DataUtil.getCFactorsSplittedMap(model, data.convert(), exoCC),  1);
        
            return ratio>=epsilon;
        }
    }
}
