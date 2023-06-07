package ch.idsia.credici.learning;

import ch.idsia.credici.model.StructuralCausalModel;
import ch.idsia.credici.utility.DataUtil;
import ch.idsia.credici.utility.Probability;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

public class TrajectoryAnalyser {


    public static boolean hasConvergedKL(StructuralCausalModel model, TIntObjectMap replacedFactors, double epsilon, int...exoCC){
        for (int u : exoCC) {
            if (Probability.KLsymmetrized(model.getFactor(u), (BayesianFactor) replacedFactors.get(u), true) >= epsilon) {
                return false;
            }
        }
        return true;
    }
    public static boolean hasConvergedL1(StructuralCausalModel model, TIntObjectMap replacedFactors, double epsilon, int...exoCC){
        for (int u : exoCC) {
            if (Probability.manhattanDist(model.getFactor(u), (BayesianFactor) replacedFactors.get(u)) >= epsilon) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasConvergedLLratio(StructuralCausalModel model, TIntIntMap[] data, double epsilon, int...exoCC){
        double ratio = Probability.ratioLogLikelihood(model.getCFactorsSplittedMap(exoCC),
                DataUtil.getCFactorsSplittedMap(model, data, exoCC),  1);

        if(ratio>=epsilon)
            return true;
        return false;
    }
/*
    StructuralCausalModel baseModel;
    TIntIntMap[] data;

    HashMap<Set<Integer>, Double> maxLL = new HashMap<>();
    HashMap<Set<Integer>, TIntObjectMap<BayesianFactor>> empiricalDecomp = new HashMap<>();


    public TrajectoryAnalyser(StructuralCausalModel baseModel, TIntIntMap[] data){
        this.baseModel = baseModel;
        this.data = data;

        for(int[] exoVars : baseModel.exoConnectComponents()){
            TIntObjectMap<BayesianFactor> emp_c = DataUtil.getCFactorsSplittedMap(baseModel, data, exoVars);
            double maxLL_c = Probability.maxLogLikelihood(emp_c, data.length);
            Set<Integer> exoSet= Arrays.stream(exoVars).boxed().collect(Collectors.toSet());
            maxLL.put(exoSet, maxLL_c);
            empiricalDecomp.put(exoSet, emp_c);
        }


    }
    public TrajectoryAnalyser of(StructuralCausalModel baseModel, TIntIntMap[] data){
        return new TrajectoryAnalyser(baseModel, data);
    }
    /*
 */
    public static boolean hasConvergedDiff(StructuralCausalModel posteriorModel,
            TIntObjectMap<BayesianFactor> replacedFactors, double threshold, int[] exoCC) {
        for (var key : replacedFactors.keys()) {
            var bf1 = replacedFactors.get(key);
            var bf2 = posteriorModel.getFactor(key);

            double diff = 0;
            double[] a1 = bf1.getInteralData();
            double[] a2 = bf2.getInteralData();
            for (int i = 0; i < a1.length; ++i) {
                diff += Math.abs(a1[i] - a2[i]);
            }
            
            if (diff > threshold)
                return false;
        }
        return true;
    }
    

}
